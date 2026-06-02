package com.egesa.clinic.shared.ui

import androidx.compose.runtime.Composable
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.ui.navigation.SessionState

/**
 * Platform-specific authenticated shell (post-login navigation chrome).
 * Mobile targets resolve to [com.egesa.clinic.shared.ui.mobile.MobileShell];
 * desktop and web resolve to the adaptive sidebar shell.
 */
@Composable
internal expect fun ClinicAuthenticatedShell(
    uiMode: AppUiMode,
    session: SessionState,
    localRepository: LocalRepository,
    documentCaptureGateway: DocumentCaptureGateway,
    onLogout: () -> Unit,
)
