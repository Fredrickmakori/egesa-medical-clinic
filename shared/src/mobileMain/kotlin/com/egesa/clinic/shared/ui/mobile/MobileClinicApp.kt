package com.egesa.clinic.shared.ui.mobile

import androidx.compose.runtime.Composable
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.NoopDocumentCaptureGateway
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import com.egesa.clinic.shared.ui.AppUiMode
import com.egesa.clinic.shared.ui.ClinicApp
import com.egesa.clinic.shared.ui.ClientPlatform

/**
 * Convenience entry for Android and iOS apps.
 */
@Composable
fun MobileClinicApp(
    platform: ClientPlatform,
    databaseDriverFactory: DatabaseDriverFactory,
    apiBaseUrl: String? = null,
    allowMockFallback: Boolean = true,
    documentCaptureGateway: DocumentCaptureGateway = NoopDocumentCaptureGateway,
) {
    ClinicApp(
        platform = platform,
        uiMode = AppUiMode.MobileNative,
        databaseDriverFactory = databaseDriverFactory,
        apiBaseUrl = apiBaseUrl,
        allowMockFallback = allowMockFallback,
        documentCaptureGateway = documentCaptureGateway,
    )
}
