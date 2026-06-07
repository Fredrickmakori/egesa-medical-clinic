package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.domain.*
import kotlinx.datetime.Clock

/**
 * Integrated Lab Repository - Combines basic and enhanced lab operations
 * Provides single entry point for all lab functionality
 */
class IntegratedLabRepository(
    private val basicRepository: LocalLabRepository,
    private val enhancedRepository: LocalEnhancedLabRepository,
) : EnhancedLabRepository by enhancedRepository {

    private val labService = LabService()

    // ========================================================================
    // HIGH-LEVEL WORKFLOWS
    // ========================================================================

    /**
     * Complete workflow: Order -> Collection -> Analysis -> Verification -> Report
     */
    suspend fun processLabOrderComplete(
        order: LabOrder,
        specimen: LabSpecimen,
        results: List<LabResultExtended>,
        actorId: String,
    ): OrderProcessingResult {
        return try {
            val now = Clock.System.now().toString()

            // Step 1: Create order
            val createdOrder = basicRepository.createLabOrder(order)

            // Step 2: Collect specimen
            val collectedSpecimen = specimen.copy(
                collectedAt = now,
                collectedBy = actorId,
                status = SpecimenStatus.COLLECTED,
            )
            enhancedRepository.createSpecimen(collectedSpecimen)

            // Step 3: Process results with validation
            val processedResults = mutableListOf<LabResultExtended>()
            val criticalAlerts = mutableListOf<LabCriticalAlert>()

            for (result in results) {
                // Get reference ranges
                val referenceRanges = enhancedRepository.getAllReferenceRanges(result.testId)
                val referenceRange = referenceRanges.firstOrNull() ?: continue

                // Get test details
                val test = enhancedRepository.getLabTest(result.testId) ?: continue

                // Get previous result for delta check
                val previousResult = enhancedRepository.getPreviousResult(order.patientId, result.testId)

                // Process with validation engine
                val processedResult = labService.processLabResult(
                    result = result,
                    test = test,
                    referenceRange = referenceRange,
                    order = createdOrder,
                    previousResult = previousResult,
                )

                processedResults.add(processedResult.result)
                processedResult.criticalAlert?.let { criticalAlerts.add(it) }
            }

            // Step 4: Save results
            enhancedRepository.saveExtendedResults(createdOrder.id, processedResults, actorId)

            // Step 5: Create critical alerts
            for (alert in criticalAlerts) {
                enhancedRepository.createCriticalAlert(alert)
            }

            // Step 6: Update order status to verified
            basicRepository.updateOrderStatus(createdOrder.id, LabOrderStatus.VERIFIED, actorId)

            // Step 7: Report order
            basicRepository.updateOrderStatus(createdOrder.id, LabOrderStatus.REPORTED, actorId)

            OrderProcessingResult(
                success = true,
                order = createdOrder,
                resultCount = processedResults.size,
                criticalAlertsCount = criticalAlerts.size,
                message = "Order processed successfully"
            )
        } catch (e: Exception) {
            OrderProcessingResult(
                success = false,
                order = null,
                resultCount = 0,
                criticalAlertsCount = 0,
                message = "Error processing order: ${e.message}"
            )
        }
    }

    /**
     * QC Check workflow before testing
     */
    suspend fun performQCCheck(
        equipment: LabEquipment,
        qcResult: LabQCResult,
        material: LabQCMaterial,
        performedBy: String,
    ): QCCheckResult {
        return try {
            // Get recent QC results
            val recentResults = enhancedRepository.getRecentQCResults(equipment.id, limit = 10)

            // Process QC result
            val processedQC = labService.processQCResult(qcResult, material, recentResults)

            // Store QC result
            enhancedRepository.createQCResult(qcResult)

            // Get equipment QC status
            val allQCResults = enhancedRepository.getQCResultsByEquipment(equipment.id)
            val equipmentStatus = labService.getEquipmentQCStatus(equipment, allQCResults)

            QCCheckResult(
                canContinueTesting = processedQC.canContinueTesting,
                assessment = processedQC.assessment,
                equipmentStatus = equipmentStatus,
                message = processedQC.assessment.message,
                recommendations = processedQC.recommendations,
            )
        } catch (e: Exception) {
            QCCheckResult(
                canContinueTesting = false,
                assessment = null,
                equipmentStatus = null,
                message = "Error performing QC check: ${e.message}",
                recommendations = listOf("Contact lab supervisor")
            )
        }
    }

    /**
     * Get worklist with TAT monitoring
     */
    suspend fun getWorklistWithTATMonitoring(
        status: LabOrderStatus,
    ): List<OrderWithTATStatus> {
        return try {
            val orders = basicRepository.getWorklist(
                LabWorklistQuery(
                    department = "LAB",
                    status = status,
                )
            )

            orders.mapNotNull { order ->
                val tatTracking = enhancedRepository.getTATTracking(order.id) ?: return@mapNotNull null
                val compliance = labService.monitorTATCompliance(tatTracking)

                OrderWithTATStatus(
                    order = order,
                    tatTracking = tatTracking,
                    complianceStatus = compliance.complianceStatus,
                    needsAttention = compliance.needsIntervention,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get dashboard with all critical metrics
     */
    suspend fun getCompleteDashboard(): CompleteLaboratoryDashboard {
        return try {
            val summary = enhancedRepository.getDashboardSummary()
            val equipmentStatus = enhancedRepository.getEquipmentStatusSummary()
            val reagentAlerts = enhancedRepository.getReagentExpiryAlerts()
            val criticalAlerts = enhancedRepository.getPendingCriticalAlerts()
            val tatMetrics = enhancedRepository.getTATComplianceMetrics(
                fromDate = addDays(Clock.System.now().toString(), -7),
                toDate = Clock.System.now().toString()
            )

            CompleteLaboratoryDashboard(
                summary = summary,
                equipmentStatus = equipmentStatus,
                reagentAlerts = reagentAlerts,
                criticalAlerts = criticalAlerts,
                tatMetrics = tatMetrics,
                hasWarnings = equipmentStatus.any { it.calibrationStatus != "OK" || it.maintenanceStatus != "OK" }
                    || reagentAlerts.isNotEmpty()
                    || criticalAlerts.isNotEmpty(),
            )
        } catch (e: Exception) {
            CompleteLaboratoryDashboard(
                summary = null,
                equipmentStatus = emptyList(),
                reagentAlerts = emptyList(),
                criticalAlerts = emptyList(),
                tatMetrics = null,
                hasWarnings = true,
            )
        }
    }

    /**
     * Acknowledge critical alert
     */
    suspend fun acknowledgeCriticalAlert(
        alertId: String,
        acknowledgedBy: String,
        notes: String? = null,
    ): Boolean {
        return try {
            enhancedRepository.updateCriticalAlertAcknowledged(alertId, acknowledgedBy, notes)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get staff eligibility for test
     */
    suspend fun canStaffPerformTest(
        staffId: String,
        testId: String,
    ): StaffEligibilityResult {
        return try {
            val competency = enhancedRepository.getStaffCompetency(staffId, testId)
            val canPerform = labService.canStaffPerformTest(competency, testId)

            StaffEligibilityResult(
                canPerform = canPerform,
                competencyLevel = competency?.competencyLevel,
                certificationExpiry = competency?.certificationExpiry,
                message = when {
                    competency == null -> "No competency record found for this staff"
                    competency.competencyLevel == CompetencyLevel.TRAINEE -> "Trainee requires supervision"
                    competency.certificationExpiry != null && competency.certificationExpiry!! < Clock.System.now().toString() ->
                        "Certification has expired"
                    !competency.isActive -> "Competency record is inactive"
                    else -> "Staff is eligible to perform this test"
                }
            )
        } catch (e: Exception) {
            StaffEligibilityResult(
                canPerform = false,
                competencyLevel = null,
                certificationExpiry = null,
                message = "Error checking eligibility: ${e.message}"
            )
        }
    }

    /**
     * Get equipment status for operations
     */
    suspend fun getEquipmentReadiness(equipmentId: String): EquipmentReadinessResult {
        return try {
            val equipment = enhancedRepository.getEquipment(equipmentId) ?: return EquipmentReadinessResult(
                isReady = false,
                message = "Equipment not found"
            )

            val calibrationStatus = labService.getCalibrationStatus(equipment)
            val qcResults = enhancedRepository.getRecentQCResults(equipmentId, limit = 5)
            val qcStatus = labService.getEquipmentQCStatus(equipment, qcResults)

            EquipmentReadinessResult(
                isReady = calibrationStatus.action == "OK" && qcStatus.action == "OK",
                equipment = equipment,
                calibrationStatus = calibrationStatus,
                qcStatus = qcStatus,
                message = when {
                    calibrationStatus.isAlert -> "Calibration issue: ${calibrationStatus.message}"
                    qcStatus.action != "OK" -> "QC issue: ${qcStatus.message}"
                    else -> "Equipment ready for testing"
                }
            )
        } catch (e: Exception) {
            EquipmentReadinessResult(
                isReady = false,
                message = "Error checking equipment: ${e.message}"
            )
        }
    }

    private fun addDays(isoDate: String, days: Int): String {
        return try {
            val instant = kotlinx.datetime.Instant.parse(isoDate)
            val newInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(
                instant.toEpochMilliseconds() + (days * 24L * 60L * 60L * 1000L)
            )
            newInstant.toString()
        } catch (e: Exception) {
            isoDate
        }
    }
}

// ============================================================================
// DATA CLASSES FOR WORKFLOWS
// ============================================================================

data class OrderProcessingResult(
    val success: Boolean,
    val order: LabOrder?,
    val resultCount: Int,
    val criticalAlertsCount: Int,
    val message: String,
)

data class QCCheckResult(
    val canContinueTesting: Boolean,
    val assessment: QCAssessment?,
    val equipmentStatus: EquipmentQCStatus?,
    val message: String,
    val recommendations: List<String>,
)

data class OrderWithTATStatus(
    val order: LabOrder,
    val tatTracking: LabTATTracking,
    val complianceStatus: TATComplianceStatus,
    val needsAttention: Boolean,
)

data class CompleteLaboratoryDashboard(
    val summary: LabDashboardSummary?,
    val equipmentStatus: List<LabEquipmentStatusSummary>,
    val reagentAlerts: List<LabReagentExpirySummary>,
    val criticalAlerts: List<LabCriticalAlert>,
    val tatMetrics: TATComplianceMetrics?,
    val hasWarnings: Boolean,
)

data class StaffEligibilityResult(
    val canPerform: Boolean,
    val competencyLevel: CompetencyLevel?,
    val certificationExpiry: String?,
    val message: String,
)

data class EquipmentReadinessResult(
    val isReady: Boolean,
    val equipment: LabEquipment? = null,
    val calibrationStatus: CalibrationStatus? = null,
    val qcStatus: EquipmentQCStatus? = null,
    val message: String,
)

// ============================================================================
// FACTORY FOR CREATING REPOSITORIES
// ============================================================================

object LabRepositoryFactory {
    fun createIntegratedRepository(
        basicRepository: LocalLabRepository,
        enhancedRepository: LocalEnhancedLabRepository,
    ): IntegratedLabRepository {
        return IntegratedLabRepository(basicRepository, enhancedRepository)
    }
}
