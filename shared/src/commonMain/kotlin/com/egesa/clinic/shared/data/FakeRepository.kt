package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.*
import kotlinx.coroutines.delay

/**
 * Simulates async API calls. Each function adds a short delay to mimic network latency.
 * Replace the body of each function with a real Ktor HTTP call once the FastAPI backend is ready.
 */
object FakeRepository {

    private val db = HospitalState()

    // ── Staff ──────────────────────────────────────────────────────────────────

    suspend fun getStaff(): List<StaffMember> {
        delay(300)
        return STAFF_MEMBERS
    }

    suspend fun validatePin(staffId: String, pin: String): Boolean {
        delay(500) // simulate auth round-trip
        // TODO: POST /auth/login  { staff_id, pin_hash }
        return pin.length >= 4  // placeholder — accept any PIN >= 4 digits
    }

    // ── Patients ──────────────────────────────────────────────────────────────

    suspend fun getPatients(): List<Patient> {
        delay(350)
        // TODO: GET /patients
        return db.allPatients()
    }

    suspend fun getPatient(id: String): Patient? {
        delay(200)
        // TODO: GET /patients/{id}
        return db.allPatients().find { it.id == id }
    }

    // ── Reception ─────────────────────────────────────────────────────────────

    suspend fun getQueue(): List<QueueItem> {
        delay(250)
        // TODO: GET /queue
        return db.receptionQueue()
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    suspend fun getKpis(): List<DashboardMetric> {
        delay(300)
        // TODO: GET /dashboard/kpis
        return db.adminKpis()
    }

    suspend fun getBottlenecks(): List<BottleneckCell> {
        delay(250)
        // TODO: GET /dashboard/bottlenecks
        return db.bottleneckHeatmap()
    }

    suspend fun getRegistrationTrend(): List<TrendPoint> {
        delay(200)
        // TODO: GET /dashboard/trend
        return db.registrationTrend()
    }

    // ── Wards ─────────────────────────────────────────────────────────────────

    suspend fun getWardOverview(): WardOverview {
        delay(200)
        // TODO: GET /wards/overview
        return db.wardOverview()
    }

    suspend fun getBedBoard(): List<BedCard> {
        delay(300)
        // TODO: GET /wards/beds
        return db.bedBoard()
    }

    suspend fun getNursingTasks(): List<NursingTask> {
        delay(200)
        // TODO: GET /wards/tasks
        return db.nursingTasks()
    }

    suspend fun getWardCensus(): List<WardCensusRow> {
        delay(200)
        // TODO: GET /wards/census
        return db.wardCensus()
    }

    suspend fun getAtdState(): AdmissionTransferDischargeState {
        delay(150)
        // TODO: GET /wards/atd
        return db.atdState()
    }

    suspend fun getShiftHandoff(shift: Shift): List<String> {
        delay(150)
        // TODO: GET /wards/handoff?shift={shift}
        return db.shiftHandoffSummary(shift)
    }
}

// ── Staff data ─────────────────────────────────────────────────────────────────

data class StaffMember(
    val id: String,
    val fullName: String,
    val role: UserRole,
    val department: String,
)

val STAFF_MEMBERS = listOf(
    StaffMember("DR-001", "Dr. James Kamau",          UserRole.DOCTOR,       "Outpatient"),
    StaffMember("DR-002", "Dr. Sarah Ochieng",        UserRole.DOCTOR,       "Emergency"),
    StaffMember("DR-003", "Dr. Michael Mwangi",       UserRole.DOCTOR,       "Surgery"),
    StaffMember("DR-004", "Dr. Amina Yusuf",          UserRole.DOCTOR,       "Pediatrics"),
    StaffMember("DR-005", "Dr. Samuel Njoroge",       UserRole.DOCTOR,       "Internal Medicine"),
    StaffMember("NR-001", "Nurse Faith Wanjiku",      UserRole.NURSE,        "Medical Ward"),
    StaffMember("NR-002", "Nurse Peter Otieno",       UserRole.NURSE,        "Surgical Ward"),
    StaffMember("NR-003", "Nurse Grace Mwangi",       UserRole.NURSE,        "Pediatrics"),
    StaffMember("NR-004", "Nurse Daniel Kibet",       UserRole.NURSE,        "ICU"),
    StaffMember("NR-005", "Nurse Naomi Atieno",       UserRole.NURSE,        "Emergency"),
    StaffMember("RC-001", "Mary Otieno",              UserRole.RECEPTIONIST, "Front Desk"),
    StaffMember("RC-002", "John Ouma",                UserRole.RECEPTIONIST, "Front Desk"),
    StaffMember("RC-003", "Esther Wekesa",            UserRole.RECEPTIONIST, "Outpatient"),
    StaffMember("AD-001", "Admin User",               UserRole.ADMIN,        "Administration"),
    StaffMember("AD-002", "Jane Njeri",               UserRole.ADMIN,        "Administration"),
)
