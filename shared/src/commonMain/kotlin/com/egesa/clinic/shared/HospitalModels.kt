package com.egesa.clinic.shared

import com.egesa.clinic.shared.sync.SyncHealthStatus

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

interface CodeEnum { val code: String }

object DomainValidation {
    fun requireCode(field: String, value: String, allowed: Set<String>): String {
        val normalized = value.trim().lowercase().replace("_", "-").replace(" ", "-")
        require(normalized in allowed) { "Invalid $field: '$value'" }
        return normalized
    }
}

@JvmInline
@Serializable
value class LegacyMappableCode private constructor(val value: String) {
    companion object {
        fun from(field: String, raw: String, aliases: Map<String, String>, allowed: Set<String>): LegacyMappableCode {
            val normalized = raw.trim().lowercase().replace("_", "-").replace(" ", "-")
            val mapped = aliases[normalized] ?: normalized
            return LegacyMappableCode(DomainValidation.requireCode(field, mapped, allowed))
        }
    }
}

enum class Sex(override val code: String) : CodeEnum { MALE("male"), FEMALE("female"), INTERSEX("intersex"), UNKNOWN("unknown") }
enum class VisitType(override val code: String) : CodeEnum { OUTPATIENT("outpatient"), INPATIENT("inpatient"), EMERGENCY("emergency"), FOLLOW_UP("follow-up"), ANC("anc") }
enum class Disposition(override val code: String) : CodeEnum { ADMITTED("admitted"), DISCHARGED("discharged"), TRANSFERRED("transferred"), REFERRED("referred"), DECEASED("deceased") }
enum class FetalPresentation(override val code: String) : CodeEnum { CEPHALIC("cephalic"), BREECH("breech"), TRANSVERSE("transverse"), OBLIQUE("oblique"), UNKNOWN("unknown") }
enum class DeliveryMode(override val code: String) : CodeEnum { SVD("svd"), ASSISTED_VAGINAL("assisted-vaginal"), CESAREAN("cesarean"), VBAC("vbac") }
enum class DeliveryOutcome(override val code: String) : CodeEnum { LIVE_BIRTH("live-birth"), STILL_BIRTH("still-birth"), NEONATAL_DEATH("neonatal-death") }
enum class WhoStage(override val code: String) : CodeEnum { STAGE_1("stage-1"), STAGE_2("stage-2"), STAGE_3("stage-3"), STAGE_4("stage-4") }
enum class HivStatus(override val code: String) : CodeEnum { POSITIVE("positive"), NEGATIVE("negative"), UNKNOWN("unknown"), EXPOSED("exposed") }
enum class AdherenceRating(override val code: String) : CodeEnum { GOOD("good"), FAIR("fair"), POOR("poor") }
enum class CohortStatus(override val code: String) : CodeEnum { ACTIVE("active"), LOST_TO_FOLLOW_UP("lost-to-follow-up"), TRANSFERRED_OUT("transferred-out"), DECEASED("deceased"), STOPPED("stopped") }
enum class MuacStatus(override val code: String) : CodeEnum { GREEN("green"), YELLOW("yellow"), RED("red") }
enum class Complication(override val code: String) : CodeEnum { NONE("none"), FEVER("fever"), HEMORRHAGE("hemorrhage"), SEPSIS("sepsis"), ECLAMPSIA("eclampsia"), OTHER("other") }

object LegacyCodeMapper {
    private fun <T : Enum<T>> codeSet(values: Array<T>): Set<String> where T : CodeEnum = values.map { it.code }.toSet()
    fun sex(raw: String): Sex = Sex.entries.first { it.code == LegacyMappableCode.from("sex", raw, mapOf("m" to "male", "f" to "female"), codeSet(Sex.entries.toTypedArray())).value }
}

// ── Permission-based access control ────────────────────────────────────────
enum class Permission {
    // Patient management
    PATIENT_CREATE, PATIENT_READ, PATIENT_UPDATE, PATIENT_DELETE,

    // Clinical operations
    CONSULTATION_WRITE, DIAGNOSIS_WRITE, PRESCRIPTION_WRITE,

    // Ward operations
    WARD_ADMISSION, WARD_DISCHARGE, WARD_TRANSFER,

    // Billing/Payment
    PAYMENT_INITIATE, PAYMENT_APPROVE,

    // Admin functions
    STAFF_MANAGE, AUDIT_VIEW, SYSTEM_CONFIG
}

data class RolePermissionMap(
    val role: UserRole,
    val permissions: Set<Permission>
) {
    companion object {
        val DEFAULTS = mapOf(
            UserRole.RECEPTIONIST to setOf(
                Permission.PATIENT_CREATE, Permission.PATIENT_READ,
                Permission.PAYMENT_INITIATE
            ),
            UserRole.DOCTOR to setOf(
                Permission.PATIENT_READ, Permission.PATIENT_UPDATE,
                Permission.CONSULTATION_WRITE,
                Permission.DIAGNOSIS_WRITE, Permission.PRESCRIPTION_WRITE,
                Permission.WARD_ADMISSION, Permission.WARD_DISCHARGE,
                Permission.WARD_TRANSFER
            ),
            UserRole.NURSE to setOf(
                Permission.PATIENT_READ, Permission.PATIENT_UPDATE,
                Permission.CONSULTATION_WRITE,
                Permission.WARD_ADMISSION, Permission.WARD_TRANSFER
            ),
            UserRole.ADMIN to Permission.entries.toSet()
        )

        fun permissionsFor(role: UserRole): Set<Permission> =
            DEFAULTS[role] ?: emptySet()

        fun hasPermission(role: UserRole, permission: Permission): Boolean =
            permissionsFor(role).contains(permission)
    }
}

enum class WorkflowArea {
    DASHBOARD,
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

enum class Shift {
    DAY,
    NIGHT
}

enum class TimelineEventType {
    CONSULTATION, LAB, MEDICATION, PROCEDURE, DISCHARGE, NOTE
}

@Serializable
data class TimelineEvent(
    val title: String,
    val details: String,
    val type: TimelineEventType,
    val timestamp: String
)

enum class SaveState { DRAFT_SAVED, UNSAVED_CHANGES, FINAL_SIGN_OFF }

data class EncounterForm(
    val chiefComplaint: String = "",
    val history: String = "",
    val examination: String = "",
    val diagnosis: String = "",
    val plan: String = ""
)

enum class StkRequestStatus { PENDING, SUCCESS, FAILED }

data class NavItem(
    val area: WorkflowArea,
    val label: String,
    val visibilityAnnotation: String = ""
)

data class GlobalAction(val label: String)

@Serializable
data class Patient(
    val id: String,
    val fullName: String,
    val age: Int,
    val sex: Sex,
    val status: String,
    val assignedWard: String? = null,
    val roomBed: String? = null,
    val acuity: String = "Moderate",
    val isolation: String? = null,
    val visits: Int = 0,
    val activeDiagnosis: String = "",
    val currentMedications: List<String> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList()
)

@Serializable
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
    val contextReference: String,
    val userId: String = "",
    val permission: Permission? = null,
    val granted: Boolean = true
)

data class ConfigDictionary(
    val title: String,
    val entries: List<String>
)

data class StaffMember(
    val id: String,
    val fullName: String,
    val role: UserRole,
    val department: String,
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

// ── Sync-related data classes ──────────────────────────────────────────────

@Serializable
data class SyncPatientDataResponse(
    val patients: List<Patient>,
    val remoteVersion: Long,
    val count: Int
)

@Serializable
data class SyncResultItem(
    val id: String,
    val status: String,  // synced, failed, conflict
    val version: Int
)

@Serializable
data class ConflictResolutionResult(
    val resolved: Boolean,
    val strategy: String,
    val finalVersion: Int
)

data class WardOverview(
    val occupancyPercent: Int,
    val bedsAvailable: Int,
    val nurseWorkload: String,
    val alerts: List<String>
)

data class BedCard(
    val ward: String,
    val roomBed: String,
    val patientName: String,
    val status: String,
    val acuity: String,
    val isolation: String?
)

data class AdmissionTransferDischargeState(
    val selectedPatientId: String,
    val selectedBed: String,
    val transferWard: String,
    val dischargeChecklist: List<Pair<String, Boolean>>
)

data class NursingTask(
    val type: String,
    val detail: String,
    val due: String,
    val priority: String
)

data class WardCensusRow(
    val ward: String,
    val occupiedBeds: Int,
    val totalBeds: Int,
    val highAcuityCount: Int,
    val isolationCount: Int
)

class HospitalState {
    private val patients = mutableListOf<Patient>()

    fun allPatients(): List<Patient> = patients.toList()

    fun adminKpis(): List<DashboardMetric> = emptyList()

    fun registrationTrend(): List<TrendPoint> = emptyList()

    fun departmentComparison(): List<DepartmentMetric> = emptyList()

    fun bottleneckHeatmap(): List<BottleneckCell> = emptyList()

    fun wardOverview(): WardOverview = WardOverview(
        occupancyPercent = 0,
        bedsAvailable = 0,
        nurseWorkload = "N/A",
        alerts = emptyList()
    )

    fun bedBoard(): List<BedCard> = patients
        .filter { it.assignedWard != null && it.roomBed != null }
        .map {
            BedCard(
                ward = it.assignedWard!!,
                roomBed = it.roomBed!!,
                patientName = it.fullName,
                status = it.status,
                acuity = it.acuity,
                isolation = it.isolation
            )
        }

    fun atdState(): AdmissionTransferDischargeState = AdmissionTransferDischargeState(
        selectedPatientId = "PT-001",
        selectedBed = "M-10A",
        transferWard = "High Dependency Unit",
        dischargeChecklist = listOf(
            "Medication reconciliation complete" to true,
            "Follow-up appointment scheduled" to true,
            "Discharge education delivered" to false,
            "Transport confirmed" to false
        )
    )

    fun nursingTasks(): List<NursingTask> = emptyList()

    fun wardCensus(): List<WardCensusRow> = emptyList()

    fun shiftHandoffSummary(shift: Shift): List<String> = emptyList()

    fun globalNavItemsFor(role: UserRole): List<NavItem> {
        val allowed = when (role) {
            UserRole.RECEPTIONIST -> setOf(WorkflowArea.RECEPTION, WorkflowArea.REPORTS)
            UserRole.DOCTOR -> setOf(WorkflowArea.CONSULTATION, WorkflowArea.DIAGNOSIS, WorkflowArea.WARDS)
            UserRole.NURSE -> setOf(WorkflowArea.WARDS, WorkflowArea.CONSULTATION)
            UserRole.ADMIN -> WorkflowArea.entries.toSet()
        }
        return WorkflowArea.entries
            .filter { it in allowed }
            .map { area ->
                NavItem(
                    area = area,
                    label = area.name.lowercase().replaceFirstChar { it.uppercase() },
                    visibilityAnnotation = if (role == UserRole.ADMIN) "" else "Role: $role"
                )
            }
    }

    fun globalActions(): List<GlobalAction> = listOf(
        GlobalAction("Search"),
        GlobalAction("Register Patient"),
        GlobalAction("Alerts"),
        GlobalAction("Profile")
    )

    fun breadcrumbFor(area: WorkflowArea): List<String> =
        listOf("Home", area.name.lowercase().replaceFirstChar { it.uppercase() })

    fun metrics(): List<DashboardMetric> = adminKpis()

    fun configurationSets(): List<ConfigDictionary> = emptyList()

    fun receptionQueue(): List<QueueItem> = emptyList()

    fun wardBeds(): List<WardBed> = emptyList()

    private val pendingStk = mutableListOf<String>()

    fun reconcilePendingStkRequests(lookup: (String) -> StkRequestStatus) {
        pendingStk.removeAll { requestId -> lookup(requestId) != StkRequestStatus.PENDING }
    }

    fun syncHealth(): SyncHealthStatus = SyncHealthStatus(
        status = "healthy",
        pendingCount = pendingStk.size,
        lastSyncTime = System.currentTimeMillis()
    )

    fun pendingStkRequests(): List<String> = pendingStk.toList()

    fun auditTrail(): List<AuditEvent> = emptyList()
}
