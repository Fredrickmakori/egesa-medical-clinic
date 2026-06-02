package com.egesa.clinic.shared.ui.navigation

import com.egesa.clinic.shared.Permission
import com.egesa.clinic.shared.RolePermissionMap
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.WorkflowArea
import com.egesa.clinic.shared.sync.SyncStatus

data class ClinicNavItem(
    val area: WorkflowArea,
    val label: String,
    val shortLabel: String = label.take(4),
    val group: String = "Operations",
    val visibleTo: Set<UserRole> = UserRole.entries.toSet(),
)

val ALL_NAV_ITEMS = listOf(
    ClinicNavItem(WorkflowArea.DASHBOARD,    "Command Center", "Dash", "Overview",
        setOf(UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.RECEPTION,    "Reception",    "Rec", "Front Office",
        setOf(UserRole.RECEPTIONIST, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.APPOINTMENTS, "Appointments", "Appt", "Front Office",
        setOf(UserRole.RECEPTIONIST, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.CONSULTATION, "Consultation", "Cons", "Clinical",
        setOf(UserRole.DOCTOR, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.DIAGNOSIS,    "Diagnosis",    "Diag", "Clinical",
        setOf(UserRole.DOCTOR, UserRole.NURSE, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.LAB_IMAGING,  "Lab / Imaging","Lab", "Clinical",
        setOf(UserRole.DOCTOR, UserRole.NURSE, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.PHARMACY,     "Pharmacy",     "Rx", "Clinical",
        setOf(UserRole.PHARMACIST, UserRole.DOCTOR, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.WARDS,        "Wards",        "Ward", "Clinical",
        setOf(UserRole.NURSE, UserRole.DOCTOR, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.BILLING,      "Billing / M-Pesa", "Bill", "Revenue",
        setOf(UserRole.RECEPTIONIST, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.INVENTORY,    "Inventory",    "Stock", "Revenue",
        setOf(UserRole.PHARMACIST, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.REPORTS,      "Reports",      "Rep", "Intelligence",
        setOf(UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.MOH_REPORTS,  "MOH Reports",  "MOH", "Intelligence",
        setOf(UserRole.ADMIN, UserRole.DOCTOR, UserRole.NURSE)),
    ClinicNavItem(WorkflowArea.NOTIFICATIONS,"SMS / Alerts", "SMS", "Engagement",
        setOf(UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.ADMIN,        "Admin",        "Adm", "System",
        setOf(UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.SETTINGS,     "Settings",     "Set", "System",
        setOf(UserRole.ADMIN)),
)

fun navItemsFor(role: UserRole): List<ClinicNavItem> =
    ALL_NAV_ITEMS.filter { role in it.visibleTo }

/** Post-login home module per role (admin = Command Center). */
fun defaultLandingArea(role: UserRole): WorkflowArea = when (role) {
    UserRole.ADMIN -> WorkflowArea.DASHBOARD
    UserRole.RECEPTIONIST -> WorkflowArea.RECEPTION
    UserRole.DOCTOR -> WorkflowArea.CONSULTATION
    UserRole.NURSE -> WorkflowArea.WARDS
    UserRole.PHARMACIST -> WorkflowArea.PHARMACY
}

/** Landing area that never points at a module the role cannot open. */
fun safeDefaultLandingArea(role: UserRole): WorkflowArea {
    val preferred = defaultLandingArea(role)
    if (roleCanAccessArea(role, preferred)) return preferred
    return navItemsFor(role).firstOrNull()?.area ?: preferred
}

fun roleCanAccessArea(role: UserRole, area: WorkflowArea): Boolean =
    navItemsFor(role).any { it.area == area }

fun areaWelcomeMessage(role: UserRole, area: WorkflowArea, firstName: String): String {
    val moduleHint = when (area) {
        WorkflowArea.DASHBOARD -> "Track flow, revenue, wards, and alerts from the command center."
        WorkflowArea.RECEPTION -> "Register patients, manage the queue, and forward to clinical areas."
        WorkflowArea.APPOINTMENTS -> "Schedule visits and manage today's appointment book."
        WorkflowArea.CONSULTATION -> "Review encounters, document care, and complete consultations."
        WorkflowArea.DIAGNOSIS -> "Capture assessments and working diagnoses for active patients."
        WorkflowArea.LAB_IMAGING -> "Order tests, track specimens, and review results."
        WorkflowArea.PHARMACY -> "Verify prescriptions and dispense medications safely."
        WorkflowArea.WARDS -> "Monitor admissions, bedside care, and ward transfers."
        WorkflowArea.BILLING -> "Collect payments, send M-Pesa STK, and reconcile invoices."
        WorkflowArea.INVENTORY -> "Track stock levels, reorders, and pharmacy inventory."
        WorkflowArea.REPORTS -> "Review operational and financial clinic reports."
        WorkflowArea.MOH_REPORTS -> "Prepare Ministry of Health returns and indicators."
        WorkflowArea.NOTIFICATIONS -> "Send SMS reminders and manage patient alerts."
        WorkflowArea.ADMIN -> "Configure staff, facilities, and system policies."
        WorkflowArea.SETTINGS -> "Adjust clinic preferences and integrations."
    }
    val roleLabel = role.name.lowercase().replaceFirstChar { it.uppercase() }
    return "Welcome, $firstName — $roleLabel workspace. $moduleHint"
}

data class SessionState(
    val staffId: String,
    val fullName: String,
    val role: UserRole,
    val shiftLabel: String = "Day shift",
    val permissions: Set<Permission> = RolePermissionMap.permissionsFor(role),
    val token: String? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncTime: Long? = null
) {
    val initials: String
        get() = fullName.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
    
    fun hasPermission(permission: Permission): Boolean = permission in permissions
    
    fun hasAllPermissions(vararg permissions: Permission): Boolean = 
        permissions.all { it in this.permissions }
    
    fun hasAnyPermission(vararg permissions: Permission): Boolean = 
        permissions.any { it in this.permissions }
}
