package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.domain.FacilityPrintInfo
import com.egesa.clinic.shared.domain.Prescription
import com.egesa.clinic.shared.domain.PrescriptionPrintStatus
import com.egesa.clinic.shared.domain.PatientPrintInfo
import com.egesa.clinic.shared.domain.ProviderPrintInfo
import com.egesa.clinic.shared.domain.renderPrescriptionToHtml
import com.egesa.clinic.shared.domain.renderPrescriptionToPlainText
import com.egesa.clinic.shared.domain.renderPrescriptionToMarkdown
import com.egesa.clinic.shared.domain.toPrintModel

/**
 * Enhanced prescription print preview screen with full rendering, export, and print capabilities.
 * Supports HTML, plain text, and markdown formats.
 * Allows marking prescriptions for external pharmacy purchase.
 */
@Composable
fun PrescriptionPrintPreviewScreen(
    prescription: Prescription,
    patients: List<Patient>,
    onDismiss: () -> Unit,
    onPrint: ((htmlContent: String, prescriptionId: String) -> Unit)? = null,
    onDownload: ((content: String, filename: String, format: String) -> Unit)? = null,
    onExternalPurchase: ((Prescription) -> Unit)? = null
) {
    val patient = patients.firstOrNull { it.id == prescription.encounterId }
    var showFormat by remember { mutableStateOf(0) } // 0=HTML preview, 1=Plain Text, 2=Markdown
    var isExternalPurchase by remember { mutableStateOf(false) }
    var showExternalPurchaseWarning by remember { mutableStateOf(false) }

    // Build facility info (in real app, fetch from session/repository)
    val facilityInfo = FacilityPrintInfo(
        facilityName = "Egesa Medical Clinic",
        address = "123 Health Street, Medical District",
        phoneNumber = "+254 20 XXX XXXX",
        licenseNumber = "MCL-2024-001"
    )

    // Build provider info (in real app, get from encounter/session)
    val providerInfo = ProviderPrintInfo(
        providerName = "Dr. Jane Smith",
        specialty = "General Practice",
        registrationNumber = "REG-2023-4567"
    )

    // Build patient info
    val patientInfo = PatientPrintInfo(
        patientName = patient?.fullName ?: "Unknown Patient",
        age = patient?.age,
        sex = patient?.sex?.code ?: "Unknown",
        patientId = patient?.id
    )

    // Generate print model with external purchase flag
    val printModel = prescription.toPrintModel(
        encounterId = prescription.encounterId,
        facilityInfo = facilityInfo,
        providerInfo = providerInfo,
        patientInfo = patientInfo,
        diagnosis = null, // Can be passed from encounter if available
        externalPurchase = isExternalPurchase
    )

    // Render in all formats
    val htmlContent = renderPrescriptionToHtml(printModel)
    val plainTextContent = renderPrescriptionToPlainText(printModel)
    val markdownContent = renderPrescriptionToMarkdown(printModel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            "Prescription Print Preview",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // External Purchase Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isExternalPurchase)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "For External Pharmacy",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Patient will buy medications outside this facility",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = isExternalPurchase,
                    onCheckedChange = {
                        isExternalPurchase = it
                        if (it) showExternalPurchaseWarning = true
                    }
                )
            }
        }

        // Format tabs
        TabRow(selectedTabIndex = showFormat) {
            Tab(
                selected = showFormat == 0,
                onClick = { showFormat = 0 },
                text = { Text("HTML Preview") }
            )
            Tab(
                selected = showFormat == 1,
                onClick = { showFormat = 1 },
                text = { Text("Text") }
            )
            Tab(
                selected = showFormat == 2,
                onClick = { showFormat = 2 },
                text = { Text("Markdown") }
            )
        }

        // Content area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (showFormat) {
                    0 -> {
                        // HTML Preview - show formatted text representation
                        HtmlPreviewContent(htmlContent)
                    }
                    1 -> {
                        // Plain Text
                        Text(
                            plainTextContent,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    2 -> {
                        // Markdown
                        Text(
                            markdownContent,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }
                Button(
                    onClick = {
                        onPrint?.invoke(htmlContent, prescription.prescriptionId)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = onPrint != null
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Print")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDownload?.invoke(
                            htmlContent,
                            "prescription_${prescription.prescriptionId}.html",
                            "html"
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = onDownload != null
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("HTML")
                }
                OutlinedButton(
                    onClick = {
                        onDownload?.invoke(
                            plainTextContent,
                            "prescription_${prescription.prescriptionId}.txt",
                            "text"
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = onDownload != null
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Text")
                }
                OutlinedButton(
                    onClick = {
                        onDownload?.invoke(
                            markdownContent,
                            "prescription_${prescription.prescriptionId}.md",
                            "markdown"
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = onDownload != null
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("MD")
                }
            }

            // Mark as external purchase button
            if (isExternalPurchase) {
                Button(
                    onClick = {
                        onExternalPurchase?.invoke(prescription)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = onExternalPurchase != null
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Confirm External Purchase")
                }
            }
        }
    }
}

/**
 * Displays a formatted preview of the HTML prescription.
 * In a real-world scenario, this could use a WebView or HTML rendering engine.
 * For now, we show a text representation with key sections highlighted.
 */
@Composable
private fun HtmlPreviewContent(htmlContent: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "HTML Prescription Ready for Printing",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "This prescription can be printed on paper or exported as a PDF. " +
            "It includes facility information, patient details, medications, dosing, " +
            "and provider signature blocks.",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Key Sections Included:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "✓ Facility header with contact info\n" +
            "✓ Patient demographic information\n" +
            "✓ Prescription metadata (ID, date, status)\n" +
            "✓ Medications table (name, strength, dose, route, frequency, duration)\n" +
            "✓ Patient instructions per medication\n" +
            "✓ Provider signature block\n" +
            "✓ Legal disclaimers and usage instructions\n" +
            "✓ Professional styling for formal use",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Use one of the download buttons to save the prescription in your preferred format.",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontSize = 10.sp
        )
    }
}
