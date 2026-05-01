package com.egesa.clinic.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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

data class PaymentRecord(
    val id: String,
    val patientId: String,
    val amount: Double,
    val createdAt: Instant,
    val stkRequestId: String? = null,
    val stkStatus: StkRequestStatus = StkRequestStatus.PENDING,
    val synced: Boolean = false,
    val lastSyncedAt: Instant? = null,
    val retryCount: Int = 0,
    val syncError: String? = null
)

data class PaymentSyncHealth(
    val pendingSyncCount: Int,
    val failedSyncCount: Int
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
        paymentRecords.replaceAll { record ->
            if (record.stkRequestId == null || record.stkStatus != StkRequestStatus.PENDING) {
                return@replaceAll record
            }
            val newStatus = checkStatus(record.stkRequestId)
            if (newStatus == StkRequestStatus.PENDING) return@replaceAll record
            updated += 1
            record.copy(
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

    fun metrics(): List<DashboardMetric> {
        val syncHealth = syncHealth()
        return listOf(
            DashboardMetric("Registered Today", patients.size.toString()),
            DashboardMetric("In Wards", patients.count { it.status == PatientStatus.ADMITTED }.toString()),
            DashboardMetric("Pending Consultation", patients.count { it.status == PatientStatus.WAITING }.toString()),
            DashboardMetric("Active Clinicians", patients.mapNotNull { it.clinician }.distinct().size.toString()),
            DashboardMetric("Pending Sync", syncHealth.pendingSyncCount.toString()),
            DashboardMetric("Failed Sync", syncHealth.failedSyncCount.toString())
        )
    }
}
