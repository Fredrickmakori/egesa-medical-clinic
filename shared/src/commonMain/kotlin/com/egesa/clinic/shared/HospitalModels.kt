package com.egesa.clinic.shared

import kotlinx.serialization.Serializable

enum class WorkflowArea {
    RECEPTION,
    CONSULTATION,
    DIAGNOSIS,
    WARDS,
    ADMIN,
    REPORTS,
    SETTINGS
}

enum class UserRole {
    RECEPTIONIST,
    DOCTOR,
    NURSE,
    ADMIN
}

enum class TimelineType {
    VISIT,
    DIAGNOSIS,
    ORDER,
    MEDICATION,
    WARD_TRANSFER
}

enum class SaveState {
    DRAFT_SAVED,
    UNSAVED_CHANGES,
    FINAL_SIGN_OFF
}

data class TimelineEvent(
    val title: String,
    val details: String,
    val timestamp: String,
    val type: TimelineType
)

data class EncounterForm(
    val chiefComplaint: String = "",
    val history: String = "",
    val examinationFindings: String = "",
    val provisionalDiagnosis: String = ""
)

data class GlobalNavItem(
    val area: WorkflowArea,
    val label: String,
    val visibleTo: Set<UserRole>,
    val visibilityAnnotation: String
)

data class GlobalAction(
    val id: String,
    val label: String
)


data class Patient(
    val id: String,
    val fullName: String,
    val age: Int,
    val sex: String,
    val status: String,
    val assignedWard: String? = null,
    val visits: Int = 0,
    val activeDiagnosis: String = "",
    val currentMedications: List<String> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList()
)

data class DashboardMetric(
    val title: String,
    val value: String,
    val subtitle: String? = null
)

data class TrendPoint(
    val label: String,
    val value: Int
)

data class DepartmentMetric(
    val department: String,
    val throughput: Int,
    val avgTurnaroundMinutes: Int
)

data class BottleneckCell(
    val workflowStage: String,
    val severity: String,
    val pendingCount: Int
)

data class UserAccount(
    val id: String,
    val fullName: String,
    val role: String,
    val active: Boolean,
    val passwordResetRequired: Boolean
)

data class AuditEvent(
    val user: String,
    val action: String,
    val module: String,
    val timestamp: String,
    val contextReference: String
)

data class ConfigDictionary(
    val title: String,
    val entries: List<String>
)

@Serializable
data class QueueItem(
    val patientId: String,
    val name: String,
    val triageLevel: Int,
    val waitMinutes: Int
)

@Serializable
data class WardBed(
    val bedId: String,
    val wardName: String,
    val occupiedBy: String? = null
)

data class CloudSyncConfig(
    val baseUrl: String,
    val anonKey: String
)

enum class PaymentCategory {
    SERVICE,
    PHARMACY
}

enum class PaymentStatus {
    PENDING,
    STK_SENT,
    SUCCESS,
    FAILED,
    CANCELLED
}

data class MpesaPaymentRequest(
    val patientId: String,
    val phoneNumber: String,
    val amount: Double,
    val accountRef: String,
    val description: String,
    val category: PaymentCategory
)

data class MpesaPaymentResult(
    val checkoutRequestId: String,
    val merchantRequestId: String,
    val status: PaymentStatus,
    val receiptNumber: String? = null,
    val resultCode: String? = null,
    val resultDesc: String? = null
)

data class PaymentRecord(
    val paymentId: String,
    val patientId: String,
    val amount: Double,
    val category: PaymentCategory,
    val status: PaymentStatus,
    val timestamp: Long,
    val billReference: String? = null,
    val visitReference: String? = null,
    val checkoutRequestId: String? = null,
    val merchantRequestId: String? = null,
    val receiptNumber: String? = null
)

data class OutstandingBill(
    val billId: String,
    val patientId: String,
    val amountDue: Double,
    val category: PaymentCategory,
    val description: String
)

interface RecordSyncClient {
    suspend fun uploadPatients(patients: List<Patient>)
    suspend fun fetchPatients(): List<Patient>
    suspend fun submitPaymentEvent(paymentRecord: PaymentRecord)
    suspend fun checkStkStatus(stkRequestId: String): StkRequestStatus
}

class PaymentSyncManager(
    private val syncClient: RecordSyncClient
) {
    private val unsyncedQueue = mutableListOf<PaymentRecord>()

    fun queueForSync(record: PaymentRecord) {
        unsyncedQueue += record.copy(synced = false)
    }

    fun pendingRecords(): List<PaymentRecord> = unsyncedQueue.toList()

    suspend fun flushQueue(): List<PaymentRecord> {
        if (unsyncedQueue.isEmpty()) return emptyList()

        val flushed = mutableListOf<PaymentRecord>()
        val iterator = unsyncedQueue.listIterator()
        while (iterator.hasNext()) {
            val record = iterator.next()
            val attempt = record.retryCount + 1
            try {
                syncClient.submitPaymentEvent(record)
                flushed += record.copy(
                    synced = true,
                    lastSyncedAt = Clock.System.now(),
                    retryCount = attempt,
                    syncError = null
                )
                iterator.remove()
            } catch (exception: Exception) {
                iterator.set(
                    record.copy(
                        retryCount = attempt,
                        syncError = exception.message ?: "sync failed"
                    )
                )
            }
        }
        return flushed
    }
}

class HospitalState {
    private val patients = listOf(
        Patient(
            id = "PT-001",
            fullName = "Amina Yusuf",
            age = 34,
            sex = "F",
            status = "Awaiting consultation",
            visits = 4,
            activeDiagnosis = "Acute pharyngitis",
            currentMedications = listOf("Paracetamol 500mg", "Cetirizine 10mg"),
            timeline = listOf(
                TimelineEvent("Outpatient visit", "Fever and sore throat", "2026-04-29 09:30", TimelineType.VISIT),
                TimelineEvent("Provisional diagnosis", "Acute pharyngitis", "2026-04-29 09:50", TimelineType.DIAGNOSIS),
                TimelineEvent("Lab order", "CBC", "2026-04-29 09:55", TimelineType.ORDER),
                TimelineEvent("Medication prescribed", "Paracetamol 500mg", "2026-04-29 10:05", TimelineType.MEDICATION)
            )
        ),
        Patient(
            id = "PT-002",
            fullName = "John Ouma",
            age = 58,
            sex = "M",
            status = "In diagnosis",
            visits = 7,
            activeDiagnosis = "Hypertensive urgency",
            currentMedications = listOf("Amlodipine 5mg"),
            timeline = listOf(
                TimelineEvent("ER visit", "Headache and high BP", "2026-05-01 08:10", TimelineType.VISIT),
                TimelineEvent("Imaging request", "CT brain", "2026-05-01 08:40", TimelineType.ORDER),
                TimelineEvent("Ward transfer", "To Observation Ward", "2026-05-01 09:00", TimelineType.WARD_TRANSFER)
            )
        ),
        Patient(
            id = "PT-003",
            fullName = "Martha Wekesa",
            age = 12,
            sex = "F",
            status = "Admitted",
            assignedWard = "Pediatrics",
            visits = 2,
            activeDiagnosis = "Severe malaria",
            currentMedications = listOf("Artemether/Lumefantrine"),
            timeline = listOf(
                TimelineEvent("Admission", "High fever and chills", "2026-04-30 14:20", TimelineType.VISIT),
                TimelineEvent("Diagnosis", "Severe malaria", "2026-04-30 15:00", TimelineType.DIAGNOSIS),
                TimelineEvent("Medication", "Artemether/Lumefantrine", "2026-04-30 15:10", TimelineType.MEDICATION)
            )
        )
    )

    fun allPatients(): List<Patient> = patients
    private val navItems = listOf(
        GlobalNavItem(WorkflowArea.RECEPTION, "Reception", setOf(UserRole.RECEPTIONIST, UserRole.ADMIN), "Receptionist (Reception)"),
        GlobalNavItem(WorkflowArea.CONSULTATION, "Consultation", setOf(UserRole.DOCTOR, UserRole.ADMIN), "Doctor (Consultation)"),
        GlobalNavItem(WorkflowArea.DIAGNOSIS, "Diagnosis", setOf(UserRole.DOCTOR, UserRole.ADMIN), "Doctor (Diagnosis)"),
        GlobalNavItem(WorkflowArea.WARDS, "Wards", setOf(UserRole.NURSE, UserRole.ADMIN), "Nurse (Wards)"),
        GlobalNavItem(WorkflowArea.ADMIN, "Admin", setOf(UserRole.ADMIN), "Admin (All + Reports/Settings)"),
        GlobalNavItem(WorkflowArea.REPORTS, "Reports", setOf(UserRole.ADMIN), "Admin (All + Reports/Settings)"),
        GlobalNavItem(WorkflowArea.SETTINGS, "Settings", setOf(UserRole.ADMIN), "Admin (All + Reports/Settings)")
    )

    private val globalActions = listOf(
        GlobalAction("patient_search", "Patient Search"),
        GlobalAction("quick_register", "Quick Register"),
        GlobalAction("alerts_notifications", "Alerts/Notifications"),
        GlobalAction("user_profile_switch_role", "User Profile / Switch Role")
    )

    fun allPatients(): List<Patient> = patients.toList()

    fun adminKpis(): List<DashboardMetric> = listOf(
        DashboardMetric("Registrations / Day", "126", "+8% vs yesterday"),
        DashboardMetric("Consultation Throughput", "93", "patients completed"),
        DashboardMetric("Avg Turnaround", "48 min", "triage → discharge"),
        DashboardMetric("Ward Occupancy", "82%", "164 / 200 beds"),
        DashboardMetric("Discharge Rate", "71%", "within 72 hours")
    )

    fun registrationTrend(): List<TrendPoint> = listOf(
        TrendPoint("Mon", 104),
        TrendPoint("Tue", 110),
        TrendPoint("Wed", 122),
        TrendPoint("Thu", 118),
        TrendPoint("Fri", 126)
    )

    fun departmentComparison(): List<DepartmentMetric> = listOf(
        DepartmentMetric("Emergency", 44, 37),
        DepartmentMetric("Outpatient", 68, 44),
        DepartmentMetric("Pediatrics", 39, 51),
        DepartmentMetric("Maternity", 31, 56),
        DepartmentMetric("Surgery", 22, 73)
    )

    fun bottleneckHeatmap(): List<BottleneckCell> = listOf(
        BottleneckCell("Triage", "Medium", 9),
        BottleneckCell("Consultation", "High", 17),
        BottleneckCell("Lab", "Critical", 21),
        BottleneckCell("Pharmacy", "Low", 4),
        BottleneckCell("Discharge", "Medium", 11)
    )

    fun users(): List<UserAccount> = listOf(
        UserAccount("USR-101", "Faith Njeri", "Administrator", true, false),
        UserAccount("USR-204", "Samuel Otieno", "Clinician", true, true),
        UserAccount("USR-317", "Janet Kilonzo", "Cashier", false, false)
    )

    fun auditLog(): List<AuditEvent> = listOf(
        AuditEvent("Faith Njeri", "Created user", "User Management", "2026-05-01 08:12", "USR-317"),
        AuditEvent("Samuel Otieno", "Updated diagnosis", "Consultation", "2026-05-01 09:04", "PT-002"),
        AuditEvent("Janet Kilonzo", "Generated invoice", "Billing", "2026-05-01 09:33", "INV-8821 / PT-001"),
        AuditEvent("Faith Njeri", "Activated ward", "Configuration", "2026-05-01 10:02", "Ward: Surgical B")
    )

    fun configurationSets(): List<ConfigDictionary> = listOf(
        ConfigDictionary("Departments", listOf("Emergency", "Outpatient", "Pediatrics", "Maternity", "Surgery")),
        ConfigDictionary("Wards", listOf("General A", "General B", "Pediatrics", "Maternity", "Surgical B")),
        ConfigDictionary("Billing Categories", listOf("Consultation", "Laboratory", "Imaging", "Procedure", "Medication")),
        ConfigDictionary("Status Dictionaries", listOf("Awaiting Consultation", "In Diagnosis", "Admitted", "Ready for Discharge", "Discharged"))
    )

    fun globalNavItemsFor(role: UserRole): List<GlobalNavItem> = navItems.filter { role in it.visibleTo }

    fun globalActions(): List<GlobalAction> = globalActions.toList()

    fun breadcrumbFor(area: WorkflowArea): List<String> = listOf("Home", area.name.lowercase().replaceFirstChar { it.uppercase() }, "Workflow")
}
