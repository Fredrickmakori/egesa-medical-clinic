package com.egesa.clinic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.egesa.clinic.shared.EncounterForm
import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.SaveState
import com.egesa.clinic.shared.TimelineEvent
import com.egesa.clinic.shared.WorkflowArea

fun main() = application {
    val state = remember { HospitalState() }
    Window(onCloseRequest = ::exitApplication, title = "Hospital Manager") {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                HospitalDashboard(state)
            }
        }
    }
}

@Composable
private fun HospitalDashboard(state: HospitalState) {
    var activeArea by remember { mutableStateOf(WorkflowArea.RECEPTION) }
    val areas = WorkflowArea.entries

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Egesa Hospital Management", style = MaterialTheme.typography.headlineSmall)

        TabRow(selectedTabIndex = areas.indexOf(activeArea)) {
            areas.forEach { area ->
                Tab(
                    selected = area == activeArea,
                    onClick = { activeArea = area },
                    text = { Text(area.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        if (activeArea == WorkflowArea.CONSULTATION) {
            ConsultationWorkbench(state)
        } else {
            DefaultAreaView(state, activeArea)
        }
    }
}

@Composable
private fun DefaultAreaView(state: HospitalState, activeArea: WorkflowArea) {
    if (activeArea == WorkflowArea.ADMIN) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            state.metrics().forEach { metric ->
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(metric.title)
                        Text(metric.value, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.allPatients()) { patient ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${patient.id} • ${patient.fullName}")
                    Text("${patient.age} yrs, ${patient.sex}")
                    Text("Status: ${patient.status}")
                    patient.assignedWard?.let { Text("Ward: $it") }
                }
            }
        }
    }
}

@Composable
private fun ConsultationWorkbench(state: HospitalState) {
    val patients = state.allPatients()
    var filterQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var selectedPatient by remember { mutableStateOf(patients.first()) }

    var form by remember { mutableStateOf(EncounterForm()) }
    var saveState by remember { mutableStateOf(SaveState.DRAFT_SAVED) }

    val filteredPatients = patients.filter {
        val queryMatches = filterQuery.isBlank() || it.fullName.contains(filterQuery, true) || it.id.contains(filterQuery, true)
        val statusMatches = statusFilter == "All" || it.status.contains(statusFilter, true)
        queryMatches && statusMatches
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.weight(1.1f).fillMaxSize()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Patients & Filters", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(filterQuery, { filterQuery = it }, label = { Text("Search patient") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "consultation", "diagnosis", "Admitted").forEach { status ->
                        Button(onClick = { statusFilter = status }) { Text(status) }
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredPatients) { patient ->
                        Card(
                            modifier = Modifier.fillMaxWidth().background(
                                if (patient.id == selectedPatient.id) Color(0xFFE8F5E9) else Color.Transparent
                            )
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${patient.id} • ${patient.fullName}")
                                Text(patient.status)
                                Button(onClick = { selectedPatient = patient }) { Text("Open") }
                            }
                        }
                    }
                }
            }
        }

        Card(Modifier.weight(1.6f).fillMaxSize()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Encounter Form", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(form.chiefComplaint, {
                    form = form.copy(chiefComplaint = it)
                    saveState = SaveState.UNSAVED_CHANGES
                }, label = { Text("Chief complaint") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(form.history, {
                    form = form.copy(history = it)
                    saveState = SaveState.UNSAVED_CHANGES
                }, label = { Text("History") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(form.examinationFindings, {
                    form = form.copy(examinationFindings = it)
                    saveState = SaveState.UNSAVED_CHANGES
                }, label = { Text("Examination findings") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(form.provisionalDiagnosis, {
                    form = form.copy(provisionalDiagnosis = it)
                    saveState = SaveState.UNSAVED_CHANGES
                }, label = { Text("Provisional diagnosis") }, modifier = Modifier.fillMaxWidth())

                Text("Quick Actions", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Order Labs", "Request Imaging", "Prescribe Meds", "Refer", "Admit/Discharge").forEach { action ->
                        Button(onClick = { saveState = SaveState.UNSAVED_CHANGES }, modifier = Modifier.widthIn(min = 140.dp)) {
                            Text(action)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { saveState = SaveState.DRAFT_SAVED }) { Text("Save Draft") }
                    Button(onClick = { saveState = SaveState.FINAL_SIGN_OFF }) { Text("Final Sign-off") }
                }

                val saveMessage = when (saveState) {
                    SaveState.DRAFT_SAVED -> "Draft saved"
                    SaveState.UNSAVED_CHANGES -> "Unsaved changes warning"
                    SaveState.FINAL_SIGN_OFF -> "Final sign-off completed"
                }
                Text("Status: $saveMessage")
            }
        }

        Card(Modifier.weight(1.3f).fillMaxSize()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Patient Summary", style = MaterialTheme.typography.titleMedium)
                Text("${selectedPatient.fullName} (${selectedPatient.id})")
                Text("${selectedPatient.age} yrs • ${selectedPatient.sex}")
                Text("Visits: ${selectedPatient.visits}")
                Text("Active diagnosis: ${selectedPatient.activeDiagnosis}")
                Text("Current meds: ${selectedPatient.currentMedications.joinToString()}")
                selectedPatient.assignedWard?.let { Text("Ward: $it") }

                Text("Clinical Timeline", style = MaterialTheme.typography.titleSmall)
                ClinicalTimeline(selectedPatient.timeline)
            }
        }
    }
}

@Composable
private fun ClinicalTimeline(events: List<TimelineEvent>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().height(420.dp)) {
        items(events) { event ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(event.title)
                    Text(event.details)
                    Text("${event.type.name.lowercase()} • ${event.timestamp}")
                }
            }
        }
    }
}
