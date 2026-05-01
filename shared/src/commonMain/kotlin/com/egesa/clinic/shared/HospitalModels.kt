package com.egesa.clinic.shared

import kotlinx.serialization.Serializable

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

enum class Shift {
    DAY,
    NIGHT
}

data class Patient(
    val id: String,
    val fullName: String,
    val age: Int,
    val sex: String,
    val status: String,
    val assignedWard: String? = null,
    val roomBed: String? = null,
    val acuity: String = "Moderate",
    val isolation: String? = null
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
    private val patients = mutableListOf(
        Patient("PT-001", "Amina Yusuf", 34, "F", "Awaiting consultation"),
        Patient("PT-002", "John Ouma", 58, "M", "In diagnosis"),
        Patient("PT-003", "Martha Wekesa", 12, "F", "Admitted", "Pediatrics", "P-12A", "High", "Contact"),
        Patient("PT-004", "Samuel Kibet", 70, "M", "Admitted", "Medical", "M-04B", "Critical", "Droplet"),
        Patient("PT-005", "Naomi Atieno", 27, "F", "Admitted", "Surgical", "S-08A", "Moderate", null)
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

    fun wardOverview(): WardOverview = WardOverview(
        occupancyPercent = 82,
        bedsAvailable = 14,
        nurseWorkload = "1:6 avg ratio",
        alerts = listOf("2 sepsis screens overdue", "1 fall-risk reassessment due", "Isolation PPE stock low")
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

    fun nursingTasks(): List<NursingTask> = listOf(
        NursingTask("Meds Due", "PT-004 Piperacillin/Tazobactam IV", "09:00", "High"),
        NursingTask("Vitals", "PT-003 q4h vitals & pain score", "10:00", "Medium"),
        NursingTask("Procedure", "PT-005 dressing change", "11:30", "High"),
        NursingTask("Handover", "Flag pending lab for PT-002", "Shift end", "Medium")
    )

    fun wardCensus(): List<WardCensusRow> = listOf(
        WardCensusRow("Medical", 26, 30, 6, 2),
        WardCensusRow("Surgical", 22, 28, 4, 1),
        WardCensusRow("Pediatrics", 18, 24, 3, 2)
    )

    fun shiftHandoffSummary(shift: Shift): List<String> = when (shift) {
        Shift.DAY -> listOf(
            "Admissions: 5 | Transfers: 2 | Discharges: 3",
            "Critical watchlist: PT-004, PT-011",
            "Pending diagnostics: 4 CBC, 2 blood cultures"
        )

        Shift.NIGHT -> listOf(
            "Overnight events: 1 rapid response, stabilized",
            "High-risk meds double-check completed",
            "Morning rounds prep complete for all wards"
        )
    }
}
