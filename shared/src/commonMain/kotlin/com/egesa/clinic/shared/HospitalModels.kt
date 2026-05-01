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

class HospitalState {
    private val patients = mutableListOf(
        Patient("PT-001", "Amina Yusuf", 34, "F", "Awaiting consultation"),
        Patient("PT-002", "John Ouma", 58, "M", "In diagnosis"),
        Patient("PT-003", "Martha Wekesa", 12, "F", "Admitted", "Pediatrics")
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
}
