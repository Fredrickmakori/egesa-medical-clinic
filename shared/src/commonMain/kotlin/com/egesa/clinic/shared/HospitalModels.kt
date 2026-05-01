package com.egesa.clinic.shared

import kotlinx.serialization.Serializable

enum class WorkflowArea {
    RECEPTION,
    CONSULTATION,
    DIAGNOSIS,
    WARD,
    ADMIN
}

data class Patient(
    val id: String,
    val fullName: String,
    val age: Int,
    val sex: String,
    val status: String,
    val assignedWard: String? = null
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
}

class HospitalState {
    private val patients = mutableListOf(
        Patient("PT-001", "Amina Yusuf", 34, "F", PatientStatus.WAITING, triageLevel = 2),
        Patient("PT-002", "John Ouma", 58, "M", PatientStatus.IN_DIAGNOSIS, clinician = "Dr. Otieno", diagnosis = "Hypertension"),
        Patient("PT-003", "Martha Wekesa", 12, "F", PatientStatus.ADMITTED, "Pediatrics", triageLevel = 2, clinician = "Dr. Naliaka"),
        Patient("PT-004", "Daniel Mwangi", 41, "M", PatientStatus.IN_CONSULTATION, clinician = "Dr. Achieng")
    )

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

    fun metrics(): List<DashboardMetric> = listOf(
        DashboardMetric("Registered Today", patients.size.toString()),
        DashboardMetric("In Wards", patients.count { it.status == PatientStatus.ADMITTED }.toString()),
        DashboardMetric("Pending Consultation", patients.count { it.status == PatientStatus.WAITING }.toString()),
        DashboardMetric("Active Clinicians", patients.mapNotNull { it.clinician }.distinct().size.toString())
    )
}
