package com.egesa.clinic.shared.ui

import androidx.compose.runtime.*
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.screens.LoginScreen
import com.egesa.clinic.shared.ui.shell.ResponsiveShell
import com.egesa.clinic.shared.ui.theme.ClinicTheme

enum class ClientPlatform { Desktop, Tablet }

@Composable
fun ClinicApp(@Suppress("UNUSED_PARAMETER") platform: ClientPlatform, databaseDriverFactory: DatabaseDriverFactory) {
    val localRepository = remember { LocalRepository(databaseDriverFactory) }
    
    LaunchedEffect(Unit) {
        localRepository.seedAdminIfEmpty()
    }
    
    ClinicTheme {
        var session by remember { mutableStateOf<SessionState?>(null) }

        if (session == null) {
            LoginScreen(localRepository = localRepository, onLogin = { session = it })
        } else {
            // ResponsiveShell automatically adapts to screen size
            // No need to manually switch between Desktop/Tablet layouts
            ResponsiveShell(session!!, localRepository, onLogout = { session = null })
        }
    }
}

