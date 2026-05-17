package com.egesa.clinic.server

import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.StkRequestStatus
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

            get("/patients") { call.respond(state.allPatients()) }
            get("/queue") { call.respond(state.receptionQueue()) }
            get("/beds") { call.respond(state.wardBeds()) }
            get("/metrics") { call.respond(state.metrics()) }

            post("/payments/stk-push") {
                val request = call.receive<StkPushRequest>()
                call.respond(mpesaService.initiateStkPush(request))
            }

            get("/payments/{checkoutRequestId}/status") {
                val checkoutRequestId = call.parameters["checkoutRequestId"].orEmpty()
                call.respond(mpesaService.status(checkoutRequestId))
            }

            post("/payments/callback/mpesa") {
                val payload = call.receive<JsonElement>()
                call.respond(mpesaService.parseCallback(payload))
            }
            get("/payments/sync-health") { call.respond(state.syncHealth()) }
            get("/payments/pending-stk") { call.respond(state.pendingStkRequests()) }

            get("/admin/audit-trail") {
                val role = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()?.role()
                if (role != "ADMIN") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                } else {
                    call.respond(state.auditTrail())
                }
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
