package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.Appointment
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.data.EncounterRepository
import com.egesa.clinic.shared.data.LocalEncounterRepository
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.domain.EncounterDiagnosis
import com.egesa.clinic.shared.domain.EncounterExam
import com.egesa.clinic.shared.domain.EncounterHistory
import com.egesa.clinic.shared.domain.EncounterPlan
import com.egesa.clinic.shared.domain.EncounterSourceType
import com.egesa.clinic.shared.domain.ImagingOrder
import com.egesa.clinic.shared.domain.OpdEncounterBundle
import com.egesa.clinic.shared.domain.Prescription
import com.egesa.clinic.shared.ui.navigation.SessionState
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun ConsultationScreen(localRepository: LocalRepository, session: SessionState) {
    val repository = remember(localRepository) { LocalEncounterRepository(localRepository.database) }
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var activePatientId by remember { mutableStateOf<String?>(null) }
    var activeEncounterId by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var chiefComplaint by remember { mutableStateOf("") }
    var hpi by remember { mutableStateOf("") }
    var examNotes by remember { mutableStateOf("") }
    var diagnosisText by remember { mutableStateOf("") }
    var diagnosisCode by remember { mutableStateOf("") }
    var planAdvice by remember { mutableStateOf("") }
    var followUpDate by remember { mutableStateOf("") }
    var rxName by remember { mutableStateOf("") }
    var rxDose by remember { mutableStateOf("") }
    var labOrderName by remember { mutableStateOf("") }
    var imagingStudy by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        patients = localRepository.getAllPatients()
        val allSchedules = localRepository.getAllSchedules()
        appointments = allSchedules.flatMap { localRepository.getAppointmentsBySchedule(it.id) }
            .sortedBy { it.startTime }
    }

    val selectedPatient = patients.firstOrNull { it.id == activePatientId }
    val tabs = listOf("History", "Examination", "Diagnosis", "Plan")

    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Text("Doctor queue / appointments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(appointments) { appointment ->
                val patient = patients.firstOrNull { it.id == appointment.patientId }
                Card(colors = CardDefaults.cardColors()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(patient?.fullName ?: appointment.patientId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${appointment.startTime} • ${appointment.appointmentType}", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                scope.launch {
                                    val encounter = repository.createEncounter(
                                        patientId = appointment.patientId,
                                        providerId = session.staffId,
                                        facilityId = "EGESA-CLINIC",
                                        sourceType = EncounterSourceType.APPOINTMENT,
                                        sourceId = appointment.id,
                                    )
                                    activePatientId = appointment.patientId
                                    activeEncounterId = encounter.encounterId
                                    status = "Consultation started: ${encounter.encounterId}"
                                }
                            }
                        ) { Text("Start Consultation") }
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize().weight(2f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                selectedPatient?.let { "Consultation: ${it.fullName}" } ?: "Select appointment to start",
                style = MaterialTheme.typography.titleLarge
            )
            if (activeEncounterId != null) {
                TabRow(selectedTabIndex = activeTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = activeTab == index, onClick = { activeTab = index }, text = { Text(title) })
                    }
                }
                when (activeTab) {
                    0 -> {
                        OutlinedTextField(chiefComplaint, { chiefComplaint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Chief complaint") })
                        OutlinedTextField(hpi, { hpi = it }, modifier = Modifier.fillMaxWidth(), label = { Text("HPI") }, minLines = 3)
                    }
                    1 -> {
                        OutlinedTextField(examNotes, { examNotes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Examination notes") }, minLines = 4)
                    }
                    2 -> {
                        OutlinedTextField(diagnosisText, { diagnosisText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Diagnosis") })
                        OutlinedTextField(diagnosisCode, { diagnosisCode = it }, modifier = Modifier.fillMaxWidth(), label = { Text("ICD code") })
                    }
                    3 -> {
                        OutlinedTextField(planAdvice, { planAdvice = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Plan / advice") }, minLines = 3)
                        OutlinedTextField(followUpDate, { followUpDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Follow-up date (ISO)") })
                        OutlinedTextField(labOrderName, { labOrderName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Lab order") })
                        OutlinedTextField(imagingStudy, { imagingStudy = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Imaging study") })
                        OutlinedTextField(rxName, { rxName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Prescription") })
                        OutlinedTextField(rxDose, { rxDose = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Dose") })
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            saveBundle(repository, activeEncounterId, chiefComplaint, hpi, examNotes, diagnosisText, diagnosisCode, planAdvice, followUpDate, labOrderName, imagingStudy, rxName, rxDose, finalize = false)
                            status = "Draft saved"
                        }
                    }) { Text("Save Draft") }
                    Button(onClick = {
                        scope.launch {
                            saveBundle(repository, activeEncounterId, chiefComplaint, hpi, examNotes, diagnosisText, diagnosisCode, planAdvice, followUpDate, labOrderName, imagingStudy, rxName, rxDose, finalize = true)
                            status = "Consultation finalized"
                        }
                    }) { Text("Finalize") }
                }
            }
            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private suspend fun saveBundle(
    repository: EncounterRepository,
    encounterId: String?,
    chiefComplaint: String,
    hpi: String,
    examNotes: String,
    diagnosisText: String,
    diagnosisCode: String,
    planAdvice: String,
    followUpDate: String,
    labOrderName: String,
    imagingStudy: String,
    rxName: String,
    rxDose: String,
    finalize: Boolean,
) {
    val id = encounterId ?: return
    val now = Clock.System.now().toString()
    val existing = repository.getEncounterById(id) ?: return
    val bundle = existing.copy(
        history = EncounterHistory(encounterId = id, chiefComplaint = chiefComplaint, hpi = hpi),
        exam = EncounterExam(encounterId = id, systemExamNotes = examNotes),
        diagnoses = diagnosisText.takeIf { it.isNotBlank() }?.let {
            listOf(
                EncounterDiagnosis(
                    diagnosisId = "DX-${Clock.System.now().toEpochMilliseconds()}",
                    encounterId = id,
                    diagnosisText = diagnosisText,
                    diagnosisCode = diagnosisCode.ifBlank { null },
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } ?: emptyList(),
        plan = EncounterPlan(encounterId = id, clinicalAdvice = planAdvice, followUpDate = followUpDate.ifBlank { null }),
        labOrders = labOrderName.takeIf { it.isNotBlank() }?.let {
            listOf(
                com.egesa.clinic.shared.domain.LabOrder(
                    id = "LAB-${Clock.System.now().toEpochMilliseconds()}",
                    patientId = existing.encounter.patientId,
                    encounterId = id,
                    orderedBy = existing.encounter.providerId ?: "system",
                    department = existing.encounter.department,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } ?: emptyList(),
        imagingOrders = imagingStudy.takeIf { it.isNotBlank() }?.let {
            listOf(
                ImagingOrder(
                    orderId = "IMG-${Clock.System.now().toEpochMilliseconds()}",
                    encounterId = id,
                    studyName = imagingStudy,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } ?: emptyList(),
        prescriptions = rxName.takeIf { it.isNotBlank() }?.let {
            listOf(
                Prescription(
                    prescriptionId = "RX-${Clock.System.now().toEpochMilliseconds()}",
                    encounterId = id,
                    medicationName = rxName,
                    dose = rxDose.ifBlank { null },
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } ?: emptyList(),
    )
    repository.updateEncounter(bundle, finalize = finalize)
}
