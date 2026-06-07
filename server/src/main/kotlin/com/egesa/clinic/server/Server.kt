package com.egesa.clinic.server

import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.Permission
import com.egesa.clinic.shared.AuditEvent
import com.egesa.clinic.shared.BedCard
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.Appointment
import com.egesa.clinic.shared.Schedule
import com.egesa.clinic.shared.WardBed
import com.egesa.clinic.shared.WardCensusRow
import com.egesa.clinic.shared.WardOverview
import com.egesa.clinic.shared.data.BedCardDto
import com.egesa.clinic.shared.data.BottleneckCellDto
import com.egesa.clinic.shared.data.ClinicalSyncBatchDto
import com.egesa.clinic.shared.data.ConflictResolutionRequestDto
import com.egesa.clinic.shared.data.ConflictResolutionResultDto
import com.egesa.clinic.shared.data.DashboardMetricDto
import com.egesa.clinic.shared.data.LabOrderDto
import com.egesa.clinic.shared.data.NursingTaskDto
import com.egesa.clinic.shared.data.PatientDto
import com.egesa.clinic.shared.data.PatientRegistrationDto
import com.egesa.clinic.shared.data.PatientRegistrationResponseDto
import com.egesa.clinic.shared.data.QueueCheckInRequestDto
import com.egesa.clinic.shared.data.QueueItemDto
import com.egesa.clinic.shared.data.StaffMemberDto
import com.egesa.clinic.shared.data.SaveLabResultsRequestDto
import com.egesa.clinic.shared.data.SyncPatientDataDto
import com.egesa.clinic.shared.data.TrendPointDto
import com.egesa.clinic.shared.data.UpdateLabOrderStatusRequestDto
import com.egesa.clinic.shared.data.WardCensusRowDto
import com.egesa.clinic.shared.data.WardOverviewDto
import com.egesa.clinic.shared.data.toDto
import com.egesa.clinic.shared.data.toDomain
import com.egesa.clinic.shared.data.toPatient
import com.egesa.clinic.shared.domain.OpdEncounterBundle
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
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
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::hospitalApi).start(wait = true)
}

fun Application.hospitalApi() {
    val auditFallback = HospitalState()
    val mpesaService = MpesaService()
    val persistence = SupabasePersistence.fromEnvironment()
    val notificationService = NotificationService()
    val reconciliationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    reconciliationScope.launch {
        while (isActive) {
            delay(30.seconds)
        }
    }

    environment.monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        reconciliationScope.cancel()
    }

    install(ContentNegotiation) {
        json()
    }

    routing {
        // Serve the SaaS portal static resources
        staticResources("/portal", "static")

        get("/health") { call.respond(mapOf("status" to "ok")) }

        // --- HIMS SaaS Portal REST Endpoints ---
        post("/api/tenants/register") {
            val request = runCatching { call.receive<TenantRegisterRequest>() }
                .getOrElse { throw BadRequestException("Invalid tenant registration payload") }
            val db = call.requirePersistence(persistence) ?: return@post
            val tenantId = java.util.UUID.randomUUID().toString()
            val tenant = db.registerTenant(
                id = tenantId,
                name = request.name,
                code = request.tenantCode,
                email = request.contactEmail,
                plan = request.plan,
                status = "active" // Default active onboarding
            )
            db.seedTenantAdmin(
                tenantId = tenantId,
                name = request.adminName,
                username = request.adminUsername,
                pin = request.adminPin
            )
            call.respond(HttpStatusCode.Created, tenant)
        }

        get("/api/tenants") {
            val db = call.requirePersistence(persistence) ?: return@get
            val tenants = db.allTenants()
            call.respond(tenants)
        }

        post("/api/tenants/{id}/billing") {
            val id = call.parameters["id"].orEmpty()
            val request = runCatching { call.receive<TenantBillingRequest>() }
                .getOrElse { throw BadRequestException("Invalid billing payload") }
            val db = call.requirePersistence(persistence) ?: return@post
            val updated = db.updateTenantBilling(id, request.plan, request.status, request.amount)
            call.respond(updated)
        }

        get("/api/tenants/{id}/metrics") {
            val id = call.parameters["id"].orEmpty()
            val db = call.requirePersistence(persistence) ?: return@get
            val metrics = db.tenantMetrics(id)
            call.respond(metrics)
        }

        post("/api/tenants/{id}/staff") {
            val id = call.parameters["id"].orEmpty()
            val request = runCatching { call.receive<TenantStaffRequest>() }
                .getOrElse { throw BadRequestException("Invalid staff payload") }
            val db = call.requirePersistence(persistence) ?: return@post
            val newStaff = db.createTenantStaff(id, request.name, request.role, request.pin, request.department)
            call.respond(HttpStatusCode.Created, mapOf(
                "id" to newStaff.id,
                "name" to newStaff.name,
                "role" to newStaff.role.name,
                "department" to request.department
            ))
        }

        post("/auth/login") {
            val request = runCatching { call.receive<LoginRequest>() }
                .getOrElse { throw BadRequestException("Invalid login payload") }
            val db = call.requirePersistence(persistence) ?: return@post
            val requestedTenantCode = request.tenantCode?.trim()?.takeIf { it.isNotBlank() }
                ?: call.request.headers["X-Tenant-Code"]?.trim()?.takeIf { it.isNotBlank() }
            val requestedFacilityId = call.request.headers["X-Facility-Id"]?.trim()?.takeIf { it.isNotBlank() }
            val resolvedFacilityId = when {
                requestedFacilityId != null -> requestedFacilityId
                requestedTenantCode != null -> db.tenantIdByCode(requestedTenantCode) ?: requestedTenantCode
                else -> "default"
            }
            val user = runCatching {
                db.verifyStaff(
                    staffId = request.staffId,
                    pin = request.pin,
                    facilityId = resolvedFacilityId,
                    tenantCode = requestedTenantCode
                )
            }.getOrNull()
            if (user == null) {
                logAudit(persistence, auditFallback, "unknown", request.staffId, "LOGIN_FAILED", "Auth", request.staffId, null, false)
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            } else {
                logAudit(persistence, auditFallback, user.name, user.id, "LOGIN_SUCCESS", "Auth", user.id, null, true)
                call.respond(JwtConfig.createToken(user))
            }
        }

        get("/auth/staff") {
            val db = call.requirePersistence(persistence) ?: return@get
            val requestedTenantCode = call.request.queryParameters["tenantCode"]?.trim()?.takeIf { it.isNotBlank() }
                ?: call.request.headers["X-Tenant-Code"]?.trim()?.takeIf { it.isNotBlank() }
            val requestedFacilityId = call.request.queryParameters["facilityId"]?.trim()?.takeIf { it.isNotBlank() }
                ?: call.request.headers["X-Facility-Id"]?.trim()?.takeIf { it.isNotBlank() }
            val resolvedFacilityId = when {
                requestedFacilityId != null -> requestedFacilityId
                requestedTenantCode != null -> db.tenantIdByCode(requestedTenantCode) ?: requestedTenantCode
                else -> "default"
            }
            val staff = db.staffDirectory(resolvedFacilityId)
            call.respond(staff.map { it.toStaffMemberDto() })
        }

        authenticate("auth-jwt") {
            get("/auth/me") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                call.respond(
                    mapOf(
                        "id" to principal?.userId(),
                        "name" to principal?.name(),
                        "role" to principal?.role(),
                        "facilityId" to principal?.facilityId(),
                        "tenantCode" to principal?.tenantCode()
                    )
                )
            }

            // ── Patient endpoints with permission checks ────────────────────────────
            get("/staff") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val staff = db.staffDirectory(facilityId)
                call.respond(staff.map { it.toStaffMemberDto() })
            }

            get("/patients") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    logDenied(persistence, auditFallback, principal, Permission.PATIENT_READ, "Patients", "GET /patients")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Patient read access denied"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val patients = db.allPatients(facilityId)
                call.respond(patients.map { it.toDto() })
            }

            post("/patients") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!hasAnyPermission(principal, Permission.PATIENT_CREATE, Permission.QUEUE_MANAGE)) {
                    logDenied(persistence, auditFallback, principal, Permission.PATIENT_CREATE, "Patients", "POST /patients")
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

                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@post
                val patient = db.upsertPatient(request.toPatient(), request.triageLevel, facilityId)
                val queueItem = db.checkInPatient(patient.id, patient.fullName, request.triageLevel, facilityId = facilityId)
                logAudit(persistence, auditFallback, principal?.name().orEmpty(), principal?.userId().orEmpty(), "PATIENT_REGISTERED", "Patients", patient.id, Permission.PATIENT_CREATE, true)
                call.respond(HttpStatusCode.Created, PatientRegistrationResponseDto(patient.toDto(), queueItem.toDto()))
            }

            get("/queue") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    logDenied(persistence, auditFallback, principal, Permission.PATIENT_READ, "Queue", "GET /queue")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Queue read access denied"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val queue = db.activeQueue(facilityId)
                call.respond(queue.map { it.toDto() })
            }

            post("/queue/check-in") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.QUEUE_MANAGE)) {
                    logDenied(persistence, auditFallback, principal, Permission.QUEUE_MANAGE, "Queue", "POST /queue/check-in")
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
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@post
                val patient = db.patientById(request.patientId, facilityId)
                if (patient == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Patient not found"))
                    return@post
                }
                val queueItem = db.checkInPatient(patient.id, patient.fullName, request.triageLevel, request.checkedInAt, facilityId)
                logAudit(persistence, auditFallback, principal?.name().orEmpty(), principal?.userId().orEmpty(), "QUEUE_CHECK_IN", "Queue", patient.id, Permission.QUEUE_MANAGE, true)
                call.respond(HttpStatusCode.Created, queueItem.toDto())
            }

            post("/queue/{patientId}/check-out") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.QUEUE_MANAGE)) {
                    logDenied(persistence, auditFallback, principal, Permission.QUEUE_MANAGE, "Queue", "POST /queue/{patientId}/check-out")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Queue management access denied"))
                    return@post
                }
                val patientId = call.parameters["patientId"].orEmpty()
                if (patientId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Patient ID is required"))
                    return@post
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@post
                val queueItem = db.checkOutPatient(patientId, facilityId)
                if (queueItem == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Patient is not in the active queue"))
                    return@post
                }
                logAudit(persistence, auditFallback, principal?.name().orEmpty(), principal?.userId().orEmpty(), "QUEUE_CHECK_OUT", "Queue", patientId, Permission.QUEUE_MANAGE, true)
                call.respond(queueItem.toDto())
            }

            get("/beds") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Bed board access denied"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val patients = db.allPatients(facilityId)
                call.respond(patients.toWardBeds())
            }

            get("/metrics") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Metrics access denied"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val patients = db.allPatients(facilityId)
                call.respond(patients.toPatientMetrics())
            }

            // --- Client API routes (mirrors shared/data/ClinicApi.kt) ---
            get("/dashboard/kpis") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "KPI access denied"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val patients = db.allPatients(facilityId)
                call.respond(patients.toPatientMetrics())
            }

            get("/dashboard/bottlenecks") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Bottleneck access denied"))
                    return@get
                }
                call.respond(emptyList<BottleneckCellDto>())
            }

            get("/dashboard/trend") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Trend access denied"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val patients = db.allPatients(facilityId)
                call.respond(listOf(TrendPointDto("Registered patients", patients.size)))
            }

            get("/wards/overview") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Ward overview access denied"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val patients = db.allPatients(facilityId)
                val o = patients.toWardOverview()
                call.respond(WardOverviewDto(o.occupancyPercent, o.bedsAvailable, o.nurseWorkload, o.alerts))
            }

            get("/wards/beds") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Bed board access denied"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val patients = db.allPatients(facilityId)
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
                call.respond(emptyList<NursingTaskDto>())
            }

            get("/wards/census") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Census access denied"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val patients = db.allPatients(facilityId)
                call.respond(patients.toWardCensus().map { WardCensusRowDto(it.ward, it.occupiedBeds, it.totalBeds, it.highAcuityCount, it.isolationCount) })
            }

            get("/wards/atd") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_UPDATE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "ATD access denied"))
                    return@get
                }
                call.respond(HttpStatusCode.NotImplemented, mapOf("error" to "ATD workflow persistence is not implemented yet"))
            }

            get("/wards/handoff") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Handoff access denied"))
                    return@get
                }
                call.respond(emptyList<String>())
            }

            post("/api/lab/orders") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!hasAnyPermission(principal, Permission.CONSULTATION_WRITE, Permission.LAB_RESULT_MANAGE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Lab order creation denied"))
                    return@post
                }
                val request = runCatching { call.receive<LabOrderDto>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid lab order payload"))
                    return@post
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@post
                val saved = db.upsertLabOrder(request, facilityId)
                call.respond(HttpStatusCode.Created, saved)
            }

            get("/api/lab/orders/{id}") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Lab order read access denied"))
                    return@get
                }
                val id = call.parameters["id"].orEmpty()
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val order = db.labOrderById(id, facilityId)
                if (order == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Lab order not found"))
                else call.respond(order)
            }

            get("/api/patients/{patientId}/lab-orders") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Lab order read access denied"))
                    return@get
                }
                val patientId = call.parameters["patientId"].orEmpty()
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val orders = db.labOrdersForPatient(patientId, facilityId)
                call.respond(orders)
            }

            get("/api/lab/worklist") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.LAB_RESULT_MANAGE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Lab worklist access denied"))
                    return@get
                }
                val department = call.request.queryParameters["department"].orEmpty()
                val status = call.request.queryParameters["status"]
                if (department.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "department is required"))
                    return@get
                }
                val fromDate = call.request.queryParameters["fromDate"]
                val toDate = call.request.queryParameters["toDate"]
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val filtered = db.labWorklist(department, status, fromDate, toDate, facilityId)
                call.respond(filtered)
            }

            post("/api/lab/results") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.LAB_RESULT_MANAGE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Lab result entry denied"))
                    return@post
                }
                val request = runCatching { call.receive<SaveLabResultsRequestDto>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid lab result payload"))
                    return@post
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@post
                val saved = db.saveLabResults(request, facilityId)
                call.respond(saved)
            }

            post("/api/lab/orders/{id}/status") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.LAB_RESULT_MANAGE)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Lab status update denied"))
                    return@post
                }
                val id = call.parameters["id"].orEmpty()
                val request = runCatching { call.receive<UpdateLabOrderStatusRequestDto>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status payload"))
                    return@post
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@post
                val updated = db.updateLabOrderStatus(id, request, facilityId)
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Lab order not found"))
                    return@post
                }
                call.respond(updated)
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
                logAudit(persistence, auditFallback, "mpesa", "mpesa", "PAYMENT_CALLBACK_PARSED", "Payments", response.data?.toString().orEmpty(), null, response.success)
                call.respond(response)
            }

            get("/payments/sync-health") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Sync health access denied"))
                    return@get
                }
                call.respond(HttpStatusCode.NotImplemented, mapOf("error" to "Payment sync health persistence is not implemented yet"))
            }

            get("/payments/pending-stk") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.AUDIT_VIEW)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Pending STK access denied"))
                    return@get
                }
                call.respond(emptyList<String>())
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
                    logDenied(persistence, auditFallback, principal, Permission.AUDIT_VIEW, "Audit", "GET /admin/audit-trail")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@get
                }
                val db = call.requirePersistence(persistence) ?: return@get
                val audit = db.auditTrail()
                call.respond(audit)
            }

            // ── Scheduling & Appointments ───────────────────────────────────────
            get("/schedules") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val schedules = db.allSchedules(facilityId)
                call.respond(schedules)
            }

            post("/schedules") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val facilityId = principal?.facilityId() ?: "default"
                val req = call.receive<Schedule>()
                val db = call.requirePersistence(persistence) ?: return@post
                val saved = db.upsertSchedule(req, facilityId)
                call.respond(HttpStatusCode.Created, saved)
            }

            get("/appointments") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val appointments = db.allAppointments(facilityId)
                call.respond(appointments)
            }

            post("/appointments/book") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val facilityId = principal?.facilityId() ?: "default"
                val req = call.receive<Appointment>()
                val db = call.requirePersistence(persistence) ?: return@post
                val conflicts = db.checkOverlappingAppointments(req.scheduleId, req.startTime, req.endTime)
                if (conflicts > 0) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Overlapping appointment booked for this provider."))
                    return@post
                }
                val saved = db.upsertAppointment(req, facilityId)
                
                val patient = db.patientById(req.patientId, facilityId)
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
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val patients = db.allPatients(facilityId)
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

                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@post
                val results = db.uploadPatients(updates, facilityId)
                logAudit(persistence, auditFallback, principal?.name().orEmpty(), principal?.userId().orEmpty(), "PATIENT_SYNC_BATCH", "Sync", "${updates.size} patients", Permission.PATIENT_UPDATE, true)
                call.respond(results)
            }

            post("/sync/clinical/batch") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_UPDATE)) {
                    logDenied(persistence, auditFallback, principal, Permission.PATIENT_UPDATE, "Sync", "POST /sync/clinical/batch")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Clinical sync not allowed"))
                    return@post
                }
                val batch = runCatching { call.receive<ClinicalSyncBatchDto>() }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid clinical sync payload"))
                        return@post
                    }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@post
                val results = db.uploadClinical(batch, facilityId)
                logAudit(persistence, auditFallback, principal?.name().orEmpty(), principal?.userId().orEmpty(), "CLINICAL_SYNC_BATCH", "Sync", batch.summary(), Permission.PATIENT_UPDATE, true)
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

            post("/api/encounters") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.CONSULTATION_WRITE)) {
                    logDenied(persistence, auditFallback, principal, Permission.CONSULTATION_WRITE, "Consultation", "POST /api/encounters")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Consultation write access denied"))
                    return@post
                }
                val bundle = runCatching { call.receive<OpdEncounterBundle>() }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid encounter payload"))
                        return@post
                    }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@post
                val saved = db.upsertEncounterBundle(bundle, facilityId)
                logAudit(
                    persistence,
                    auditFallback,
                    principal?.name().orEmpty(),
                    principal?.userId().orEmpty(),
                    "ENCOUNTER_UPSERT",
                    "Consultation",
                    bundle.encounter.encounterId,
                    Permission.CONSULTATION_WRITE,
                    true
                )
                call.respond(HttpStatusCode.Created, saved)
            }

            get("/api/encounters/{id}") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    logDenied(persistence, auditFallback, principal, Permission.PATIENT_READ, "Consultation", "GET /api/encounters/{id}")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Consultation read access denied"))
                    return@get
                }
                val id = call.parameters["id"].orEmpty()
                if (id.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Encounter id is required"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val encounter = db.encounterBundleById(id, facilityId)
                if (encounter == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Encounter not found"))
                    return@get
                }
                call.respond(encounter)
            }

            get("/api/patients/{patientId}/encounters") {
                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                if (!requirePermission(principal, Permission.PATIENT_READ)) {
                    logDenied(persistence, auditFallback, principal, Permission.PATIENT_READ, "Consultation", "GET /api/patients/{patientId}/encounters")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Consultation read access denied"))
                    return@get
                }
                val patientId = call.parameters["patientId"].orEmpty()
                if (patientId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Patient id is required"))
                    return@get
                }
                val facilityId = principal?.facilityId() ?: "default"
                val db = call.requirePersistence(persistence) ?: return@get
                val encounters = db.encounterBundlesForPatient(patientId, facilityId)
                call.respond(encounters)
            }
        }

        post("/payments/callback/mpesa") {
            val payload = call.receive<JsonElement>()
            call.respond(mpesaService.parseCallback(payload))
        }
        get("/payments/sync-health") { call.respond(state.syncHealth()) }
        get("/payments/pending-stk") { call.respond(state.pendingStkRequests()) }
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

private suspend fun ApplicationCall.requirePersistence(persistence: SupabasePersistence?): SupabasePersistence? {
    if (persistence != null) return persistence
    respond(
        HttpStatusCode.ServiceUnavailable,
        mapOf("error" to "Database connection is not configured. Set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY.")
    )
    return null
}

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

private fun List<Patient>.toPatientMetrics(): List<DashboardMetricDto> =
    listOf(
        DashboardMetricDto("Patients", size.toString(), "Registered in database"),
        DashboardMetricDto("Admitted", count { it.assignedWard != null }.toString(), "Assigned to wards"),
        DashboardMetricDto("Isolation", count { it.isolation != null }.toString(), "Isolation precautions")
    )

private fun ClinicalSyncBatchDto.summary(): String =
    "encounters=${encounters.size}, vitals=${vitalSigns.size}, diagnoses=${diagnoses.size}, medications=${medicationOrders.size}, outcomes=${encounterOutcomes.size}, hts=${htsEntries.size}, services=${serviceEvents.size}, documents=${patientDocuments.size}"

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

@kotlinx.serialization.Serializable
data class TenantRegisterRequest(
    val name: String,
    val tenantCode: String,
    val contactEmail: String,
    val plan: String,
    val adminName: String,
    val adminUsername: String,
    val adminPin: String
)

@kotlinx.serialization.Serializable
data class TenantBillingRequest(
    val plan: String,
    val status: String,
    val amount: Double
)

@kotlinx.serialization.Serializable
data class TenantStaffRequest(
    val name: String,
    val role: String,
    val pin: String,
    val department: String
)
