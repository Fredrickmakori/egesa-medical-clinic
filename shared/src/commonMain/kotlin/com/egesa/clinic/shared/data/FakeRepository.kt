package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.*
import com.egesa.clinic.shared.domain.LabOrder
import com.egesa.clinic.shared.domain.LabOrderStatus
import com.egesa.clinic.shared.domain.LabResult
import com.egesa.clinic.shared.sync.SyncHealthStatus
import com.egesa.clinic.shared.domain.OpdEncounterBundle
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

object FakeRepository {

    private val db = HospitalState()
    private var clinicApi: ClinicApi? = null

    /**
     * Enable local mock fallback for development and offline testing.
     */
    var useMockFallback: Boolean = true

    private var clinicApi: ClinicApi? = null

    fun clearAccessToken() = ClinicAuth.clearAccessToken()

    fun restoreAccessToken(token: String?) = ClinicAuth.setAccessToken(token)

    /**
     * Source of truth for non-production builds is a seeded fixture so login works offline/demo-first.
     * When backend integration is ready, replace with GET /staff and keep this as fallback data.
     */
    suspend fun getStaff(): List<StaffMember> = apiOrFallback({
        it.getStaff().map { staff -> staff.toDomain() }
    }) {
        delay(300)
        STAFF_MEMBERS
    }

    suspend fun getMockStaff(): List<StaffMember> = getStaff()

    fun installClinicApi(api: ClinicApi) {
        clinicApi = api
    }

    // ── Staff ──────────────────────────────────────────────────────────────────

    suspend fun login(staffId: String, pin: String): Result<LoginResponseDto> = runResultOrFallback(
        apiCall = {
            val response = it.login(staffId, pin)
            ClinicAuth.setAccessToken(response.accessToken)
            response
        },
        fallback = {
            if (pin.length < 4) throw IllegalArgumentException("Invalid PIN")
            // Demo-only token. In production, useMockFallback must be false.
            val response = LoginResponseDto(
                accessToken = "demo-token",
                expiresAtEpochSeconds = (Clock.System.now().epochSeconds + 8 * 60 * 60),
                role = UserRole.ADMIN.name,
                staffName = "Demo User"
            )
            ClinicAuth.setAccessToken(response.accessToken)
            response
        }
    )

    suspend fun validatePin(staffId: String, pin: String): Boolean {
        return login(staffId, pin).isSuccess
    }

    suspend fun getPatients(): List<Patient> = apiOrFallback({
        it.getPatients().map { it.toDomain() }
    }) { db.allPatients() }

    suspend fun getPatient(id: String): Patient? = apiOrFallback({
        it.getPatient(id)?.toDomain()
    }) { db.allPatients().find { it.id == id } }

    suspend fun getQueue(): List<QueueItem> = apiOrFallback({
        it.getQueue().map { it.toDomain() }
    }) { db.receptionQueue() }

    suspend fun registerPatient(input: PatientRegistrationInput, triageLevel: Int): Result<PatientRegistrationResponseDto> = runResultOrFallback({
        it.registerPatient(input.toRegistrationDto(triageLevel))
    }) {
        val patient = db.registerPatient(
            Patient(
                id = input.id,
                fullName = input.fullName,
                age = input.age,
                sex = input.sex,
                status = input.status,
                assignedWard = input.assignedWard,
                roomBed = input.roomBed,
                acuity = input.acuity,
                isolation = input.isolation
            )
        )
        val queueItem = db.checkInPatient(patient.id, patient.fullName, triageLevel)
        PatientRegistrationResponseDto(patient.toDto(), queueItem.toDto())
    }

    suspend fun checkInPatient(patient: Patient, triageLevel: Int): Result<QueueItem> = runResultOrFallback({
        it.checkInPatient(QueueCheckInRequestDto(patient.id, patient.fullName, triageLevel)).toDomain()
    }) {
        db.registerPatient(patient)
        db.checkInPatient(patient.id, patient.fullName, triageLevel)
    }

    suspend fun checkOutPatient(patientId: String): Result<QueueItem> = runResultOrFallback({
        it.checkOutPatient(patientId).toDomain()
    }) {
        db.checkOutPatient(patientId) ?: throw IllegalArgumentException("Patient is not in the active queue")
    }

    suspend fun getKpis(): List<DashboardMetric> = apiOrFallback({
        it.getKpis().map { it.toDomain() }
    }) { db.adminKpis() }

    suspend fun getBottlenecks(): List<BottleneckCell> = apiOrFallback({
        it.getBottlenecks().map { it.toDomain() }
    }) { db.bottleneckHeatmap() }

    suspend fun getRegistrationTrend(): List<TrendPoint> = apiOrFallback({
        it.getRegistrationTrend().map { it.toDomain() }
    }) { db.registrationTrend() }

    suspend fun getWardOverview(): WardOverview = apiOrFallback({
        it.getWardOverview().toDomain()
    }) { db.wardOverview() }

    suspend fun getBedBoard(): List<BedCard> = apiOrFallback({
        it.getBedBoard().map { it.toDomain() }
    }) { db.bedBoard() }

    suspend fun getNursingTasks(): List<NursingTask> = apiOrFallback({
        it.getNursingTasks().map { it.toDomain() }
    }) { db.nursingTasks() }

    suspend fun getWardCensus(): List<WardCensusRow> = apiOrFallback({
        it.getWardCensus().map { it.toDomain() }
    }) { db.wardCensus() }

    suspend fun getAtdState(): AdmissionTransferDischargeState = apiOrFallback({
        it.getAtdState().toDomain()
    }) { db.atdState() }

    suspend fun getShiftHandoff(shift: Shift): List<String> = apiOrFallback({
        it.getShiftHandoff(shift)
    }) { db.shiftHandoffSummary(shift) }

    suspend fun syncPatientData(remoteVersion: Long = 0): Result<SyncPatientDataResponse> = runResultOrFallback({
        it.syncPatientData(remoteVersion).toDomain()
    }) {
        val patients = db.allPatients()
        SyncPatientDataResponse(patients = patients, remoteVersion = remoteVersion, count = patients.size)
    }

    suspend fun uploadPatientChanges(changes: List<Patient>): Result<List<SyncResultItem>> = runResultOrFallback({
        it.uploadPatientChanges(changes.map { it.toDto() }).map { it.toDomain() }
    }) {
        changes.map { SyncResultItem(id = it.id, status = "synced", version = 1) }
    }

    suspend fun uploadClinicalChanges(batch: ClinicalSyncBatchDto): Result<List<SyncResultItem>> = runResultOrFallback({
        it.uploadClinicalChanges(batch).map { it.toDomain() }
    }) {
        fallbackClinicalResults(batch).map { it.toDomain() }
    }

    suspend fun resolveConflict(
        entityId: String,
        localVersion: Int,
        remoteVersion: Int,
        strategy: String = "SERVER_WINS"
    ): Result<ConflictResolutionResult> = runResultOrFallback({
        it.resolveConflict(ConflictResolutionRequestDto(entityId, localVersion, remoteVersion, strategy)).toDomain()
    }) {
        ConflictResolutionResult(resolved = true, strategy = strategy, finalVersion = maxOf(localVersion, remoteVersion))
    }

    suspend fun createLabOrder(order: LabOrder): Result<LabOrder> = runResultOrFallback({
        it.createLabOrder(order.toDto()).toDomain()
    }) { order }

    suspend fun getLabOrder(orderId: String): Result<LabOrder?> = runResultOrFallback({
        it.getLabOrder(orderId)?.toDomain()
    }) { null }

    suspend fun getPatientLabOrders(patientId: String): Result<List<LabOrder>> = runResultOrFallback({
        it.getLabOrdersForPatient(patientId).map { dto -> dto.toDomain() }
    }) { emptyList() }

    suspend fun updateLabOrderStatus(orderId: String, status: LabOrderStatus, actorId: String): Result<LabOrder> = runResultOrFallback({
        it.updateLabOrderStatus(orderId, UpdateLabOrderStatusRequestDto(status.name, actorId)).toDomain()
    }) { throw IllegalStateException("No remote lab order store in fallback mode") }

    suspend fun saveLabResults(orderId: String, actorId: String, results: List<LabResult>): Result<List<LabResult>> = runResultOrFallback({
        it.saveLabResults(SaveLabResultsRequestDto(orderId = orderId, actorId = actorId, results = results.map { r -> r.toDto() }))
            .map { dto -> dto.toDomain() }
    }) { results }

    private suspend inline fun <T> runResultOrFallback(
        apiCall: suspend (ClinicApi) -> T,
        fallback: suspend () -> T
    ): Result<T> {
        val api = clinicApi
        return try {
            if (api == null) {
                Result.success(fallback())
            } else {
                Result.success(apiCall(api))
            }
        } catch (e: Exception) {
            if (useMockFallback) {
                runCatching { fallback() }
            } else {
                Result.failure(e.toRepositoryException())
            }
        }
    }

    private suspend inline fun <T> apiOrFallback(
        apiCall: suspend (ClinicApi) -> T,
        fallback: suspend () -> T
    ): T {
        val api = clinicApi
        return if (api == null) {
            fallback()
        } else {
            try {
                apiCall(api)
            } catch (e: Exception) {
                if (useMockFallback) fallback() else throw e
            }
        }
    }

    suspend fun getSyncHealth(): Result<SyncHealthStatus> = runResultOrFallback(
        apiCall = { it.getSyncHealth() },
        fallback = {
            SyncHealthStatus(
                status = if (ClinicAuth.hasToken()) "local-only" else "offline",
                pendingCount = 0,
                lastSyncTime = Clock.System.now().toEpochMilliseconds()
            )
        }
    )

    suspend fun createEncounter(bundle: OpdEncounterBundle): Result<OpdEncounterBundle> = runResultOrFallback(
        apiCall = { it.createEncounter(bundle) },
        fallback = { bundle }
    )

    suspend fun getEncounter(encounterId: String): Result<OpdEncounterBundle> = runResultOrFallback(
        apiCall = { it.getEncounter(encounterId) },
        fallback = { throw IllegalArgumentException("Encounter $encounterId not found in mock mode") }
    )

    suspend fun getPatientEncounters(patientId: String): Result<List<OpdEncounterBundle>> = runResultOrFallback(
        apiCall = { it.getPatientEncounters(patientId) },
        fallback = { emptyList() }
    )

    @Suppress("UNUSED_PARAMETER")
    private suspend fun Throwable.toRepositoryException(): Throwable = this
}

private fun fallbackClinicalResults(batch: ClinicalSyncBatchDto): List<SyncResultItemDto> = buildList {
    batch.encounters.forEach { add(SyncResultItemDto(it.encounterId, "queued-local", 0)) }
    batch.vitalSigns.forEach { add(SyncResultItemDto(it.vitalSignsId, "queued-local", 0)) }
    batch.diagnoses.forEach { add(SyncResultItemDto(it.diagnosisId, "queued-local", 0)) }
    batch.medicationOrders.forEach { add(SyncResultItemDto(it.medicationOrderId, "queued-local", 0)) }
    batch.encounterOutcomes.forEach { add(SyncResultItemDto(it.outcomeId, "queued-local", 0)) }
    batch.htsEntries.forEach { add(SyncResultItemDto(it.htsId, "queued-local", 0)) }
    batch.serviceEvents.forEach { add(SyncResultItemDto(it.serviceEventId, "queued-local", 0)) }
    batch.patientDocuments.forEach { add(SyncResultItemDto(it.documentId, "queued-local", 0)) }
}

class RepositoryHttpException(statusCode: Int, responseBody: String) : Exception(
    "HTTP $statusCode: ${responseBody.ifBlank { "No response body" }}"
)

data class SyncPatientDataResponse(
    val patients: List<Patient>,
    val remoteVersion: Long,
    val count: Int
)

data class SyncResultItem(
    val id: String,
    val status: String,
    val version: Int
)

data class ConflictResolutionResult(
    val resolved: Boolean,
    val strategy: String,
    val finalVersion: Int
)

val STAFF_MEMBERS = listOf(
    StaffMember("DR-001", "Dr. James Kamau", UserRole.DOCTOR, "General Medicine"),
    StaffMember("NR-001", "Nurse Faith Wanjiku", UserRole.NURSE, "Emergency Department"),
    StaffMember("PH-001", "Pharmacist Brian Otieno", UserRole.PHARMACIST, "Pharmacy"),
    StaffMember("RC-001", "Mary Otieno", UserRole.RECEPTIONIST, "Front Desk"),
    StaffMember("AD-001", "Admin User", UserRole.ADMIN, "Administration")
)
