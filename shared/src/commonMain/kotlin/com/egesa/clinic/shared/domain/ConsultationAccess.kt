package com.egesa.clinic.shared.domain

import com.egesa.clinic.shared.Permission
import com.egesa.clinic.shared.RolePermissionMap
import com.egesa.clinic.shared.UserRole

/** RBAC helpers for the OPD consultation workspace. */
object ConsultationAccess {
    fun canViewEmr(role: UserRole): Boolean =
        role != UserRole.RECEPTIONIST || RolePermissionMap.hasPermission(role, Permission.PATIENT_READ)

    fun canEditHistory(role: UserRole): Boolean =
        RolePermissionMap.hasPermission(role, Permission.CONSULTATION_WRITE) &&
            RolePermissionMap.hasPermission(role, Permission.DIAGNOSIS_WRITE)

    fun canEditVitalsAndExam(role: UserRole): Boolean =
        RolePermissionMap.hasPermission(role, Permission.CONSULTATION_WRITE)

    fun canEditDiagnosisAndPlan(role: UserRole): Boolean =
        RolePermissionMap.hasPermission(role, Permission.DIAGNOSIS_WRITE) &&
            RolePermissionMap.hasPermission(role, Permission.PRESCRIPTION_WRITE)

    fun canEditNursingNotes(role: UserRole): Boolean =
        role == UserRole.NURSE || role == UserRole.DOCTOR || role == UserRole.ADMIN

    /** Receptionist: visit header only (no SOAP, vitals detail, diagnosis, or plan). */
    fun isReceptionistSummaryOnly(role: UserRole): Boolean = role == UserRole.RECEPTIONIST

    fun canReadPharmacySlice(role: UserRole): Boolean =
        role == UserRole.PHARMACIST || role == UserRole.DOCTOR || role == UserRole.ADMIN

    fun canReadLabSlice(role: UserRole): Boolean =
        RolePermissionMap.hasPermission(role, Permission.LAB_RESULT_MANAGE) ||
            role == UserRole.PHARMACIST
}
