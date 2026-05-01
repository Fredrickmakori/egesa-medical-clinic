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

<<<<<<< codex/create-app-shell-frame-templates-and-navigation
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

=======
object PatientStatus {
    const val WAITING = "WAITING"
    const val IN_CONSULTATION = "IN_CONSULTATION"
    const val IN_DIAGNOSIS = "IN_DIAGNOSIS"
    const val ADMITTED = "ADMITTED"
}

enum class StkRequestStatus {
    PENDING,
    SUCCESS,
    FAILED
}

@Serializable
>>>>>>> main
data class Patient(
    val id: String,
    val fullName: String,
    val age: Int,
    val sex: String,
    val status: String,
    val assignedWard: String? = null,
    val triageLevel: Int = 3,
    val clinician: String? = null,
    val diagnosis: String? = null
)

data class DashboardMetric(
    val title: String,
    val value: String
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
    private val patients = mutableListOf(
        Patient("PT-001", "Amina Yusuf", 34, "F", PatientStatus.WAITING, triageLevel = 2),
        Patient("PT-002", "John Ouma", 58, "M", PatientStatus.IN_DIAGNOSIS, clinician = "Dr. Otieno", diagnosis = "Hypertension"),
        Patient("PT-003", "Martha Wekesa", 12, "F", PatientStatus.ADMITTED, "Pediatrics", triageLevel = 2, clinician = "Dr. Naliaka"),
        Patient("PT-004", "Daniel Mwangi", 41, "M", PatientStatus.IN_CONSULTATION, clinician = "Dr. Achieng")
    )
    private val paymentRecords = mutableListOf<PaymentRecord>()

    private val wardBeds = listOf(
        WardBed("PED-01", "Pediatrics", "PT-003"),
        WardBed("PED-02", "Pediatrics", null),
        WardBed("GEN-01", "General", null),
        WardBed("GEN-02", "General", null)
    )

    private val outstandingBills = mutableListOf(
        OutstandingBill("BILL-001", "PT-001", 1200.0, PaymentCategory.SERVICE, "General consultation"),
        OutstandingBill("BILL-002", "PT-002", 850.0, PaymentCategory.PHARMACY, "Hypertension medication"),
        OutstandingBill("BILL-003", "PT-003", 3000.0, PaymentCategory.SERVICE, "Pediatric ward admission")
    )

    private val paymentRecords = mutableListOf(
        PaymentRecord(
            paymentId = "PAY-001",
            patientId = "PT-002",
            amount = 850.0,
            category = PaymentCategory.PHARMACY,
            status = PaymentStatus.SUCCESS,
            timestamp = 1714550400000,
            billReference = "BILL-002",
            visitReference = "VISIT-002",
            checkoutRequestId = "ws_CO_12345",
            merchantRequestId = "29115-34620561-1",
            receiptNumber = "QHG7T8Y9"
        ),
        PaymentRecord(
            paymentId = "PAY-002",
            patientId = "PT-001",
            amount = 1200.0,
            category = PaymentCategory.SERVICE,
            status = PaymentStatus.PENDING,
            timestamp = 1714636800000,
            billReference = "BILL-001",
            visitReference = "VISIT-001"
        )
    )

    fun allPatients(query: String = ""): List<Patient> {
        if (query.isBlank()) return patients.toList()
        return patients.filter {
            it.fullName.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true)
        }
    }

    fun addPaymentRecord(record: PaymentRecord) {
        paymentRecords += record
    }

    fun paymentRecords(): List<PaymentRecord> = paymentRecords.toList()

    fun updatePaymentRecordStatus(paymentId: String, status: StkRequestStatus, syncError: String? = null) {
        val index = paymentRecords.indexOfFirst { it.id == paymentId }
        if (index < 0) return
        val existing = paymentRecords[index]
        paymentRecords[index] = existing.copy(
            stkStatus = status,
            syncError = syncError,
            lastSyncedAt = Clock.System.now(),
            synced = status == StkRequestStatus.SUCCESS
        )
    }

    fun pendingStkRequests(): List<PaymentRecord> = paymentRecords.filter {
        it.stkRequestId != null && it.stkStatus == StkRequestStatus.PENDING
    }

    fun reconcilePendingStkRequests(checkStatus: (String) -> StkRequestStatus): Int {
        var updated = 0
<<<<<<< codex/add-server-side-scheduled-status-reconciliation-8eg6mq
        for (index in paymentRecords.indices) {
            val record = paymentRecords[index]
            if (record.stkRequestId == null || record.stkStatus != StkRequestStatus.PENDING) {
                continue
            }
            val newStatus = checkStatus(record.stkRequestId)
            if (newStatus == StkRequestStatus.PENDING) {
                continue
            }
            updated += 1
            paymentRecords[index] = record.copy(
=======
        paymentRecords.replaceAll { record ->
            if (record.stkRequestId == null || record.stkStatus != StkRequestStatus.PENDING) {
                return@replaceAll record
            }
            val newStatus = checkStatus(record.stkRequestId)
            if (newStatus == StkRequestStatus.PENDING) return@replaceAll record
            updated += 1
            record.copy(
>>>>>>> main
                stkStatus = newStatus,
                synced = newStatus == StkRequestStatus.SUCCESS,
                lastSyncedAt = Clock.System.now(),
                syncError = if (newStatus == StkRequestStatus.FAILED) "STK charge failed" else null
            )
        }
        return updated
    }

    fun syncHealth(): PaymentSyncHealth {
        val pending = paymentRecords.count { !it.synced }
        val failed = paymentRecords.count { it.syncError != null }
        return PaymentSyncHealth(pendingSyncCount = pending, failedSyncCount = failed)
    }

    fun receptionQueue(): List<QueueItem> = patients
        .filter { it.status == PatientStatus.WAITING }
        .mapIndexed { index, patient ->
            QueueItem(patient.id, patient.fullName, patient.triageLevel, waitMinutes = 12 + (index * 8))
        }

    fun wardBeds(): List<WardBed> = wardBeds

    fun outstandingBills(): List<OutstandingBill> = outstandingBills.toList()

    fun paymentRecords(): List<PaymentRecord> = paymentRecords.toList()

    fun outstandingBillsByPatient(patientId: String): List<OutstandingBill> =
        outstandingBills.filter { it.patientId.equals(patientId, ignoreCase = true) }

    fun paymentRecordsByPatient(patientId: String): List<PaymentRecord> =
        paymentRecords.filter { it.patientId.equals(patientId, ignoreCase = true) }

<<<<<<< codex/create-app-shell-frame-templates-and-navigation
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
=======
    fun paymentRecordsByStatus(status: PaymentStatus): List<PaymentRecord> =
        paymentRecords.filter { it.status == status }
>>>>>>> main

    fun metrics(): List<DashboardMetric> = listOf(
        DashboardMetric("Registered Today", patients.size.toString()),
        DashboardMetric("In Wards", patients.count { it.status == PatientStatus.ADMITTED }.toString()),
        DashboardMetric("Pending Consultation", patients.count { it.status == PatientStatus.WAITING }.toString()),
        DashboardMetric("Active Clinicians", patients.mapNotNull { it.clinician }.distinct().size.toString())
    )

    fun globalNavItemsFor(role: UserRole): List<GlobalNavItem> = navItems.filter { role in it.visibleTo }

    fun globalActions(): List<GlobalAction> = globalActions.toList()

    fun breadcrumbFor(area: WorkflowArea): List<String> = listOf("Home", area.name.lowercase().replaceFirstChar { it.uppercase() }, "Workflow")
}
