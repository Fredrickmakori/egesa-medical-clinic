package com.egesa.clinic.shared.ui.navigation

import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.WorkflowArea

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
)

val SessionState.initials: String
    get() = fullName.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
