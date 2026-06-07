package com.egesa.clinic.shared.domain

import kotlinx.serialization.Serializable

// ============================================================================
// LAB TEST MANAGEMENT
// ============================================================================

@Serializable
data class LabTestDetail(
    val id: String,
    val code: String,
    val name: String,
    val description: String? = null,
    val category: String,  // e.g., "Hematology", "Biochemistry"
    val specimenType: String,  // e.g., "Whole Blood", "Serum", "Urine"
    val specimenVolumeMl: Double? = null,
    val specimenContainer: String? = null,
    val specimenStorageTemp: String? = null,  // "Room Temp", "2-8C", "-20C"
    val specimenStabilityHours: Int? = null,
    val turnaroundTimeHours: Int = 24,
    val loincCode: String? = null,
    val defaultUnit: String? = null,
    val defaultReferenceRangeMale: String? = null,
    val defaultReferenceRangeFemale: String? = null,
    val defaultReferenceRangePediatric: String? = null,
    val billingCode: String,
    val price: Double,
    val cost: Double? = null,
    val requiresFasting: Boolean = false,
    val requiresSpecialPreparation: String? = null,
    val isCriticalValueTest: Boolean = false,
    val criticalValueLow: Double? = null,
    val criticalValueHigh: Double? = null,
    val method: String? = null,
    val equipmentId: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class LabTestPanel(
    val id: String,
    val code: String,
    val name: String,
    val description: String? = null,
    val category: String,
    val testIds: List<String>,  // References to LabTestDetail
    val specimenType: String,
    val turnaroundTimeHours: Int = 24,
    val billingCode: String,
    val price: Double,
    val cost: Double? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class LabReferenceRange(
    val id: String,
    val testId: String,
    val ageGroup: String,  // "Adult", "Child (5-12)", "Infant (0-1)"
    val gender: String,  // "M", "F", "All"
    val unit: String,
    val rangeLow: Double,
    val rangeHigh: Double,
    val criticalLow: Double? = null,
    val criticalHigh: Double? = null,
    val notes: String? = null,
    val effectiveFrom: String? = null,
    val effectiveTo: String? = null,
)

// ============================================================================
// LAB SPECIMEN AND SAMPLE MANAGEMENT
// ============================================================================

@Serializable
enum class SpecimenStatus {
    COLLECTED, RECEIVED, ACCEPTED, REJECTED, TESTING, COMPLETED
}

@Serializable
data class LabSpecimen(
    val id: String,
    val accessionNumber: String? = null,  // Barcode/tracking number
    val orderId: String,
    val patientId: String,
    val testId: String? = null,
    val specimenType: String,
    val containerId: String? = null,  // Physical barcode
    val volumeActualMl: Double? = null,
    val volumeRequiredMl: Double? = null,
    val collectedBy: String,
    val collectedAt: String,
    val receivedBy: String? = null,
    val receivedAt: String? = null,
    val sampleQuality: String? = null,  // "Acceptable", "Hemolyzed", "Clotted"
    val rejectionReason: String? = null,
    val rejectionReasonId: String? = null,
    val storageLocation: String? = null,  // "Fridge A, Shelf 2, Position 15"
    val storageTemperature: String? = null,
    val status: SpecimenStatus = SpecimenStatus.COLLECTED,
    val chainOfCustodyComplete: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class LabRejectionReason(
    val id: String,
    val code: String,
    val reason: String,
    val category: String,  // "Pre-analytical", "Collection", "Storage"
    val severity: String,  // "Minor", "Major"
    val isActive: Boolean = true,
)

// ============================================================================
// LAB EQUIPMENT AND INSTRUMENTS
// ============================================================================

@Serializable
enum class EquipmentStatus {
    ACTIVE, INACTIVE, MAINTENANCE, DECOMMISSIONED
}

@Serializable
data class LabEquipment(
    val id: String,
    val code: String,
    val name: String,
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val equipmentType: String,  // "Hematology Analyzer", "Chemistry Analyzer"
    val location: String? = null,
    val acquisitionDate: String? = null,
    val warrantyExpiryDate: String? = null,
    val purchaseCost: Double? = null,
    val status: EquipmentStatus = EquipmentStatus.ACTIVE,
    val operationalFrom: String? = null,
    val operationalUntil: String? = null,
    val lastCalibrationDate: String? = null,
    val nextCalibrationDue: String? = null,
    val lastMaintenanceDate: String? = null,
    val nextMaintenanceDue: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class LabEquipmentCalibration(
    val id: String,
    val equipmentId: String,
    val calibrationDate: String,
    val calibrationTime: String? = null,
    val performedBy: String,
    val calibrationResult: String,  // "PASSED", "FAILED", "OUT_OF_SPEC"
    val notes: String? = null,
    val certificateUrl: String? = null,
    val nextDueDate: String? = null,
    val createdAt: String,
)

@Serializable
data class LabEquipmentMaintenance(
    val id: String,
    val equipmentId: String,
    val maintenanceDate: String,
    val maintenanceType: String,  // "PREVENTIVE", "CORRECTIVE"
    val performedBy: String? = null,
    val maintenanceContractor: String? = null,
    val description: String? = null,
    val partsReplaced: String? = null,
    val cost: Double? = null,
    val notes: String? = null,
    val status: String,  // "SCHEDULED", "IN_PROGRESS", "COMPLETED"
    val downtimeHours: Double? = null,
    val createdAt: String,
)

// ============================================================================
// LAB QUALITY CONTROL
// ============================================================================

@Serializable
data class LabQCMaterial(
    val id: String,
    val code: String,
    val name: String,
    val materialType: String,  // "Control", "Calibrator", "Reference Standard"
    val lotNumber: String,
    val manufacturer: String? = null,
    val receivedDate: String,
    val expiryDate: String,
    val storageLocation: String,
    val testIds: List<String>,
    val expectedValue: Double,
    val expectedRangeLow: Double,
    val expectedRangeHigh: Double,
    val unit: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
)

@Serializable
enum class QCResultStatus {
    PASSED, WARNING, FAILED
}

@Serializable
data class LabQCResult(
    val id: String,
    val qcMaterialId: String,
    val equipmentId: String,
    val testId: String? = null,
    val qcDate: String,
    val qcTime: String? = null,
    val resultValue: Double,
    val unit: String? = null,
    val status: QCResultStatus,
    val controlNumber: Int? = null,  // 1st run, 2nd run, etc.
    val performedBy: String? = null,
    val meanDeviation: Double? = null,
    val sdDeviation: Double? = null,  // Standard deviation multiple
    val notes: String? = null,
    val createdAt: String,
)

// ============================================================================
// LAB REAGENTS AND CONSUMABLES
// ============================================================================

@Serializable
enum class ReagentStatus {
    IN_USE, RESERVED, EXPIRED, DEPLETED
}

@Serializable
data class LabReagent(
    val id: String,
    val code: String,
    val name: String,
    val reagentType: String,  // "Reagent", "Control", "Calibrator"
    val manufacturer: String? = null,
    val lotNumber: String,
    val catalogNumber: String? = null,
    val receivedDate: String,
    val expiryDate: String,
    val storageLocation: String? = null,
    val storageTemperature: String? = null,  // "Room Temp", "2-8C", "-20C"
    val quantity: Double,
    val unitOfMeasurement: String,  // "mL", "Units", "Doses"
    val usageCount: Int = 0,
    val testIds: List<String>,
    val reorderLevel: Double? = null,
    val status: ReagentStatus = ReagentStatus.IN_USE,
    val cost: Double? = null,
    val supplierId: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
)

// ============================================================================
// LAB STAFF COMPETENCIES
// ============================================================================

@Serializable
enum class CompetencyLevel {
    TRAINEE, COMPETENT, ADVANCED, SUPERVISOR
}

@Serializable
data class LabStaffCompetency(
    val id: String,
    val staffId: String,
    val testId: String,
    val competencyLevel: CompetencyLevel,
    val certificationDate: String? = null,
    val certificationExpiry: String? = null,
    val certifiedBy: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
)

// ============================================================================
// LAB RESULTS EXTENDED
// ============================================================================

@Serializable
enum class ResultStatus {
    PRELIMINARY, VERIFIED, REPORTED, CORRECTED, CANCELLED
}

@Serializable
data class LabResultExtended(
    val id: String,
    val orderId: String,
    val orderItemId: String,
    val patientId: String,
    val testId: String,
    val testCode: String,
    val testName: String,
    val value: String,
    val valueNumeric: Double? = null,
    val unit: String? = null,
    val referenceRange: String? = null,
    val flag: LabResultFlag? = null,
    val comment: String? = null,
    val enteredBy: String,
    val enteredAt: String,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val reportedBy: String? = null,
    val reportedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    // Extended fields
    val specimenId: String? = null,
    val resultStatus: ResultStatus = ResultStatus.PRELIMINARY,
    val isCriticalValue: Boolean = false,
    val criticalNotificationSent: Boolean = false,
    val previousResultValue: String? = null,
    val deltaCheckPerformed: Boolean = false,
    val deltaCheckPassed: Boolean? = null,
    val reflexiveTestOrdered: Boolean = false,
    val methodUsed: String? = null,
    val qcPerformanceId: String? = null,
)

// ============================================================================
// LAB AUDIT AND TRACKING
// ============================================================================

@Serializable
data class LabOrderStatusAudit(
    val id: String,
    val orderId: String,
    val specimenId: String? = null,
    val resultId: String? = null,
    val fromStatus: String,
    val toStatus: String,
    val changedBy: String,
    val changedAt: String,
    val reason: String? = null,
    val notes: String? = null,
    val createdAt: String,
)

@Serializable
enum class AlertSeverity {
    INFO, WARNING, CRITICAL
}

@Serializable
data class LabCriticalAlert(
    val id: String,
    val resultId: String,
    val orderId: String,
    val patientId: String,
    val testName: String,
    val criticalValue: Double,
    val thresholdType: String,  // "HIGH", "LOW", "PANIC"
    val alertSeverity: AlertSeverity,
    val notifiedToRoles: List<String> = listOf("DOCTOR", "LAB_SUPERVISOR"),
    val notificationSentAt: String? = null,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: String? = null,
    val acknowledgementNotes: String? = null,
    val createdAt: String,
)

@Serializable
data class LabTATTracking(
    val id: String,
    val orderId: String,
    val testId: String? = null,
    val orderedAt: String,
    val specimenCollectedAt: String? = null,
    val specimenReceivedAt: String? = null,
    val analysisStartedAt: String? = null,
    val analysisCompletedAt: String? = null,
    val resultVerifiedAt: String? = null,
    val resultReportedAt: String? = null,
    val expectedTatHours: Int,
    val actualTatHours: Double? = null,
    val tatMet: Boolean? = null,
    val delayReasons: String? = null,
    val createdAt: String,
)

// ============================================================================
// SUMMARY VIEWS
// ============================================================================

@Serializable
data class LabDashboardSummary(
    val totalOrders: Int,
    val pendingResults: Int,
    val criticalAlertsCount: Int,
    val equipmentIssuesCount: Int,
    val reagentExpiryAlertsCount: Int,
    val averageTATHours: Double,
    val tatCompliancePercent: Double,
    val tatNonCompliantOrders: Int,
)

@Serializable
data class LabEquipmentStatusSummary(
    val equipmentId: String,
    val code: String,
    val name: String,
    val status: EquipmentStatus,
    val calibrationStatus: String,  // "OVERDUE", "DUE_SOON", "OK"
    val maintenanceStatus: String,  // "OVERDUE", "DUE_SOON", "OK"
)

@Serializable
data class LabReagentExpirySummary(
    val reagentId: String,
    val code: String,
    val name: String,
    val expiryDate: String,
    val expiryStatus: String,  // "EXPIRED", "EXPIRING_SOON", "OK"
    val stockStatus: String,  // "REORDER_NEEDED", "IN_STOCK"
    val quantity: Double,
    val reorderLevel: Double? = null,
)

