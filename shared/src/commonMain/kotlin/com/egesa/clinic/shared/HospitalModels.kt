package com.egesa.clinic.shared

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

    fun metrics(): List<DashboardMetric> = listOf(
        DashboardMetric("Registered Today", patients.size.toString()),
        DashboardMetric("In Wards", patients.count { it.assignedWard != null }.toString()),
        DashboardMetric("Pending Consultation", patients.count { it.status.contains("consultation", true) }.toString())
    )

    fun globalNavItemsFor(role: UserRole): List<GlobalNavItem> = navItems.filter { role in it.visibleTo }

    fun globalActions(): List<GlobalAction> = globalActions.toList()

    fun breadcrumbFor(area: WorkflowArea): List<String> = listOf("Home", area.name.lowercase().replaceFirstChar { it.uppercase() }, "Workflow")
}
