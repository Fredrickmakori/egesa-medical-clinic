package com.egesa.clinic.shared.domain

/**
 * Common interface for handling prescription print/export actions across platforms.
 * Platform-specific implementations (Android, iOS, Desktop, Web) should implement these callbacks.
 */
interface PrescriptionPrintActions {
    /**
     * Print the prescription to the system printer or print dialog.
     * @param htmlContent The HTML content of the prescription
     * @param prescriptionId The prescription ID for reference
     */
    suspend fun print(htmlContent: String, prescriptionId: String)

    /**
     * Download/save the prescription to the file system.
     * @param content The content to save
     * @param filename The desired filename (e.g., "prescription_RX-123.html")
     * @param format The format type ("html", "text", "markdown", "pdf")
     */
    suspend fun download(content: String, filename: String, format: String)

    /**
     * Share the prescription via email, messaging, or other sharing methods.
     * @param content The content to share
     * @param format The format to share in
     * @param patientName The patient name for context
     */
    suspend fun share(content: String, format: String, patientName: String)
}

/**
 * No-op implementation for when platform-specific implementations aren't available.
 */
class NoOpPrescriptionPrintActions : PrescriptionPrintActions {
    override suspend fun print(htmlContent: String, prescriptionId: String) {
        println("Print not implemented: $prescriptionId")
    }

    override suspend fun download(content: String, filename: String, format: String) {
        println("Download not implemented: $filename")
    }

    override suspend fun share(content: String, format: String, patientName: String) {
        println("Share not implemented for: $patientName")
    }
}

/**
 * Extension functions for converting prescription data to shareable formats.
 */

/**
 * Converts HTML prescription to a data URI for sharing or embedding.
 */
fun String.toHtmlDataUri(): String {
    val encoded = this.replace("\"", "&quot;").replace("\n", "")
    return "data:text/html;charset=utf-8,$encoded"
}

/**
 * Creates a filename for prescription exports with timestamp.
 */
fun prescriptionFilename(
    prescriptionId: String,
    format: String,
    timestamp: String? = null
): String {
    val ts = timestamp ?: ""
    val extension = when (format.lowercase()) {
        "html" -> "html"
        "text", "txt" -> "txt"
        "markdown", "md" -> "md"
        "pdf" -> "pdf"
        else -> "txt"
    }
    return "prescription_${prescriptionId}${if (ts.isNotBlank()) "_$ts" else ""}.$extension"
}

/**
 * Prepares prescription content for email transmission.
 * Converts HTML to a safe email format with text fallback.
 */
fun prepareForEmailShare(
    htmlContent: String,
    plainTextContent: String,
    patientName: String,
    prescriptionId: String
): EmailContent {
    return EmailContent(
        to = "",
        subject = "Prescription for $patientName ($prescriptionId)",
        body = plainTextContent,
        htmlBody = htmlContent,
        attachmentName = "prescription_$prescriptionId.html"
    )
}

data class EmailContent(
    val to: String,
    val subject: String,
    val body: String,
    val htmlBody: String,
    val attachmentName: String
)

/**
 * Formats prescription for WhatsApp or SMS sharing (plain text only).
 */
fun prepareForMessagingShare(
    plainTextContent: String,
    maxLength: Int = 500
): String {
    // WhatsApp/SMS has character limits
    val lines = plainTextContent.lines()
    val shortened = StringBuilder()
    var current = 0

    for (line in lines) {
        val newLength = current + line.length + 1
        if (newLength <= maxLength) {
            shortened.appendLine(line)
            current = newLength
        } else {
            break
        }
    }

    if (current < plainTextContent.length) {
        shortened.append("\n[... Full prescription available via email/download]")
    }

    return shortened.toString()
}

