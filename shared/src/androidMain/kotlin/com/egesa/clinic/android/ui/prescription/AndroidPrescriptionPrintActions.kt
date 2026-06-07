package com.egesa.clinic.android.ui.prescription

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.egesa.clinic.shared.domain.PrescriptionPrintActions
import java.io.File
import java.io.FileWriter
import kotlin.coroutines.resume
import kotlin.coroutines. suspendCancellableCoroutine

/**
 * Android implementation of prescription print/export functionality.
 * Handles printing via system print dialog and downloading files to device storage.
 */
class AndroidPrescriptionPrintActions(private val context: Context) : PrescriptionPrintActions {

    override suspend fun print(htmlContent: String, prescriptionId: String) {
        suspendCancellableCoroutine { continuation ->
            try {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

                // Create a WebView to render the HTML for printing
                val webView = WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            // Print once page is loaded
                            printManager.print(
                                "Prescription_$prescriptionId",
                                webView.createPrintDocumentAdapter("prescription"),
                                PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                    .build()
                            )
                            continuation.resume(Unit)
                        }
                    }
                }

                // Load HTML into WebView
                webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            } catch (e: Exception) {
                continuation.resume(Unit) // Continue despite errors
                e.printStackTrace()
            }
        }
    }

    override suspend fun download(content: String, filename: String, format: String) {
        try {
            // Get Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Create file
            val file = File(downloadsDir, filename)

            // Write content
            FileWriter(file).use { writer ->
                writer.write(content)
            }

            // Show notification (optional - can be enhanced with NotificationCompat)
            android.widget.Toast.makeText(
                context,
                "Prescription saved to Downloads: $filename",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(
                context,
                "Failed to save prescription: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override suspend fun share(content: String, format: String, patientName: String) {
        try {
            // Create temporary file
            val cacheDir = context.cacheDir
            val tempFile = File(
                cacheDir,
                "prescription_${System.currentTimeMillis()}.${getExtension(format)}"
            )

            // Write content to temp file
            FileWriter(tempFile).use { writer ->
                writer.write(content)
            }

            // Get content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.prescription.provider",
                tempFile
            )

            // Create share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "Prescription for $patientName")
                putExtra(Intent.EXTRA_TEXT, "Please see attached prescription")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = getMimeType(format)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Launch share chooser
            val chooser = Intent.createChooser(shareIntent, "Share prescription with:")
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(
                context,
                "Failed to share prescription: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getExtension(format: String): String {
        return when (format.lowercase()) {
            "html" -> "html"
            "text", "txt" -> "txt"
            "markdown", "md" -> "md"
            "pdf" -> "pdf"
            else -> "txt"
        }
    }

    private fun getMimeType(format: String): String {
        return when (format.lowercase()) {
            "html" -> "text/html"
            "text", "txt" -> "text/plain"
            "markdown", "md" -> "text/markdown"
            "pdf" -> "application/pdf"
            else -> "text/plain"
        }
    }
}

