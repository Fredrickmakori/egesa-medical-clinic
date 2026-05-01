package com.egesa.clinic.shared

import kotlinx.serialization.Serializable

enum class WorkflowArea {
    RECEPTION,
    CONSULTATION,
    DIAGNOSIS,
    WARD,
    ADMIN
}

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

    fun paymentRecordsByStatus(status: PaymentStatus): List<PaymentRecord> =
        paymentRecords.filter { it.status == status }

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
}
