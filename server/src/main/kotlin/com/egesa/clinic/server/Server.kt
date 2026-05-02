package com.egesa.clinic.server

import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.StkRequestStatus
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
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

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }
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
