package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ClinicalProgramsScreen() {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("OPD", "ANC", "Maternity", "CCC", "NCD")
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Clinical Registers", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, t ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(t) })
            }
        }
        Spacer(Modifier.height(8.dp))
        when (tab) {
            0 -> GenericDepartmentForm("OPD Encounter", "Diagnosis", "Treatment")
            1 -> GenericDepartmentForm("ANC Visit", "Gestational Age", "IFA / IPTp")
            2 -> GenericDepartmentForm("Delivery Event", "Mode of Delivery", "Maternal Outcome")
            3 -> GenericDepartmentForm("CCC Follow-up", "Current Regimen", "Viral Load")
            4 -> GenericDepartmentForm("NCD Follow-up", "BP / Glucose", "Medication Refill")
        }
    }
}

@Composable
private fun GenericDepartmentForm(title: String, field1Label: String, field2Label: String) {
    var patientId by remember { mutableStateOf("") }
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var syncMode by remember { mutableStateOf("Offline-first: save locally, sync automatically when online") }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = patientId, onValueChange = { patientId = it }, label = { Text("Patient ID") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text(field1Label) }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text(field2Label) }, modifier = Modifier.weight(1f))
                }
                Text(syncMode, style = MaterialTheme.typography.bodySmall)
                Button(onClick = { syncMode = "Saved locally (PENDING_SYNC). Auto-sync in background when online." }) {
                    Text("Save")
                }
            }
        }
    }
}
