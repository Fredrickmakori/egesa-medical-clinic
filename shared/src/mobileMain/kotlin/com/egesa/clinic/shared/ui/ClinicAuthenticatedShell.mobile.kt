package com.egesa.clinic.shared.ui

import androidx.compose.runtime.Composable
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.ui.mobile.MobileShell
import com.egesa.clinic.shared.ui.navigation.SessionState

@Composable
internal actual fun ClinicAuthenticatedShell(
    uiMode: AppUiMode,
    session: SessionState,
    localRepository: LocalRepository,
    documentCaptureGateway: DocumentCaptureGateway,
    onLogout: () -> Unit,
) {
    MobileShell(
        session = session,
        localRepository = localRepository,
        documentCaptureGateway = documentCaptureGateway,
        onLogout = onLogout,
    )
}
