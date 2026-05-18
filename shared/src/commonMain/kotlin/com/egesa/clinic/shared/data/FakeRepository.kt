package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

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

    // ── Cloud Synchronization ──────────────────────────────────────────────────

    /**
     * Fetch patients from server with delta sync support
     * Returns: patients data and current remote version
     */
    suspend fun syncPatientData(remoteVersion: Long = 0): Result<SyncPatientDataResponse> {
        return try {
            delay(500) // Simulate network latency
            // TODO: GET /sync/patients?version={remoteVersion}
            val patients = db.allPatients()
            Result.success(SyncPatientDataResponse(
                patients = patients,
                remoteVersion = Clock.System.now().toEpochMilliseconds(),
                count = patients.size
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload batch of patient changes to server
     * Used for syncing local modifications back to cloud
     */
    suspend fun uploadPatientChanges(changes: List<Patient>): Result<List<SyncResultItem>> {
        return try {
            delay(400) // Simulate network latency
            // TODO: POST /sync/patients/batch
            val results = changes.map { patient ->
                SyncResultItem(
                    id = patient.id,
                    status = "synced",
                    version = 1
                )
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolve a conflict between local and remote versions
     * Supported strategies: CLIENT_WINS, SERVER_WINS, MERGE
     */
    suspend fun resolveConflict(
        entityId: String,
        localVersion: Int,
        remoteVersion: Int,
        strategy: String = "SERVER_WINS"
    ): Result<ConflictResolutionResult> {
        return try {
            delay(200) // Simulate network latency
            // TODO: POST /sync/resolve-conflict
            Result.success(ConflictResolutionResult(
                resolved = true,
                strategy = strategy,
                finalVersion = maxOf(localVersion, remoteVersion)
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get sync health status from server
     */
    suspend fun getSyncHealth(): Result<Map<String, String>> {
        return try {
            delay(200)
            // TODO: GET /payments/sync-health
            Result.success(mapOf(
                "status" to "healthy",
                "pendingReconciliation" to "0",
                "lastSync" to Clock.System.now().toEpochMilliseconds().toString()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Staff data ─────────────────────────────────────────────────────────────────

val STAFF_MEMBERS = emptyList<StaffMember>()
