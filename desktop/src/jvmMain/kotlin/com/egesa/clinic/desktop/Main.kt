package com.egesa.clinic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.Patient
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

        when (activeArea) {
            WorkflowArea.RECEPTION -> ReceptionWorkspace(state)
            WorkflowArea.ADMIN -> AdminMetrics(state)
            else -> PatientList(state.allPatients(), "No patients available for this workflow area.")
        }
    }
}

@Composable
private fun ReceptionWorkspace(state: HospitalState) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1.25f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QueueSummaryCards()
            QueueBoard(state.allPatients())
            QueueActionsPanel()
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NewPatientRegistrationForm()
            QuickTriage()
        }
    }
}

@Composable
private fun QueueSummaryCards() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard("Waiting", "12", Color(0xFF1E88E5), Modifier.weight(1f))
        SummaryCard("Priority", "3", Color(0xFFFB8C00), Modifier.weight(1f))
        SummaryCard("Avg Wait", "18 min", Color(0xFF43A047), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Box(
                Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(value, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QueueBoard(patients: List<Patient>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Live Patient Queue", style = MaterialTheme.typography.titleMedium)
            QueueTableHeader()

            when {
                patients.isEmpty() -> EmptyState("No patients in queue.")
                patients.any { it.fullName.contains("Error", true) } -> ErrorState("Queue feed unavailable. Retry in 30 seconds.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(patients) { patient ->
                        QueueRow(patient)
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTableHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF3F6FA), RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text("Token", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text("Patient", Modifier.weight(2f), fontWeight = FontWeight.SemiBold)
        Text("Status", Modifier.weight(2f), fontWeight = FontWeight.SemiBold)
        Text("Priority", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QueueRow(patient: Patient) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(patient.id, Modifier.weight(1f))
        Text(patient.fullName, Modifier.weight(2f))
        Text(patient.status, Modifier.weight(2f))
        AssistChip(onClick = {}, label = { Text(if (patient.age > 55) "High" else "Normal") }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QueueActionsPanel() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Queue Actions", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Assign Doctor") }
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Send to Consultation") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Escalate Priority") }
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Print Token") }
            }
        }
    }
}

@Composable
private fun NewPatientRegistrationForm() {
    var fullName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var insuranceProvider by remember { mutableStateOf("") }
    var consentAccepted by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("New Patient Registration", style = MaterialTheme.typography.titleMedium)

            SectionLabel("Demographics")
            RequiredField("Full Name *", fullName, { fullName = it }, submitAttempted)
            RequiredField("Date of Birth *", dateOfBirth, { dateOfBirth = it }, submitAttempted)
            OutlinedTextField(value = "Female", onValueChange = {}, label = { Text("Sex") }, modifier = Modifier.fillMaxWidth())

            SectionLabel("Contact")
            RequiredField("Primary Phone *", phone, { phone = it }, submitAttempted)
            OutlinedTextField(value = "Kakamega", onValueChange = {}, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())

            SectionLabel("Emergency Contact")
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Contact Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Relationship") }, modifier = Modifier.fillMaxWidth())

            SectionLabel("Insurance / Payment")
            RequiredField("Insurance Provider *", insuranceProvider, { insuranceProvider = it }, submitAttempted)
            OutlinedTextField(value = "Cash", onValueChange = {}, label = { Text("Preferred Payment Method") }, modifier = Modifier.fillMaxWidth())

            SectionLabel("Consent")
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = consentAccepted, onClick = { consentAccepted = true })
                Text("Consent granted for treatment and data use")
            }
            if (submitAttempted && !consentAccepted) {
                Text("Consent is required.", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { submitAttempted = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Register Patient")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun RequiredField(label: String, value: String, onChange: (String) -> Unit, attempted: Boolean) {
    val isError = attempted && value.isBlank()
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )
        if (isError) {
            Text("This field is required.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun QuickTriage() {
    var vitals by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Routine") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Quick Triage", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = vitals,
                onValueChange = { vitals = it },
                label = { Text("Vitals (BP, Temp, Pulse)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Routine", "Urgent", "Critical").forEach { level ->
                    AssistChip(onClick = { urgency = level }, label = { Text(level) })
                }
            }
            Text("Selected urgency: $urgency", style = MaterialTheme.typography.labelMedium)

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Triage Notes") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray)
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun AdminMetrics(state: HospitalState) {
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
    PatientList(state.allPatients(), "No admin records found.")
}

@Composable
private fun PatientList(patients: List<Patient>, emptyMessage: String) {
    if (patients.isEmpty()) {
        EmptyState(emptyMessage)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(patients) { patient ->
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
