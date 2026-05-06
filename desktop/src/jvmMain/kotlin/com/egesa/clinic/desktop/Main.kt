@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.egesa.clinic.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.egesa.clinic.shared.ui.ClinicApp
import com.egesa.clinic.shared.ui.ClientPlatform

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1280.dp, 800.dp))
    Window(
        onCloseRequest = ::exitApplication,
        title          = "Egesa Medical Clinic",
        state          = windowState,
    ) {
        ClinicApp(ClientPlatform.Desktop)
    }
}
