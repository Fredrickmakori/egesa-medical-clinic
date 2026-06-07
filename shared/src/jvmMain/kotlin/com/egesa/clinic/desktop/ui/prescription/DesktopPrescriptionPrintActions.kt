package com.egesa.clinic.desktop.ui.prescription

import com.egesa.clinic.shared.domain.PrescriptionPrintActions
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Desktop/JVM implementation of prescription print/export functionality.
 * Handles printing via system print command and file downloads to standard directories.
 */
class DesktopPrescriptionPrintActions : PrescriptionPrintActions {

    override suspend fun print(htmlContent: String, prescriptionId: String) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Create a temporary HTML file
                val tempFile = File.createTempFile(
                    "prescription_$prescriptionId",
                    ".html"
                ).apply {
                    writeText(htmlContent)
                    deleteOnExit()
                }

                // Open with default browser/printer
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.PRINT)) {
                    Desktop.getDesktop().print(tempFile)
                    println("Printing: $tempFile")
                } else {
                    // Fallback: open in default browser
                    Desktop.getDesktop().browse(tempFile.toURI())
                    println("Opening prescription in browser for printing: $tempFile")
                }

                continuation.resume(Unit)
            } catch (e: Exception) {
                println("Print failed: ${e.message}")
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun download(content: String, filename: String, format: String) {
        try {
            // Use standard Downloads directory
            val downloadsDir = when {
                System.getProperty("os.name").contains("Windows", ignoreCase = true) -> {
                    Paths.get(System.getProperty("user.home"), "Downloads")
                }
                System.getProperty("os.name").contains("Mac", ignoreCase = true) -> {
                    Paths.get(System.getProperty("user.home"), "Downloads")
                }
                else -> { // Linux and others
                    Paths.get(System.getProperty("user.home"), "Downloads")
                }
            }

            // Create Downloads directory if it doesn't exist
            Files.createDirectories(downloadsDir)

            val filePath = downloadsDir.resolve(filename)
            Files.write(
                filePath,
                content.toByteArray(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )

            println("Prescription saved: $filePath")

            // Open containing folder (platform-specific)
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(downloadsDir.toFile())
            }
        } catch (e: Exception) {
            println("Download failed: ${e.message}")
            e.printStackTrace()
        }
    }

    override suspend fun share(content: String, format: String, patientName: String) {
        try {
            // Create a temporary file
            val tempFile = File.createTempFile(
                "prescription_",
                ".${getExtension(format)}"
            ).apply {
                writeText(content)
            }

            println("Prescription ready to share: $tempFile")

            // Open with default application
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile)
            }

            // Note: For email, users would need to manually send or use platform-specific
            // email client integration
            println("To email: Open the file manually and attach to your email client")
        } catch (e: Exception) {
            println("Share failed: ${e.message}")
            e.printStackTrace()
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
}

