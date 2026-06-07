package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.domain.*
import kotlinx.datetime.Clock

/**
 * Enhanced Lab Repository Interface
 * Defines all contracts for lab data operations
 */
interface EnhancedLabRepository : LabRepository {

    // ========================================================================
    // LAB TESTS & PANELS
    // ========================================================================

    suspend fun createLabTest(test: LabTestDetail): LabTestDetail
    suspend fun getLabTest(id: String): LabTestDetail?
    suspend fun getLabTestByCode(code: String): LabTestDetail?
    suspend fun getAllLabTests(category: String? = null): List<LabTestDetail>
    suspend fun updateLabTest(test: LabTestDetail): LabTestDetail
    suspend fun searchLabTests(query: String): List<LabTestDetail>

    suspend fun createLabPanel(panel: LabTestPanel): LabTestPanel
    suspend fun getLabPanel(id: String): LabTestPanel?
    suspend fun getAllLabPanels(): List<LabTestPanel>
    suspend fun updateLabPanel(panel: LabTestPanel): LabTestPanel

    suspend fun createReferenceRange(range: LabReferenceRange): LabReferenceRange
    suspend fun getReferenceRange(testId: String, ageGroup: String, gender: String): LabReferenceRange?
    suspend fun getAllReferenceRanges(testId: String): List<LabReferenceRange>

    // ========================================================================
    // SPECIMENS
    // ========================================================================

    suspend fun createSpecimen(specimen: LabSpecimen): LabSpecimen
    suspend fun getSpecimen(id: String): LabSpecimen?
    suspend fun getSpecimenByAccessionNumber(accessionNumber: String): LabSpecimen?
    suspend fun getSpecimensForOrder(orderId: String): List<LabSpecimen>
    suspend fun updateSpecimen(specimen: LabSpecimen): LabSpecimen
    suspend fun getSpecimensByStatus(status: SpecimenStatus): List<LabSpecimen>
    suspend fun getRejectionReasons(): List<LabRejectionReason>

    // ========================================================================
    // EQUIPMENT & MAINTENANCE
    // ========================================================================

    suspend fun createEquipment(equipment: LabEquipment): LabEquipment
    suspend fun getEquipment(id: String): LabEquipment?
    suspend fun getEquipmentByCode(code: String): LabEquipment?
    suspend fun getAllEquipment(status: EquipmentStatus? = null): List<LabEquipment>
    suspend fun updateEquipment(equipment: LabEquipment): LabEquipment

    suspend fun createCalibration(calibration: LabEquipmentCalibration): LabEquipmentCalibration
    suspend fun getCalibrationHistory(equipmentId: String): List<LabEquipmentCalibration>
    suspend fun getLatestCalibration(equipmentId: String): LabEquipmentCalibration?

    suspend fun createMaintenance(maintenance: LabEquipmentMaintenance): LabEquipmentMaintenance
    suspend fun getMaintenanceHistory(equipmentId: String): List<LabEquipmentMaintenance>
    suspend fun getEquipmentNeedingMaintenance(): List<LabEquipment>

    // ========================================================================
    // QUALITY CONTROL
    // ========================================================================

    suspend fun createQCMaterial(material: LabQCMaterial): LabQCMaterial
    suspend fun getQCMaterial(id: String): LabQCMaterial?
    suspend fun getQCMaterialsByEquipment(equipmentId: String): List<LabQCMaterial>
    suspend fun getAllQCMaterials(): List<LabQCMaterial>
    suspend fun updateQCMaterial(material: LabQCMaterial): LabQCMaterial

    suspend fun createQCResult(result: LabQCResult): LabQCResult
    suspend fun getQCResult(id: String): LabQCResult?
    suspend fun getQCResultsByMaterial(materialId: String, daysBack: Int = 30): List<LabQCResult>
    suspend fun getQCResultsByEquipment(equipmentId: String, daysBack: Int = 30): List<LabQCResult>
    suspend fun getRecentQCResults(equipmentId: String, limit: Int = 5): List<LabQCResult>

    // ========================================================================
    // REAGENTS & CONSUMABLES
    // ========================================================================

    suspend fun createReagent(reagent: LabReagent): LabReagent
    suspend fun getReagent(id: String): LabReagent?
    suspend fun getReagentByCode(code: String): LabReagent?
    suspend fun getAllReagents(status: ReagentStatus? = null): List<LabReagent>
    suspend fun updateReagent(reagent: LabReagent): LabReagent
    suspend fun getExpiringReagents(daysThreshold: Int = 30): List<LabReagent>
    suspend fun getLowStockReagents(): List<LabReagent>

    // ========================================================================
    // STAFF COMPETENCIES
    // ========================================================================

    suspend fun createStaffCompetency(competency: LabStaffCompetency): LabStaffCompetency
    suspend fun getStaffCompetency(staffId: String, testId: String): LabStaffCompetency?
    suspend fun getStaffCompetencies(staffId: String): List<LabStaffCompetency>
    suspend fun updateStaffCompetency(competency: LabStaffCompetency): LabStaffCompetency
    suspend fun getStaffEligibleForTest(testId: String): List<LabStaffCompetency>

    // ========================================================================
    // RESULTS & VALIDATION
    // ========================================================================

    suspend fun saveExtendedResults(
        orderId: String,
        results: List<LabResultExtended>,
        actorId: String
    ): List<LabResultExtended>

    suspend fun getLabResult(id: String): LabResultExtended?
    suspend fun getLabResultsByOrder(orderId: String): List<LabResultExtended>
    suspend fun getLabResultsBySpecimen(specimenId: String): List<LabResultExtended>
    suspend fun updateLabResultStatus(
        resultId: String,
        status: ResultStatus,
        actorId: String
    ): LabResultExtended?
    suspend fun getPreviousResult(patientId: String, testId: String): LabResultExtended?

    // ========================================================================
    // CRITICAL ALERTS
    // ========================================================================

    suspend fun createCriticalAlert(alert: LabCriticalAlert): LabCriticalAlert
    suspend fun getCriticalAlert(id: String): LabCriticalAlert?
    suspend fun getPendingCriticalAlerts(): List<LabCriticalAlert>
    suspend fun getCriticalAlertsByPriority(): List<LabCriticalAlert>
    suspend fun updateCriticalAlertAcknowledged(
        alertId: String,
        acknowledgedBy: String,
        notes: String? = null
    ): LabCriticalAlert?

    // ========================================================================
    // TURNAROUND TIME TRACKING
    // ========================================================================

    suspend fun createTATTracking(tracking: LabTATTracking): LabTATTracking
    suspend fun getTATTracking(orderId: String): LabTATTracking?
    suspend fun updateTATTracking(tracking: LabTATTracking): LabTATTracking
    suspend fun updateTATMilestone(
        orderId: String,
        milestone: String,
        timestamp: String
    ): LabTATTracking?
    suspend fun getNonCompliantTATs(): List<LabTATTracking>
    suspend fun getTATComplianceData(fromDate: String, toDate: String): List<LabTATTracking>

    // ========================================================================
    // AUDIT & STATUS TRACKING
    // ========================================================================

    suspend fun createStatusAudit(audit: LabOrderStatusAudit): LabOrderStatusAudit
    suspend fun getStatusAuditTrail(orderId: String): List<LabOrderStatusAudit>
    suspend fun getStatusAuditTrailForSpecimen(specimenId: String): List<LabOrderStatusAudit>

    // ========================================================================
    // DASHBOARD & REPORTING
    // ========================================================================

    suspend fun getDashboardSummary(): LabDashboardSummary
    suspend fun getEquipmentStatusSummary(): List<LabEquipmentStatusSummary>
    suspend fun getReagentExpiryAlerts(): List<LabReagentExpirySummary>
    suspend fun getPendingResultsCount(): Int
    suspend fun getTATComplianceMetrics(fromDate: String, toDate: String): TATComplianceMetrics
}

data class TATComplianceMetrics(
    val totalOrders: Int,
    val onTimeOrders: Int,
    val lateOrders: Int,
    val compliancePercent: Double,
    val averageTATHours: Double,
    val maxTATHours: Double,
    val minTATHours: Double,
)

