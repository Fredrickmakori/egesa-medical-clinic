package com.egesa.clinic.shared.ui

import androidx.compose.runtime.*
import com.egesa.clinic.shared.data.FakeRepository
import com.egesa.clinic.shared.data.KtorClinicApi
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.db.ClinicDatabase
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.screens.LoginScreen
import com.egesa.clinic.shared.ui.shell.ResponsiveShell
import com.egesa.clinic.shared.ui.theme.ClinicTheme
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json

enum class ClientPlatform { Desktop, Tablet }

@Composable
fun ClinicApp(
    @Suppress("UNUSED_PARAMETER") platform: ClientPlatform,
    databaseDriverFactory: DatabaseDriverFactory,
    apiBaseUrl: String? = null,
    allowMockFallback: Boolean = true,
) {
    var localRepository by remember { mutableStateOf<LocalRepository?>(null) }

    LaunchedEffect(databaseDriverFactory) {
        val driver = databaseDriverFactory.createDriver()
        val repository = LocalRepository(ClinicDatabase(driver))
        if (allowMockFallback) {
            // Demo-only convenience. Real deployments must provision staff via a secure admin workflow.
            repository.seedAdminIfEmpty()
        }
        localRepository = repository

        FakeRepository.useMockFallback = allowMockFallback

        // Initialize ClinicApi for backend communication
        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json()
            }
            // Bearer token is installed after login via FakeRepository.accessToken.
            // Server endpoints are JWT-protected; without this header requests will be rejected.
            defaultRequest {
                val token = FakeRepository.accessToken
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
        }
        
        // Use emulator localhost for Android, standard localhost for others (dev only).
        val defaultBaseUrl = if (platform == ClientPlatform.Tablet) "http://10.0.2.2:8080" else "http://localhost:8080"
        val baseUrl = apiBaseUrl ?: defaultBaseUrl

        // For hospital use, mock fallback should be disabled and HTTPS should be used.
        if (!allowMockFallback && baseUrl.startsWith("http://")) {
            // Keep the app running but make the configuration issue obvious.
            // (Without HTTPS, credentials and PHI can be exposed in transit.)
            error("Insecure API base URL for production: $baseUrl. Use HTTPS.")
        }
        
        val api = KtorClinicApi(httpClient, baseUrl)
        FakeRepository.installClinicApi(api)
        
        // Note: FakeRepository is configured to use mock fallback by default in FakeRepository.kt.
    }

    if (localRepository == null) {
        // You could show a loading screen here
        return
    }

    ClinicTheme {
        var session by remember { mutableStateOf<SessionState?>(null) }

        if (session == null) {
            LoginScreen(localRepository = localRepository!!, onLogin = { session = it })
        } else {
            // ResponsiveShell automatically adapts to screen size
            // No need to manually switch between Desktop/Tablet layouts
            ResponsiveShell(session!!, localRepository!!, onLogout = { session = null })
        }
    }
}

