package com.egesa.clinic.shared.ui

import androidx.compose.runtime.*
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.screens.LoginScreen
import com.egesa.clinic.shared.ui.shell.DesktopShell
import com.egesa.clinic.shared.ui.shell.TabletShell
import com.egesa.clinic.shared.ui.theme.ClinicTheme

enum class ClientPlatform { Desktop, Tablet }

@Composable
fun ClinicApp(platform: ClientPlatform) {
    ClinicTheme {
        var session by remember { mutableStateOf<SessionState?>(null) }

        if (session == null) {
            LoginScreen(onLogin = { session = it })
        } else {
            when (platform) {
                ClientPlatform.Desktop -> DesktopShell(session!!, onLogout = { session = null })
                ClientPlatform.Tablet  -> TabletShell(session!!, onLogout = { session = null })
            }
        }
    }
}
