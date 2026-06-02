package com.egesa.clinic.shared.ui.screens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.domain.FacilityPrintInfo
import com.egesa.clinic.shared.domain.Prescription
import com.egesa.clinic.shared.domain.PatientPrintInfo
import com.egesa.clinic.shared.domain.ProviderPrintInfo
import com.egesa.clinic.shared.domain.renderPrescriptionToHtml
import com.egesa.clinic.shared.domain.renderPrescriptionToPlainText
import com.egesa.clinic.shared.domain.toPrintModel
@Composable
fun PrescriptionPrintPreviewScreen(
    prescription: Prescription,
    patients: List<Patient>,
    onDismiss: () -> Unit
) {
    val patient = patients.firstOrNull { it.id == prescription.encounterId }
    var showFormat by remember { mutableStateOf(0) }
    val facilityInfo = FacilityPrintInfo(
        facilityName = "Egesa Medical Clinic",
        address = "123 Health Street, Medical District",
        phoneNumber = "+254 20 XXX XXXX",
        licenseNumber = "MCL-2024-001"
    )
    val providerInfo = ProviderPrintInfo(
        providerName = "Dr. Jane Smith",
        specialty = "General Practice",
        registrationNumber = "REG-2023-4567"
    )
    val patientInfo = PatientPrintInfo(
        patientName = patient?.fullName ?: "Unknown Patient",
        age = patient?.age,
        sex = patient?.sex?.code ?: "Unknown",
        patientId = patient?.id
    )
    val printModel = prescription.toPrintModel(
        encounterId = prescription.encounterId,
        facilityInfo = facilityInfo,
        providerInfo = providerInfo,
        patientInfo = patientInfo,
        externalPurchase = prescription.isExternalPurchase
    )
    val plainTextContent = renderPrescriptionToPlainText(printModel)
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Prescription Print Preview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        TabRow(showFormat) {
            Tab(selected = showFormat == 0, onClick = { showFormat = 0 }, text = { Text("HTML") })
            Tab(selected = showFormat == 1, onClick = { showFormat = 1 }, text = { Text("Text") })
        }
        Card(
            modifier = Modifier.fillMaxSize().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (showFormat == 0) {
                    item {
                        Text("HTML Prescription Ready for Printing", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    item {
                        Text(plainTextContent, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(modifier = Modifier.weight(1f), onClick = {}) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Print")
            }
        }
    }
}
