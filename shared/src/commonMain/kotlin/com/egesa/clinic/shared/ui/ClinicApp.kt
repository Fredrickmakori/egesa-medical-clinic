package com.egesa.clinic.shared.ui

import androidx.compose.runtime.*
import com.egesa.clinic.shared.data.ClinicAuth
import com.egesa.clinic.shared.data.FakeRepository
import com.egesa.clinic.shared.data.KtorClinicApi
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.sync.SyncManager
import com.egesa.clinic.shared.sync.SyncNotifier
import com.egesa.clinic.shared.sync.SyncStatus
import com.egesa.clinic.shared.sync.SyncUploader
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.NoopDocumentCaptureGateway
import com.egesa.clinic.shared.db.ClinicDatabase
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.screens.LoginScreen
import com.egesa.clinic.shared.ui.theme.ClinicTheme
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

enum class ClientPlatform { Desktop, Tablet, Mobile }

@Composable
fun ClinicApp(
    platform: ClientPlatform,
    uiMode: AppUiMode = AppUiMode.Adaptive,
    databaseDriverFactory: DatabaseDriverFactory,
    apiBaseUrl: String? = null,
    allowMockFallback: Boolean = true,
    documentCaptureGateway: DocumentCaptureGateway = NoopDocumentCaptureGateway,
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
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            // Bearer token is read from ClinicAuth on every request (set after login).
            defaultRequest {
                ClinicAuth.accessToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }
        }
        
        // Use emulator localhost for Android, standard localhost for others (dev only).
        val defaultBaseUrl = when (platform) {
            ClientPlatform.Tablet -> "http://10.0.2.2:8080"
            ClientPlatform.Desktop, ClientPlatform.Mobile -> "http://localhost:8080"
        }
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

        LaunchedEffect(session?.staffId, session?.token) {
            val staffId = session?.staffId ?: return@LaunchedEffect
            ClinicAuth.setAccessToken(session?.token)
            val repository = localRepository ?: return@LaunchedEffect
            val syncManager = SyncManager(repository)

            suspend fun runSyncCycle() {
                session = session?.copy(syncStatus = SyncStatus.SYNCING)
                val health = syncManager.runAutoSync(isOnline = ClinicAuth.hasToken()) { entityType, entityId, payload ->
                    SyncUploader.upload(repository, entityType, entityId, payload)
                }
                session = session?.copy(
                    syncStatus = health.status,
                    lastSyncTime = Clock.System.now().toEpochMilliseconds()
                )
            }

            runSyncCycle()
            coroutineScope {
                launch {
                    SyncNotifier.requests.collect {
                        if (session?.staffId == staffId) runSyncCycle()
                    }
                }
                while (isActive && session?.staffId == staffId) {
                    delay(30_000)
                    runSyncCycle()
                }
            }
        }

        if (session == null) {
            LoginScreen(localRepository = localRepository!!, onLogin = { session = it })
        } else {
            ClinicAuthenticatedShell(
                uiMode = uiMode,
                session = session!!,
                localRepository = localRepository!!,
                documentCaptureGateway = documentCaptureGateway,
                onLogout = {
                    FakeRepository.clearAccessToken()
                    session = null
                },
            )
        }
    }
}

