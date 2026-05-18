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
    val visibleTo: Set<UserRole> = UserRole.entries.toSet(),
)

val ALL_NAV_ITEMS = listOf(
    ClinicNavItem(WorkflowArea.RECEPTION,    "Reception",    "Rec",
        setOf(UserRole.RECEPTIONIST, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.CONSULTATION, "Consultation", "Cons",
        setOf(UserRole.DOCTOR, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.DIAGNOSIS,    "Diagnosis",    "Diag",
        setOf(UserRole.DOCTOR, UserRole.NURSE, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.WARDS,        "Wards",        "Ward",
        setOf(UserRole.NURSE, UserRole.DOCTOR, UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.ADMIN,        "Admin",        "Adm",
        setOf(UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.REPORTS,      "Reports",      "Rep",
        setOf(UserRole.ADMIN)),
    ClinicNavItem(WorkflowArea.SETTINGS,     "Settings",     "Set",
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

val SessionState.initials: String
    get() = fullName.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
