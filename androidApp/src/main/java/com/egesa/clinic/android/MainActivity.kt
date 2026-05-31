@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.egesa.clinic.android

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.multidex.MultiDex
import com.egesa.clinic.shared.data.DocumentExtractedData
import com.egesa.clinic.shared.ui.ClinicApp
import com.egesa.clinic.shared.ui.ClientPlatform
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.DocumentCaptureResult
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import androidx.compose.runtime.remember
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiDex.install(this)
        enableEdgeToEdge()
        setContent {
            val driverFactory = remember { DatabaseDriverFactory(this) }
            val documentCaptureGateway = remember { AndroidDocumentCaptureGateway(this) }
            val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                documentCaptureGateway.onImagePicked(uri)
            }
            val cameraPreview = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
                documentCaptureGateway.onPhotoCaptured(bitmap)
            }
            documentCaptureGateway.bind(imagePicker, cameraPreview)
            ClinicApp(
                platform = ClientPlatform.Tablet,
                databaseDriverFactory = driverFactory,
                apiBaseUrl = BuildConfig.API_BASE_URL,
                allowMockFallback = BuildConfig.ALLOW_MOCK_FALLBACK,
                documentCaptureGateway = documentCaptureGateway,
            )
        }
    }
}

private class AndroidDocumentCaptureGateway(
    private val activity: ComponentActivity
) : DocumentCaptureGateway {
    private var imagePicker: ManagedActivityResultLauncher<String, Uri?>? = null
    private var cameraPreview: ManagedActivityResultLauncher<Void?, Bitmap?>? = null
    private var pendingPick: CancellableContinuation<DocumentCaptureResult>? = null
    private var pendingCapture: CancellableContinuation<DocumentCaptureResult>? = null

    override val canCapturePhoto: Boolean = true
    override val canAttachImage: Boolean = true

    fun bind(
        imagePicker: ManagedActivityResultLauncher<String, Uri?>,
        cameraPreview: ManagedActivityResultLauncher<Void?, Bitmap?>
    ) {
        this.imagePicker = imagePicker
        this.cameraPreview = cameraPreview
    }

    override suspend fun capturePhoto(documentType: String): DocumentCaptureResult =
        suspendCancellableCoroutine { continuation ->
            pendingCapture = continuation
            continuation.invokeOnCancellation { pendingCapture = null }
            cameraPreview?.launch(null) ?: continuation.resume(
                DocumentCaptureResult(message = "Camera is not available on this device.")
            )
        }

    override suspend fun attachImage(documentType: String): DocumentCaptureResult =
        suspendCancellableCoroutine { continuation ->
            pendingPick = continuation
            continuation.invokeOnCancellation { pendingPick = null }
            imagePicker?.launch("image/*") ?: continuation.resume(
                DocumentCaptureResult(message = "Image attachment is not available on this device.")
            )
        }

    override suspend fun extractData(imageUri: String, documentType: String): DocumentCaptureResult =
        suspendCancellableCoroutine { continuation ->
            val image = runCatching {
                val uri = if (imageUri.startsWith("/") || imageUri.startsWith("file:")) {
                    Uri.fromFile(File(imageUri.removePrefix("file:")))
                } else {
                    Uri.parse(imageUri)
                }
                InputImage.fromFilePath(activity, uri)
            }.getOrElse {
                continuation.resume(
                    DocumentCaptureResult(
                        imageUri = imageUri,
                        message = "Could not open document image for OCR: ${it.message ?: "unknown error"}"
                    )
                )
                return@suspendCancellableCoroutine
            }

            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { text ->
                    val extracted = parseDocumentText(text)
                    continuation.resume(
                        DocumentCaptureResult(
                            imageUri = imageUri,
                            extractedData = extracted,
                            message = if (extracted.hasAnyField()) {
                                "OCR extracted document data. Review the fields before registering."
                            } else {
                                "OCR completed, but no reliable demographic fields were found. Enter the fields manually."
                            }
                        )
                    )
                }
                .addOnFailureListener {
                    continuation.resume(
                        DocumentCaptureResult(
                            imageUri = imageUri,
                            message = "OCR failed: ${it.message ?: "unknown error"}"
                        )
                    )
                }
        }

    fun onImagePicked(uri: Uri?) {
        pendingPick?.resume(
            if (uri == null) {
                DocumentCaptureResult(message = "No document image selected.")
            } else {
                DocumentCaptureResult(
                    imageUri = uri.toString(),
                    message = "Document image attached. Review or extract document data before registering."
                )
            }
        )
        pendingPick = null
    }

    fun onPhotoCaptured(bitmap: Bitmap?) {
        pendingCapture?.resume(
            if (bitmap == null) {
                DocumentCaptureResult(message = "No document photo captured.")
            } else {
                val file = File(activity.cacheDir, "registration-document-${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                }
                DocumentCaptureResult(
                    imageUri = file.absolutePath,
                    message = "Document photo captured. Review or extract document data before registering."
                )
            }
        )
        pendingCapture = null
    }
}

private fun parseDocumentText(text: Text): DocumentExtractedData {
    val lines = text.textBlocks.flatMap { block -> block.lines.map { it.text.trim() } }
        .filter { it.isNotBlank() }
    val joined = lines.joinToString("\n")
    return DocumentExtractedData(
        fullName = fieldAfterLabel(lines, "name", "full name") ?: likelyName(lines),
        identifier = labeledIdentifier(joined) ?: Regex("\\b\\d{6,12}\\b").find(joined)?.value,
        birthDate = labeledDate(joined),
        sex = labeledSex(joined),
        guardianName = fieldAfterLabel(lines, "guardian", "mother", "father", "parent")
    )
}

private fun fieldAfterLabel(lines: List<String>, vararg labels: String): String? {
    lines.forEachIndexed { index, line ->
        val lower = line.lowercase()
        val matched = labels.firstOrNull { lower.contains(it) } ?: return@forEachIndexed
        Regex("(?i)${Regex.escape(matched)}\\s*[:#-]?\\s*(.+)")
            .find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.cleanExtractedValue()
            ?.takeIf { it.length >= 3 }
            ?.let { return it }
        lines.getOrNull(index + 1)?.cleanExtractedValue()?.takeIf { it.length >= 3 }?.let { return it }
    }
    return null
}

private fun likelyName(lines: List<String>): String? =
    lines.firstOrNull { line ->
        val upperWords = line.split(Regex("\\s+")).filter { it.length > 1 && it.all { ch -> ch.isLetter() } }
        upperWords.size >= 2 &&
            line == line.uppercase() &&
            !line.contains("REPUBLIC", ignoreCase = true) &&
            !line.contains("CERTIFICATE", ignoreCase = true) &&
            !line.contains("IDENTITY", ignoreCase = true)
    }?.cleanExtractedValue()

private fun labeledIdentifier(text: String): String? =
    Regex("(?i)(identity|id|certificate|birth|serial|number|no\\.?)[\\s:#-]+([A-Z0-9/-]{5,})")
        .find(text)
        ?.groupValues
        ?.getOrNull(2)
        ?.cleanExtractedValue()

private fun labeledDate(text: String): String? =
    Regex("(?i)(birth|born|dob|date of birth)[\\s:#-]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})")
        .find(text)
        ?.groupValues
        ?.getOrNull(2)
        ?.cleanExtractedValue()

private fun labeledSex(text: String): String? {
    val labeled = Regex("(?i)(sex|gender)[\\s:#-]+(male|female|m|f)").find(text)?.groupValues?.getOrNull(2)
    return when (labeled?.lowercase()) {
        "m", "male" -> "male"
        "f", "female" -> "female"
        else -> null
    }
}

private fun String.cleanExtractedValue(): String =
    trim().trim(':', '#', '-', ' ').replace(Regex("\\s+"), " ")

private fun DocumentExtractedData.hasAnyField(): Boolean =
    listOf(fullName, identifier, birthDate, sex, guardianName).any { !it.isNullOrBlank() }
