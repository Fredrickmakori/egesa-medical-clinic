package com.egesa.clinic.server

import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.Permission
import com.egesa.clinic.shared.StkRequestStatus
import com.egesa.clinic.shared.Shift
import com.egesa.clinic.shared.data.AdmissionTransferDischargeStateDto
import com.egesa.clinic.shared.data.BedCardDto
import com.egesa.clinic.shared.data.BottleneckCellDto
import com.egesa.clinic.shared.data.ChecklistItemDto
import com.egesa.clinic.shared.data.ConflictResolutionRequestDto
import com.egesa.clinic.shared.data.ConflictResolutionResultDto
import com.egesa.clinic.shared.data.DashboardMetricDto
import com.egesa.clinic.shared.data.NursingTaskDto
import com.egesa.clinic.shared.data.PatientDto
import com.egesa.clinic.shared.data.QueueItemDto
import com.egesa.clinic.shared.data.SyncPatientDataDto
import com.egesa.clinic.shared.data.SyncResultItemDto
import com.egesa.clinic.shared.data.TrendPointDto
import com.egesa.clinic.shared.data.WardCensusRowDto
import com.egesa.clinic.shared.data.WardOverviewDto
import com.egesa.clinic.shared.data.toDto
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
            val user = AuthStore.verify(request.staffId, request.pin)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            } else {
                call.respond(JwtConfig.createToken(user))
            }
        }

        authenticate("auth-jwt") {
            get("/auth/me") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                call.respond(mapOf("id" to principal?.userId(), "name" to principal?.name(), "role" to principal?.role()))
            }

            // ── Patient endpoints with permission checks ────────────────────────────
            get("/patients") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Patient read access denied"))
                    return@get
                }
                call.respond(state.allPatients().map { it.toDto() })
            }

            get("/queue") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Queue read access denied"))
                    return@get
                }
                call.respond(state.receptionQueue().map { QueueItemDto(it.patientId, it.name, it.triageLevel, it.waitMinutes) })
            }

            get("/beds") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Bed board access denied"))
                    return@get
                }
                call.respond(state.wardBeds())
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
                val o = state.wardOverview()
                call.respond(WardOverviewDto(o.occupancyPercent, o.bedsAvailable, o.nurseWorkload, o.alerts))
            }

            get("/wards/beds") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Bed board access denied"))
                    return@get
                }
                call.respond(state.bedBoard().map {
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
                call.respond(state.wardCensus().map { WardCensusRowDto(it.ward, it.occupiedBeds, it.totalBeds, it.highAcuityCount, it.isolationCount) })
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
                call.respond(mpesaService.parseCallback(payload))
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
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@get
                }
                call.respond(state.auditTrail())
            }

            // ── Cloud sync endpoints ───────────────────────────────────────────────
            get("/sync/patients") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Sync access denied"))
                    return@get
                }
                val remoteVersion = call.request.queryParameters["version"]?.toLongOrNull() ?: 0L
                val patients = state.allPatients()
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

                val results = updates.map { patient ->
                    SyncResultItemDto(id = patient.id, status = "synced", version = 1)
                }
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

