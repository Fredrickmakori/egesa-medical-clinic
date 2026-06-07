package com.egesa.clinic.shared.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Core Lab Service - Orchestrates all lab operations
 * Coordinates validation, QC, alerts, equipment management, and turnaround time tracking
 */
class LabService(
    private val validationEngine: LabResultValidationEngine = LabResultValidationEngine(),
    private val qcEngine: LabQCEngine = LabQCEngine(),
    private val tatEngine: LabTATEngine = LabTATEngine(),
    private val alertEngine: LabCriticalAlertEngine = LabCriticalAlertEngine(),
) {

    // ========================================================================
    // RESULT PROCESSING AND VALIDATION
    // ========================================================================

    /**
     * Process a lab result with full validation pipeline
     */
    suspend fun processLabResult(
        result: LabResultExtended,
        test: LabTestDetail,
        referenceRange: LabReferenceRange,
        order: LabOrder,
        previousResult: LabResultExtended? = null,
        patientAge: Int? = null,
        patientGender: String? = null,
    ): ProcessedLabResult {
        // Validate result
        val validationResult = validationEngine.validateResult(
            result = result,
            referenceRange = referenceRange,
            test = test,
            previousResult = previousResult,
            patientAge = patientAge,
            patientGender = patientGender,
        )

        // Create critical alert if needed
        var criticalAlert: LabCriticalAlert? = null
        if (validationResult.isCriticalValue) {
            criticalAlert = alertEngine.createCriticalAlert(result, test, order)
        }

        // Update result with validation info
        val updatedResult = result.copy(
            flag = validationResult.flag,
            isCriticalValue = validationResult.isCriticalValue,
            valueNumeric = result.valueNumeric ?: result.value.toDoubleOrNull(),
        )

        return ProcessedLabResult(
            result = updatedResult,
            validationResult = validationResult,
            criticalAlert = criticalAlert,
            reflexiveTestsNeeded = validationResult.shouldTriggerReflexiveTest,
        )
    }

    /**
     * Verify lab result (approval workflow)
     */
    suspend fun verifyLabResult(
        result: LabResultExtended,
        verifiedBy: String,
        qcCheckPerformed: Boolean = true,
    ): LabResultExtended {
        val now = Clock.System.now().toString()

        return result.copy(
            resultStatus = ResultStatus.VERIFIED,
            verifiedBy = verifiedBy,
            verifiedAt = now,
            updatedAt = now,
        )
    }

    /**
     * Report lab result (make available to clinicians)
     */
    suspend fun reportLabResult(
        result: LabResultExtended,
        reportedBy: String,
    ): LabResultExtended {
        val now = Clock.System.now().toString()

        return result.copy(
            resultStatus = ResultStatus.REPORTED,
            reportedBy = reportedBy,
            reportedAt = now,
            updatedAt = now,
        )
    }

    // ========================================================================
    // QUALITY CONTROL OPERATIONS
    // ========================================================================

    /**
     * Process QC result with assessment
     */
    suspend fun processQCResult(
        qcResult: LabQCResult,
        material: LabQCMaterial,
        recentResults: List<LabQCResult> = emptyList(),
    ): ProcessedQCResult {
        // Check material validity
        val materialStatus = qcEngine.isQCMaterialValid(material)

        // Assess QC result
        val assessment = qcEngine.assessQCResult(qcResult, material)

        // Analyze trend
        val allResults = (recentResults + qcResult).sortedBy { it.qcDate }
        val trendAnalysis = qcEngine.analyzeLeveyJennsingsTrend(allResults, material)

        // Determine if testing can continue
        val canContinueTesting = assessment.isEquipmentSuitable && materialStatus.isValid

        val alerts = mutableListOf<String>()
        if (!canContinueTesting) {
            alerts.add("TESTING SUSPENDED - Equipment unsuitable or material invalid")
        }
        alerts.addAll(trendAnalysis.alerts)
        if (!materialStatus.isValid) {
            alerts.add(materialStatus.reason)
        }

        return ProcessedQCResult(
            qcResult = qcResult,
            assessment = assessment,
            materialStatus = materialStatus,
            trendAnalysis = trendAnalysis,
            canContinueTesting = canContinueTesting,
            alerts = alerts,
            recommendations = assessment.recommendations +
                listOf("Next QC due: Check equipment manufacturer schedule")
        )
    }

    /**
     * Check if equipment is QC-compliant
     */
    suspend fun getEquipmentQCStatus(
        equipment: LabEquipment,
        recentQCResults: List<LabQCResult>,
    ): EquipmentQCStatus {
        if (recentQCResults.isEmpty()) {
            return EquipmentQCStatus(
                equipmentId = equipment.id,
                isCompliant = false,
                lastQCResult = null,
                dayssinceLastQC = null,
                message = "No QC results found - equipment cannot be used",
                action = "RUN_QC_IMMEDIATELY"
            )
        }

        val lastResult = recentQCResults.maxByOrNull { it.qcDate } ?: return EquipmentQCStatus(
            equipmentId = equipment.id,
            isCompliant = false,
            lastQCResult = null,
            dayssinceLastQC = null,
            message = "No QC results found",
            action = "RUN_QC_IMMEDIATELY"
        )

        val daysSinceQC = try {
            val lastQCInstant = Instant.parse(lastResult.qcDate)
            val now = Clock.System.now()
            (now.toEpochMilliseconds() - lastQCInstant.toEpochMilliseconds()) / (1000.0 * 60.0 * 60.0 * 24.0)
        } catch (e: Exception) {
            0.0
        }

        val isCompliant = lastResult.status == QCResultStatus.PASSED && daysSinceQC < 1.0

        return EquipmentQCStatus(
            equipmentId = equipment.id,
            isCompliant = isCompliant,
            lastQCResult = lastResult,
            dayssinceLastQC = daysSinceQC,
            message = when {
                !isCompliant && daysSinceQC > 1.0 -> "QC result is stale - rerun QC"
                lastResult.status == QCResultStatus.FAILED -> "Last QC FAILED - equipment not suitable"
                lastResult.status == QCResultStatus.WARNING -> "Last QC WARNING - monitor closely"
                else -> "Equipment is QC compliant"
            },
            action = when {
                lastResult.status == QCResultStatus.FAILED -> "DO_NOT_USE"
                daysSinceQC > 1.0 -> "RUN_QC_BEFORE_USE"
                else -> "OK"
            }
        )
    }

    // ========================================================================
    // TURNAROUND TIME TRACKING
    // ========================================================================

    /**
     * Monitor TAT compliance for an order
     */
    suspend fun monitorTATCompliance(tracking: LabTATTracking): TATMonitoringStatus {
        val compliance = tatEngine.checkTATCompliance(tracking)
        val calculation = tatEngine.calculateTAT(tracking)
        val bottlenecks = tatEngine.identifyBottlenecks(tracking)

        return TATMonitoringStatus(
            tracking = tracking,
            complianceStatus = compliance,
            tatCalculation = calculation,
            bottlenecks = bottlenecks,
            needsIntervention = compliance.isAlert || bottlenecks.isNotEmpty(),
        )
    }

    /**
     * Get pending orders sorted by TAT urgency
     */
    suspend fun getPendingOrdersByTATUrgency(
        pendingOrders: List<Pair<LabOrder, LabTATTracking>>
    ): List<TATUrgencyItem> {
        return pendingOrders.mapNotNull { (order, tracking) ->
            val compliance = tatEngine.checkTATCompliance(tracking)
            if (!tracking.resultReportedAt.isNullOrEmpty()) return@mapNotNull null

            TATUrgencyItem(
                orderId = order.id,
                patientId = order.patientId,
                testName = order.testName,
                priority = order.priority,
                status = compliance.status,
                percentageUsed = compliance.percentageUsed,
                hoursRemaining = compliance.hoursRemaining,
                message = compliance.message,
            )
        }.sortedWith(compareBy(
            { it.status != "BREACHED" },
            { it.status != "CRITICAL" },
            { -it.percentageUsed.toInt() }
        ))
    }

    // ========================================================================
    // CRITICAL ALERTS
    // ========================================================================

    /**
     * Get pending critical alerts requiring action
     */
    suspend fun getPendingCriticalAlerts(
        alerts: List<LabCriticalAlert>
    ): List<LabCriticalAlert> {
        return alerts
            .filter { it.acknowledgedAt == null && it.alertSeverity == AlertSeverity.CRITICAL }
            .sortedBy { it.createdAt }
    }

    /**
     * Check for alerts needing escalation
     */
    suspend fun checkAlertEscalations(
        alerts: List<LabCriticalAlert>
    ): List<Pair<LabCriticalAlert, EscalationStatus>> {
        return alerts.map { alert ->
            alert to alertEngine.checkEscalation(alert)
        }.filter { (_, escalation) ->
            escalation.needsEscalation
        }
    }

    // ========================================================================
    // EQUIPMENT AND REAGENT MANAGEMENT
    // ========================================================================

    /**
     * Check calibration status for equipment
     */
    suspend fun getCalibrationStatus(equipment: LabEquipment): CalibrationStatus {
        val now = Clock.System.now().toString()

        return when {
            equipment.nextCalibrationDue == null -> {
                CalibrationStatus(
                    status = "UNKNOWN",
                    message = "No calibration date recorded",
                    isAlert = true,
                    action = "SCHEDULE_CALIBRATION"
                )
            }
            equipment.nextCalibrationDue < now -> {
                CalibrationStatus(
                    status = "OVERDUE",
                    message = "Calibration due date passed",
                    isAlert = true,
                    action = "SCHEDULE_CALIBRATION_IMMEDIATELY"
                )
            }
            equipment.nextCalibrationDue < addDays(now, 7) -> {
                CalibrationStatus(
                    status = "DUE_SOON",
                    message = "Calibration due within 7 days",
                    isAlert = true,
                    action = "SCHEDULE_CALIBRATION"
                )
            }
            else -> {
                CalibrationStatus(
                    status = "CURRENT",
                    message = "Calibration current",
                    isAlert = false,
                    action = "OK"
                )
            }
        }
    }

    /**
     * Check reagent expiry and stock levels
     */
    suspend fun getReagentStatus(reagent: LabReagent): ReagentStatusCheck {
        val now = Clock.System.now().toString()

        val expiryStatus = when {
            reagent.expiryDate < now -> "EXPIRED"
            reagent.expiryDate < addDays(now, 30) -> "EXPIRING_SOON"
            else -> "OK"
        }

        val stockStatus = when {
            reagent.quantity <= 0 -> "DEPLETED"
            reagent.reorderLevel != null && reagent.quantity <= reagent.reorderLevel -> "REORDER_NEEDED"
            else -> "IN_STOCK"
        }

        val isAlert = expiryStatus != "OK" || stockStatus != "IN_STOCK"

        return ReagentStatusCheck(
            reagentId = reagent.id,
            expiryStatus = expiryStatus,
            stockStatus = stockStatus,
            isAlert = isAlert,
            message = buildString {
                if (expiryStatus != "OK") append("Expiry: $expiryStatus | ")
                if (stockStatus != "IN_STOCK") append("Stock: $stockStatus")
            }
        )
    }

    /**
     * Check staff competency for test
     */
    suspend fun canStaffPerformTest(
        staffCompetency: LabStaffCompetency?,
        testId: String,
    ): Boolean {
        if (staffCompetency == null) return false
        if (staffCompetency.testId != testId) return false

        val now = Clock.System.now().toString()
        if (staffCompetency.competencyLevel == CompetencyLevel.TRAINEE) return false
        if (staffCompetency.certificationExpiry != null && staffCompetency.certificationExpiry < now) return false

        return staffCompetency.isActive
    }

    // ========================================================================
    // DASHBOARD AND SUMMARY
    // ========================================================================

    /**
     * Build lab dashboard summary
     */
    suspend fun getDashboardSummary(
        totalOrders: Int,
        pendingResults: Int,
        criticalAlerts: List<LabCriticalAlert>,
        equipmentIssues: List<LabEquipment>,
        reagentExpiries: List<LabReagent>,
        tatMetData: List<Pair<Boolean, Int>>,  // (tatMet, expectedHours)
    ): LabDashboardSummary {
        val tatMet = tatMetData.count { it.first }
        val totalCompleted = tatMetData.size
        val tatCompliancePercent = if (totalCompleted > 0) (tatMet.toDouble() / totalCompleted) * 100 else 0.0
        val tatNonCompliant = totalCompleted - tatMet
        val avgTAT = if (tatMetData.isNotEmpty()) {
            tatMetData.map { it.second }.average()
        } else {
            0.0
        }

        return LabDashboardSummary(
            totalOrders = totalOrders,
            pendingResults = pendingResults,
            criticalAlertsCount = criticalAlerts.count { it.acknowledgedAt == null },
            equipmentIssuesCount = equipmentIssues.size,
            reagentExpiryAlertsCount = reagentExpiries.size,
            averageTATHours = avgTAT,
            tatCompliancePercent = tatCompliancePercent,
            tatNonCompliantOrders = tatNonCompliant,
        )
    }

    private fun addDays(isoDate: String, days: Int): String {
        return try {
            val instant = Instant.parse(isoDate)
            val newInstant = Instant.fromEpochMilliseconds(
                instant.toEpochMilliseconds() + (days * 24L * 60L * 60L * 1000L)
            )
            newInstant.toString()
        } catch (e: Exception) {
            isoDate
        }
    }
}

// ============================================================================
// DATA CLASSES
// ============================================================================

data class ProcessedLabResult(
    val result: LabResultExtended,
    val validationResult: ValidationResult,
    val criticalAlert: LabCriticalAlert? = null,
    val reflexiveTestsNeeded: Boolean = false,
)

data class ProcessedQCResult(
    val qcResult: LabQCResult,
    val assessment: QCAssessment,
    val materialStatus: MaterialValidityStatus,
    val trendAnalysis: TrendAnalysis,
    val canContinueTesting: Boolean,
    val alerts: List<String>,
    val recommendations: List<String>,
)

data class EquipmentQCStatus(
    val equipmentId: String,
    val isCompliant: Boolean,
    val lastQCResult: LabQCResult?,
    val dayssinceLastQC: Double?,
    val message: String,
    val action: String,  // "OK", "RUN_QC_IMMEDIATELY", "RUN_QC_BEFORE_USE", "DO_NOT_USE"
)

data class TATMonitoringStatus(
    val tracking: LabTATTracking,
    val complianceStatus: TATComplianceStatus,
    val tatCalculation: TATCalculation,
    val bottlenecks: List<String>,
    val needsIntervention: Boolean,
)

data class TATUrgencyItem(
    val orderId: String,
    val patientId: String,
    val testName: String,
    val priority: LabPriority,
    val status: String,
    val percentageUsed: Double,
    val hoursRemaining: Int,
    val message: String,
)

data class CalibrationStatus(
    val status: String,
    val message: String,
    val isAlert: Boolean,
    val action: String,
)

data class ReagentStatusCheck(
    val reagentId: String,
    val expiryStatus: String,
    val stockStatus: String,
    val isAlert: Boolean,
    val message: String,
)
