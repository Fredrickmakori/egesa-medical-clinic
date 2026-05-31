@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.egesa.clinic.desktop

import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.DocumentCaptureResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.egesa.clinic.shared.ui.ClinicApp
import com.egesa.clinic.shared.ui.ClientPlatform
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame

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
            documentCaptureGateway = DesktopDocumentCaptureGateway,
        )
    }
}

private object DesktopDocumentCaptureGateway : DocumentCaptureGateway {
    override val canCapturePhoto: Boolean = false
    override val canAttachImage: Boolean = true

    override suspend fun capturePhoto(documentType: String): DocumentCaptureResult =
        DocumentCaptureResult(message = "Camera capture is only available on supported tablet devices.")

    override suspend fun attachImage(documentType: String): DocumentCaptureResult {
        val dialog = FileDialog(null as Frame?, "Attach $documentType image", FileDialog.LOAD)
        dialog.isVisible = true
        val file = dialog.file
        val directory = dialog.directory
        dialog.dispose()
        return if (file == null || directory == null) {
            DocumentCaptureResult(message = "No document image selected.")
        } else {
            DocumentCaptureResult(
                imageUri = java.io.File(directory, file).absolutePath,
                message = "Document image attached. Review or extract document data before registering."
            )
        }
    }

    override suspend fun extractData(imageUri: String, documentType: String): DocumentCaptureResult =
        DocumentCaptureResult(
            imageUri = imageUri,
            message = "Image saved for verification. OCR extraction is ready to connect to the configured OCR service."
        )
}
