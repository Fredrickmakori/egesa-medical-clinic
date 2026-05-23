@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.egesa.clinic.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.egesa.clinic.shared.ui.ClinicApp
import com.egesa.clinic.shared.ui.ClientPlatform
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import androidx.compose.runtime.remember

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1280.dp, 800.dp))
    Window(
        onCloseRequest = ::exitApplication,
        title          = "Egesa Medical Clinic",
        state          = windowState,
    ) {
        val driverFactory = remember { DatabaseDriverFactory() }
        val apiBaseUrl = System.getenv("EGESA_API_BASE_URL")
        val allowMock = (System.getenv("EGESA_ALLOW_MOCK_FALLBACK") ?: "true").equals("true", ignoreCase = true)
        ClinicApp(
            platform = ClientPlatform.Desktop,
            databaseDriverFactory = driverFactory,
            apiBaseUrl = apiBaseUrl,
            allowMockFallback = allowMock,
        )
    }
}
