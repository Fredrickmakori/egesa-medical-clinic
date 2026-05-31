package com.egesa.clinic.shared.data

data class DocumentExtractedData(
    val fullName: String? = null,
    val identifier: String? = null,
    val birthDate: String? = null,
    val sex: String? = null,
    val guardianName: String? = null
)

data class DocumentCaptureResult(
    val imageUri: String? = null,
    val extractedData: DocumentExtractedData = DocumentExtractedData(),
    val message: String? = null
)

interface DocumentCaptureGateway {
    val canCapturePhoto: Boolean
    val canAttachImage: Boolean

    suspend fun capturePhoto(documentType: String): DocumentCaptureResult
    suspend fun attachImage(documentType: String): DocumentCaptureResult
    suspend fun extractData(imageUri: String, documentType: String): DocumentCaptureResult
}

object NoopDocumentCaptureGateway : DocumentCaptureGateway {
    override val canCapturePhoto: Boolean = false
    override val canAttachImage: Boolean = false

    override suspend fun capturePhoto(documentType: String): DocumentCaptureResult =
        DocumentCaptureResult(message = "Document camera capture is not configured for this platform.")

    override suspend fun attachImage(documentType: String): DocumentCaptureResult =
        DocumentCaptureResult(message = "Document image attachment is not configured for this platform.")

    override suspend fun extractData(imageUri: String, documentType: String): DocumentCaptureResult =
        DocumentCaptureResult(imageUri = imageUri, message = "OCR extraction is not configured. Enter extracted fields manually.")
}
