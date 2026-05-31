package com.egesa.clinic.server

import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.Permission
import com.egesa.clinic.shared.StkRequestStatus
import com.egesa.clinic.shared.Shift
import com.egesa.clinic.shared.AuditEvent
import com.egesa.clinic.shared.BedCard
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.WardBed
import com.egesa.clinic.shared.WardCensusRow
import com.egesa.clinic.shared.WardOverview
import com.egesa.clinic.shared.data.AdmissionTransferDischargeStateDto
import com.egesa.clinic.shared.data.BedCardDto
import com.egesa.clinic.shared.data.BottleneckCellDto
import com.egesa.clinic.shared.data.ChecklistItemDto
import com.egesa.clinic.shared.data.ClinicalSyncBatchDto
import com.egesa.clinic.shared.data.ConflictResolutionRequestDto
import com.egesa.clinic.shared.data.ConflictResolutionResultDto
import com.egesa.clinic.shared.data.DashboardMetricDto
import com.egesa.clinic.shared.data.NursingTaskDto
import com.egesa.clinic.shared.data.PatientDto
import com.egesa.clinic.shared.data.PatientRegistrationDto
import com.egesa.clinic.shared.data.PatientRegistrationResponseDto
import com.egesa.clinic.shared.data.QueueCheckInRequestDto
import com.egesa.clinic.shared.data.QueueItemDto
import com.egesa.clinic.shared.data.StaffMemberDto
import com.egesa.clinic.shared.data.SyncPatientDataDto
import com.egesa.clinic.shared.data.SyncResultItemDto
import com.egesa.clinic.shared.data.TrendPointDto
import com.egesa.clinic.shared.data.WardCensusRowDto
import com.egesa.clinic.shared.data.WardOverviewDto
import com.egesa.clinic.shared.data.toDto
import com.egesa.clinic.shared.data.toDomain
import com.egesa.clinic.shared.data.toPatient
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.JsonElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::hospitalApi).start(wait = true)
}

fun Application.hospitalApi() {
    val state = HospitalState()
    val mpesaService = MpesaService()
    val persistence = SupabasePersistence.fromEnvironment()
    val notificationService = NotificationService()
    val reconciliationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    reconciliationScope.launch {
        while (isActive) {
            state.reconcilePendingStkRequests(::simulateStkStatusLookup)
            delay(30.seconds)
        }
    }

    environment.monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        reconciliationScope.cancel()
    }

    install(ContentNegotiation) {
        json()
    }
    install(io.ktor.server.auth.Authentication) {
        jwt("auth-jwt") {
            verifier(JwtConfig.verifier())
            validate { credential ->
                if (!credential.subject.isNullOrBlank()) io.ktor.server.auth.jwt.JWTPrincipal(credential.payload) else null
            }
        }
    }

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }
        post("/auth/login") {
            val request = runCatching { call.receive<LoginRequest>() }
                .getOrElse { throw BadRequestException("Invalid login payload") }
            val user = runCatching { persistence?.verifyStaff(request.staffId, request.pin) }.getOrNull()
                ?: AuthStore.verify(request.staffId, request.pin)
            if (user == null) {
                logAudit(persistence, state, "unknown", request.staffId, "LOGIN_FAILED", "Auth", request.staffId, null, false)
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            } else {
                logAudit(persistence, state, user.name, user.id, "LOGIN_SUCCESS", "Auth", user.id, null, true)
                call.respond(JwtConfig.createToken(user))
            }
        }

        get("/auth/staff") {
            val staff = runCatching { persistence?.staffDirectory() }.getOrNull() ?: AuthStore.staffDirectory()
            call.respond(staff.map { it.toStaffMemberDto() })
        }

        authenticate("auth-jwt") {
            get("/auth/me") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                call.respond(mapOf("id" to principal?.userId(), "name" to principal?.name(), "role" to principal?.role()))
            }

            // ── Patient endpoints with permission checks ────────────────────────────
            get("/staff") {
                val staff = runCatching { persistence?.staffDirectory() }.getOrNull() ?: AuthStore.staffDirectory()
                call.respond(staff.map { it.toStaffMemberDto() })
            }

            get("/patients") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    logDenied(persistence, state, principal, Permission.PATIENT_READ, "Patients", "GET /patients")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Patient read access denied"))
                    return@get
                }
                val patients = runCatching { persistence?.allPatients() }.getOrNull() ?: state.allPatients()
                call.respond(patients.map { it.toDto() })
            }

            post("/patients") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!hasAnyPermission(principal, Permission.PATIENT_CREATE, Permission.QUEUE_MANAGE)) {
                    logDenied(persistence, state, principal, Permission.PATIENT_CREATE, "Patients", "POST /patients")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Patient registration access denied"))
                    return@post
                }
                val request = runCatching { call.receive<PatientRegistrationDto>() }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid patient registration payload"))
                        return@post
                    }
                val error = validateRegistration(request)
                if (error != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
                    return@post
                }

                val patient = runCatching { persistence?.upsertPatient(request.toPatient(), request.triageLevel) }
                    .getOrNull() ?: state.registerPatient(request.toPatient())
                val queueItem = runCatching { persistence?.checkInPatient(patient.id, patient.fullName, request.triageLevel) }
                    .getOrNull() ?: state.checkInPatient(patient.id, patient.fullName, request.triageLevel)
                logAudit(persistence, state, principal?.name().orEmpty(), principal?.userId().orEmpty(), "PATIENT_REGISTERED", "Patients", patient.id, Permission.PATIENT_CREATE, true)
                call.respond(HttpStatusCode.Created, PatientRegistrationResponseDto(patient.toDto(), queueItem.toDto()))
            }

            get("/queue") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    logDenied(persistence, state, principal, Permission.PATIENT_READ, "Queue", "GET /queue")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Queue read access denied"))
                    return@get
                }
                val queue = runCatching { persistence?.activeQueue() }.getOrNull() ?: state.receptionQueue()
                call.respond(queue.map { it.toDto() })
            }

            post("/queue/check-in") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.QUEUE_MANAGE)) {
                    logDenied(persistence, state, principal, Permission.QUEUE_MANAGE, "Queue", "POST /queue/check-in")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Queue management access denied"))
                    return@post
                }
                val request = runCatching { call.receive<QueueCheckInRequestDto>() }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid queue check-in payload"))
                        return@post
                    }
                if (request.patientId.isBlank() || request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Patient ID and name are required"))
                    return@post
                }
                val patient = runCatching { persistence?.patientById(request.patientId) }.getOrNull()
                    ?: state.patientById(request.patientId)
                if (patient == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Patient not found"))
                    return@post
                }
                val queueItem = runCatching { persistence?.checkInPatient(patient.id, patient.fullName, request.triageLevel, request.checkedInAt) }
                    .getOrNull() ?: state.checkInPatient(patient.id, patient.fullName, request.triageLevel, request.checkedInAt)
                logAudit(persistence, state, principal?.name().orEmpty(), principal?.userId().orEmpty(), "QUEUE_CHECK_IN", "Queue", patient.id, Permission.QUEUE_MANAGE, true)
                call.respond(HttpStatusCode.Created, queueItem.toDto())
            }

            post("/queue/{patientId}/check-out") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.QUEUE_MANAGE)) {
                    logDenied(persistence, state, principal, Permission.QUEUE_MANAGE, "Queue", "POST /queue/{patientId}/check-out")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Queue management access denied"))
                    return@post
                }
                val patientId = call.parameters["patientId"].orEmpty()
                if (patientId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Patient ID is required"))
                    return@post
                }
                val queueItem = runCatching { persistence?.checkOutPatient(patientId) }.getOrNull()
                    ?: state.checkOutPatient(patientId)
                if (queueItem == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Patient is not in the active queue"))
                    return@post
                }
                logAudit(persistence, state, principal?.name().orEmpty(), principal?.userId().orEmpty(), "QUEUE_CHECK_OUT", "Queue", patientId, Permission.QUEUE_MANAGE, true)
                call.respond(queueItem.toDto())
            }

            get("/beds") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Bed board access denied"))
                    return@get
                }
                val patients = runCatching { persistence?.allPatients() }.getOrNull() ?: state.allPatients()
                call.respond(patients.toWardBeds())
            }

            get("/metrics") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Metrics access denied"))
                    return@get
                }
                call.respond(state.metrics().map { DashboardMetricDto(it.title, it.value, it.subtitle) })
            }

            // --- Client API routes (mirrors shared/data/ClinicApi.kt) ---
            get("/dashboard/kpis") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "KPI access denied"))
                    return@get
                }
                call.respond(state.adminKpis().map { DashboardMetricDto(it.title, it.value, it.subtitle) })
            }

            get("/dashboard/bottlenecks") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Bottleneck access denied"))
                    return@get
                }
                call.respond(state.bottleneckHeatmap().map { BottleneckCellDto(it.workflowStage, it.severity, it.pendingCount) })
            }

            get("/dashboard/trend") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Trend access denied"))
                    return@get
                }
                call.respond(state.registrationTrend().map { TrendPointDto(it.label, it.value) })
            }

            get("/wards/overview") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Ward overview access denied"))
                    return@get
                }
                val patients = runCatching { persistence?.allPatients() }.getOrNull() ?: state.allPatients()
                val o = patients.toWardOverview()
                call.respond(WardOverviewDto(o.occupancyPercent, o.bedsAvailable, o.nurseWorkload, o.alerts))
            }

            get("/wards/beds") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Bed board access denied"))
                    return@get
                }
                val patients = runCatching { persistence?.allPatients() }.getOrNull() ?: state.allPatients()
                call.respond(patients.toBedBoard().map {
                    BedCardDto(it.ward, it.roomBed, it.patientName, it.status, it.acuity, it.isolation)
                })
            }

            get("/wards/tasks") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Tasks access denied"))
                    return@get
                }
                call.respond(state.nursingTasks().map { NursingTaskDto(it.type, it.detail, it.due, it.priority) })
            }

            get("/wards/census") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Census access denied"))
                    return@get
                }
                val patients = runCatching { persistence?.allPatients() }.getOrNull() ?: state.allPatients()
                call.respond(patients.toWardCensus().map { WardCensusRowDto(it.ward, it.occupiedBeds, it.totalBeds, it.highAcuityCount, it.isolationCount) })
            }

            get("/wards/atd") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_UPDATE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "ATD access denied"))
                    return@get
                }
                val s = state.atdState()
                val checklist = s.dischargeChecklist.map { (label, complete) -> ChecklistItemDto(label, complete) }
                call.respond(AdmissionTransferDischargeStateDto(s.selectedPatientId, s.selectedBed, s.transferWard, checklist))
            }

            get("/wards/handoff") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Handoff access denied"))
                    return@get
                }
                val shiftName = call.request.queryParameters["shift"] ?: Shift.DAY.name
                val shift = runCatching { Shift.valueOf(shiftName) }.getOrDefault(Shift.DAY)
                call.respond(state.shiftHandoffSummary(shift))
            }

            // ── Payment endpoints with permission checks ────────────────────────────
            post("/payments/stk-push") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PAYMENT_INITIATE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Payment initiation not allowed"))
                    return@post
                }
                val request = call.receive<StkPushRequest>()
                call.respond(mpesaService.initiateStkPush(request))
            }

            get("/payments/{checkoutRequestId}/status") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PAYMENT_INITIATE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Payment status access denied"))
                    return@get
                }
                val checkoutRequestId = call.parameters["checkoutRequestId"].orEmpty()
                call.respond(mpesaService.status(checkoutRequestId))
            }

            post("/payments/callback/mpesa") {
                val payload = call.receive<JsonElement>()
                val response = mpesaService.parseCallback(payload)
                logAudit(persistence, state, "mpesa", "mpesa", "PAYMENT_CALLBACK_PARSED", "Payments", response.data?.toString().orEmpty(), null, response.success)
                call.respond(response)
            }

            get("/payments/sync-health") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Sync health access denied"))
                    return@get
                }
                call.respond(state.syncHealth())
            }

            get("/payments/pending-stk") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Pending STK access denied"))
                    return@get
                }
                call.respond(state.pendingStkRequests())
            }


            // ── Reporting endpoints ────────────────────────────────────────────────
            get("/reports/{report}") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Report access denied"))
                    return@get
                }
                val report = call.parameters["report"].orEmpty()
                call.respondReport(report)
            }
            // ── Admin endpoints ────────────────────────────────────────────────────
            get("/admin/audit-trail") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    logDenied(persistence, state, principal, Permission.AUDIT_VIEW, "Audit", "GET /admin/audit-trail")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@get
                }
                val audit = runCatching { persistence?.auditTrail() }.getOrNull() ?: state.auditTrail()
                call.respond(audit)
            }

            // ── Scheduling & Appointments ───────────────────────────────────────
            get("/schedules") {
                val schedules = persistence?.allSchedules() ?: emptyList()
                call.respond(schedules)
            }

            post("/schedules") {
                val req = call.receive<Schedule>()
                val saved = persistence?.upsertSchedule(req) ?: req
                call.respond(HttpStatusCode.Created, saved)
            }

            get("/appointments") {
                val appointments = persistence?.allAppointments() ?: emptyList()
                call.respond(appointments)
            }

            post("/appointments/book") {
                val req = call.receive<Appointment>()
                val conflicts = persistence?.checkOverlappingAppointments(req.scheduleId, req.startTime, req.endTime) ?: 0
                if (conflicts > 0) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Overlapping appointment booked for this provider."))
                    return@post
                }
                val saved = persistence?.upsertAppointment(req) ?: req
                
                val patient = persistence?.patientById(req.patientId)
                patient?.let { p ->
                    val message = "Hello ${p.fullName}, your clinic appointment is booked for ${req.startTime}."
                    val phone = "254700000000"
                    kotlinx.coroutines.GlobalScope.launch {
                        notificationService.sendSms(phone, message)
                    }
                }
                call.respond(HttpStatusCode.Created, saved)
            }

            // ── Cloud sync endpoints ───────────────────────────────────────────────
            get("/sync/patients") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Sync access denied"))
                    return@get
                }
                val remoteVersion = call.request.queryParameters["version"]?.toLongOrNull() ?: 0L
                val patients = runCatching { persistence?.allPatients() }.getOrNull() ?: state.allPatients()
                call.respond(
                    SyncPatientDataDto(
                        patients = patients.map { it.toDto() },
                        remoteVersion = System.currentTimeMillis(),
                        count = patients.size
                    )
                )
            }

            post("/sync/patients/batch") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_UPDATE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Batch sync not allowed"))
                    return@post
                }
                val updates = runCatching { call.receive<List<PatientDto>>() }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid batch payload"))
                        return@post
                    }

                val results = runCatching { persistence?.uploadPatients(updates) }.getOrNull()
                    ?: updates.map { patient ->
                        state.registerPatient(patient.toDomain())
                        SyncResultItemDto(id = patient.id, status = "synced", version = 1)
                    }
                logAudit(persistence, state, principal?.name().orEmpty(), principal?.userId().orEmpty(), "PATIENT_SYNC_BATCH", "Sync", "${updates.size} patients", Permission.PATIENT_UPDATE, true)
                call.respond(results)
            }

            post("/sync/clinical/batch") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_UPDATE)) {
                    logDenied(persistence, state, principal, Permission.PATIENT_UPDATE, "Sync", "POST /sync/clinical/batch")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Clinical sync not allowed"))
                    return@post
                }
                val batch = runCatching { call.receive<ClinicalSyncBatchDto>() }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid clinical sync payload"))
                        return@post
                    }
                val results = runCatching { persistence?.uploadClinical(batch) }.getOrNull()
                    ?: fallbackClinicalSyncResults(batch)
                logAudit(persistence, state, principal?.name().orEmpty(), principal?.userId().orEmpty(), "CLINICAL_SYNC_BATCH", "Sync", batch.summary(), Permission.PATIENT_UPDATE, true)
                call.respond(results)
            }

            post("/sync/resolve-conflict") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_UPDATE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Conflict resolution not allowed"))
                    return@post
                }
                val req = call.receive<ConflictResolutionRequestDto>()
                call.respond(
                    ConflictResolutionResultDto(
                        resolved = true,
                        strategy = req.strategy,
                        finalVersion = maxOf(req.localVersion, req.remoteVersion)
                    )
                )
            }
        }
        get("/scope") {
            call.respondText("""
                Multiplatform clients: Android + Desktop Compose.
                Secure auth: JWT-based login (/auth/login) with role-based endpoints.
                Database server: Supabase/PostgreSQL migrations available under infra/supabase.
                Core modules: registration, appointments/queue, consultation, diagnosis, wards, billing/STK, reporting, audit trail.
            """.trimIndent())
        }
    }
}

private fun AuthUser.toStaffMemberDto(): StaffMemberDto = StaffMemberDto(
    id = id,
    fullName = name,
    role = role.name,
    department = when (role) {
        com.egesa.clinic.shared.UserRole.ADMIN -> "Administration"
        com.egesa.clinic.shared.UserRole.DOCTOR -> "General Medicine"
        com.egesa.clinic.shared.UserRole.NURSE -> "Nursing"
        com.egesa.clinic.shared.UserRole.PHARMACIST -> "Pharmacy"
        com.egesa.clinic.shared.UserRole.RECEPTIONIST -> "Front Desk"
    }
)

private fun validateRegistration(request: PatientRegistrationDto): String? = when {
    request.id.isBlank() -> "Patient ID is required"
    request.fullName.isBlank() -> "Full name is required"
    request.age < 0 -> "Age must be zero or greater"
    request.triageLevel !in 1..5 -> "Triage level must be between 1 and 5"
    else -> null
}

private suspend fun logDenied(
    persistence: SupabasePersistence?,
    state: HospitalState,
    principal: io.ktor.server.auth.jwt.JWTPrincipal?,
    permission: Permission,
    module: String,
    context: String
) {
    logAudit(
        persistence = persistence,
        state = state,
        user = principal?.name().orEmpty(),
        userId = principal?.userId().orEmpty(),
        action = "PERMISSION_DENIED",
        module = module,
        context = context,
        permission = permission,
        granted = false
    )
}

private suspend fun logAudit(
    persistence: SupabasePersistence?,
    state: HospitalState,
    user: String,
    userId: String,
    action: String,
    module: String,
    context: String,
    permission: Permission?,
    granted: Boolean
) {
    val event = AuditEvent(
        user = user.ifBlank { "unknown" },
        userId = userId,
        action = action,
        module = module,
        timestamp = kotlinx.datetime.Clock.System.now().toString(),
        contextReference = context,
        permission = permission,
        granted = granted
    )
    val persisted = runCatching { persistence?.logAudit(event) }.isSuccess && persistence != null
    if (!persisted) state.logEvent(event)
}

private fun List<Patient>.toWardBeds(): List<WardBed> =
    filter { it.assignedWard != null && it.roomBed != null }
        .map { WardBed(bedId = it.roomBed!!, wardName = it.assignedWard!!, occupiedBy = it.fullName) }

private fun List<Patient>.toBedBoard(): List<BedCard> =
    filter { it.assignedWard != null && it.roomBed != null }
        .map { BedCard(it.assignedWard!!, it.roomBed!!, it.fullName, it.status, it.acuity, it.isolation) }

private fun List<Patient>.toWardOverview(): WardOverview {
    val admitted = filter { it.assignedWard != null && it.roomBed != null }
    val occupied = admitted.size
    val totalBeds = maxOf(occupied, 1)
    return WardOverview(
        occupancyPercent = ((occupied.toDouble() / totalBeds) * 100).toInt(),
        bedsAvailable = maxOf(totalBeds - occupied, 0),
        nurseWorkload = if (occupied == 0) "N/A" else "$occupied active",
        alerts = admitted.filter { it.acuity.equals("Critical", true) || it.isolation != null }
            .map { "${it.fullName}: ${it.acuity}${it.isolation?.let { iso -> " / Isolation: $iso" } ?: ""}" }
    )
}

private fun List<Patient>.toWardCensus(): List<WardCensusRow> =
    filter { it.assignedWard != null }
        .groupBy { it.assignedWard!! }
        .map { (ward, patients) ->
            WardCensusRow(
                ward = ward,
                occupiedBeds = patients.size,
                totalBeds = patients.size,
                highAcuityCount = patients.count { it.acuity.equals("High", true) || it.acuity.equals("Critical", true) },
                isolationCount = patients.count { it.isolation != null }
            )
        }

private fun fallbackClinicalSyncResults(batch: ClinicalSyncBatchDto): List<SyncResultItemDto> =
    buildList {
        batch.encounters.forEach { add(SyncResultItemDto(it.encounterId, "queued-local", 0)) }
        batch.vitalSigns.forEach { add(SyncResultItemDto(it.vitalSignsId, "queued-local", 0)) }
        batch.diagnoses.forEach { add(SyncResultItemDto(it.diagnosisId, "queued-local", 0)) }
        batch.medicationOrders.forEach { add(SyncResultItemDto(it.medicationOrderId, "queued-local", 0)) }
        batch.encounterOutcomes.forEach { add(SyncResultItemDto(it.outcomeId, "queued-local", 0)) }
        batch.htsEntries.forEach { add(SyncResultItemDto(it.htsId, "queued-local", 0)) }
        batch.serviceEvents.forEach { add(SyncResultItemDto(it.serviceEventId, "queued-local", 0)) }
        batch.patientDocuments.forEach { add(SyncResultItemDto(it.documentId, "queued-local", 0)) }
    }

private fun ClinicalSyncBatchDto.summary(): String =
    "encounters=${encounters.size}, vitals=${vitalSigns.size}, diagnoses=${diagnoses.size}, medications=${medicationOrders.size}, outcomes=${encounterOutcomes.size}, hts=${htsEntries.size}, services=${serviceEvents.size}, documents=${patientDocuments.size}"

private fun simulateStkStatusLookup(requestId: String): StkRequestStatus {
    val seed = requestId.hashCode() + Random.nextInt(100)
    return when (seed % 5) {
        0 -> StkRequestStatus.FAILED
        1, 2 -> StkRequestStatus.PENDING
        else -> StkRequestStatus.SUCCESS
    }
}

// ── Sync-related request/response classes ──────────────────────────────────

@kotlinx.serialization.Serializable
data class SyncPatientRequest(
    val patientId: String,
    val version: Int,
    val localVersion: Int
)

@kotlinx.serialization.Serializable
data class SyncPatientResponse(
    val patientId: String,
    val updated: Boolean,
    val remoteVersion: Int,
    val message: String
)

@kotlinx.serialization.Serializable
data class ConflictResolutionRequest(
    val entityId: String,
    val localVersion: Int,
    val remoteVersion: Int,
    val strategy: String  // CLIENT_WINS, SERVER_WINS, MERGE
)

@kotlinx.serialization.Serializable
data class ConflictResolutionResponse(
    val resolved: Boolean,
    val strategy: String,
    val finalVersion: Int
)

