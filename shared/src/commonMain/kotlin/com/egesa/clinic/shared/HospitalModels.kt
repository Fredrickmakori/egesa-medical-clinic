package com.egesa.clinic.shared

import kotlinx.serialization.Serializable

@Serializable
enum class WorkflowArea {
    RECEPTION,
    CONSULTATION,
    DIAGNOSIS,
    WARD,
    ADMIN
}

@Serializable
enum class PatientStatus {
    WAITING,
    IN_CONSULTATION,
    IN_DIAGNOSIS,
    ADMITTED,
    DISCHARGED
}

@Serializable
data class Patient(
    val id: String,
    val fullName: String,
    val age: Int,
    val sex: String,
    val status: PatientStatus,
    val assignedWard: String? = null,
    val triageLevel: Int = 3,
    val clinician: String? = null,
    val diagnosis: String? = null
)

@Serializable
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

    fun metrics(): List<DashboardMetric> = listOf(
        DashboardMetric("Registered Today", patients.size.toString()),
        DashboardMetric("In Wards", patients.count { it.status == PatientStatus.ADMITTED }.toString()),
        DashboardMetric("Pending Consultation", patients.count { it.status == PatientStatus.WAITING }.toString()),
        DashboardMetric("Active Clinicians", patients.mapNotNull { it.clinician }.distinct().size.toString())
    )
}
