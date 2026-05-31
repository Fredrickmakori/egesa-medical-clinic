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
