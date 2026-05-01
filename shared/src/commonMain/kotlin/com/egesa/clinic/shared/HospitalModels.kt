package com.egesa.clinic.shared

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

class HospitalState {
    private val patients = mutableListOf(
        Patient("PT-001", "Amina Yusuf", 34, "F", "Awaiting consultation"),
        Patient("PT-002", "John Ouma", 58, "M", "In diagnosis"),
        Patient("PT-003", "Martha Wekesa", 12, "F", "Admitted", "Pediatrics")
    )

    fun allPatients(): List<Patient> = patients.toList()

    fun metrics(): List<DashboardMetric> = listOf(
        DashboardMetric("Registered Today", patients.size.toString()),
        DashboardMetric("In Wards", patients.count { it.assignedWard != null }.toString()),
        DashboardMetric("Pending Consultation", patients.count { it.status.contains("consultation", true) }.toString())
    )
}
