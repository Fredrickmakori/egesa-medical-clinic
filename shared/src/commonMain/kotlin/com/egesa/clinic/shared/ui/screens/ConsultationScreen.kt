package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
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
    var reportType by remember { mutableStateOf(ConsultationReportType.MedicalReport) }
    var reportDraft by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        patients = localRepository.getAllPatients()
        val allSchedules = localRepository.getAllSchedules()
        appointments = allSchedules.flatMap { localRepository.getAppointmentsBySchedule(it.id) }
            .sortedBy { it.startTime }
    }

    val selectedPatient = patients.firstOrNull { it.id == activePatientId }

    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DoctorQueue(
            modifier = Modifier.weight(1f),
            appointments = appointments,
            patients = patients,
            onStartConsultation = { appointment ->
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
        )

        ConsultationWorkArea(
            modifier = Modifier.weight(2f),
            selectedPatient = selectedPatient,
            activeEncounterId = activeEncounterId,
            activeTab = activeTab,
            onTabSelected = { activeTab = it },
            status = status,
            inputs = ConsultationInputs(
                chiefComplaint = chiefComplaint, onChiefComplaintChange = { chiefComplaint = it },
                hpi = hpi, onHpiChange = { hpi = it },
                examNotes = examNotes, onExamNotesChange = { examNotes = it },
                diagnosisText = diagnosisText, onDiagnosisTextChange = { diagnosisText = it },
                diagnosisCode = diagnosisCode, onDiagnosisCodeChange = { diagnosisCode = it },
                planAdvice = planAdvice, onPlanAdviceChange = { planAdvice = it },
                followUpDate = followUpDate, onFollowUpDateChange = { followUpDate = it },
                rxName = rxName, onRxNameChange = { rxName = it },
                rxDose = rxDose, onRxDoseChange = { rxDose = it },
                labOrderName = labOrderName, onLabOrderNameChange = { labOrderName = it },
                imagingStudy = imagingStudy, onImagingStudyChange = { imagingStudy = it },
                reportType = reportType, onReportTypeChange = { reportType = it },
                reportDraft = reportDraft, onReportDraftChange = { reportDraft = it },
                onGenerateReportDraft = {
                    reportDraft = buildAiAssistedConsultationReport(
                        reportType = reportType,
                        patient = selectedPatient,
                        session = session,
                        chiefComplaint = chiefComplaint,
                        hpi = hpi,
                        examNotes = examNotes,
                        diagnosisText = diagnosisText,
                        diagnosisCode = diagnosisCode,
                        planAdvice = planAdvice,
                        labOrderName = labOrderName,
                        imagingStudy = imagingStudy,
                        rxName = rxName,
                        rxDose = rxDose,
                    )
                }
            ),
            onSaveDraft = {
                scope.launch {
                    saveBundle(repository, activeEncounterId, chiefComplaint, hpi, examNotes, diagnosisText, diagnosisCode, planAdvice, followUpDate, labOrderName, imagingStudy, rxName, rxDose, finalize = false)
                    status = "Draft saved"
                }
            },
            onFinalize = {
                scope.launch {
                    saveBundle(repository, activeEncounterId, chiefComplaint, hpi, examNotes, diagnosisText, diagnosisCode, planAdvice, followUpDate, labOrderName, imagingStudy, rxName, rxDose, finalize = true)
                    status = "Consultation finalized"
                }
            }
        )
    }
}

@Composable
private fun DoctorQueue(
    modifier: Modifier = Modifier,
    appointments: List<Appointment>,
    patients: List<Patient>,
    onStartConsultation: (Appointment) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Text("Doctor queue / appointments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(appointments) { appointment ->
            val patient = patients.firstOrNull { it.id == appointment.patientId }
            QueueItem(patient = patient, appointment = appointment, onStartConsultation = onStartConsultation)
        }
    }
}

@Composable
private fun QueueItem(
    patient: Patient?,
    appointment: Appointment,
    onStartConsultation: (Appointment) -> Unit
) {
    Card(colors = CardDefaults.cardColors()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(patient?.fullName ?: appointment.patientId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${appointment.startTime} • ${appointment.appointmentType}", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onStartConsultation(appointment) }) {
                Text("Start Consultation")
            }
        }
    }
}

private data class ConsultationInputs(
    val chiefComplaint: String, val onChiefComplaintChange: (String) -> Unit,
    val hpi: String, val onHpiChange: (String) -> Unit,
    val examNotes: String, val onExamNotesChange: (String) -> Unit,
    val diagnosisText: String, val onDiagnosisTextChange: (String) -> Unit,
    val diagnosisCode: String, val onDiagnosisCodeChange: (String) -> Unit,
    val planAdvice: String, val onPlanAdviceChange: (String) -> Unit,
    val followUpDate: String, val onFollowUpDateChange: (String) -> Unit,
    val rxName: String, val onRxNameChange: (String) -> Unit,
    val rxDose: String, val onRxDoseChange: (String) -> Unit,
    val labOrderName: String, val onLabOrderNameChange: (String) -> Unit,
    val imagingStudy: String, val onImagingStudyChange: (String) -> Unit,
    val reportType: ConsultationReportType, val onReportTypeChange: (ConsultationReportType) -> Unit,
    val reportDraft: String, val onReportDraftChange: (String) -> Unit,
    val onGenerateReportDraft: () -> Unit,
)

private enum class ConsultationReportType(val label: String, val documentTitle: String) {
    MedicalReport("Medical report", "MEDICAL REPORT"),
    PharmacyReport("Pharmacy report", "PHARMACY REPORT"),
    PharmacyPrescription("Pharmacy prescription", "PHARMACY PRESCRIPTION"),
}

@Composable
private fun ConsultationWorkArea(
    modifier: Modifier = Modifier,
    selectedPatient: Patient?,
    activeEncounterId: String?,
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    status: String,
    inputs: ConsultationInputs,
    onSaveDraft: () -> Unit,
    onFinalize: () -> Unit
) {
    val tabs = listOf("History", "Examination", "Diagnosis", "Plan", "AI Reports")
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            selectedPatient?.let { "Consultation: ${it.fullName}" } ?: "Select appointment to start",
            style = MaterialTheme.typography.titleLarge
        )
        if (activeEncounterId != null) {
            TabRow(selectedTabIndex = activeTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = activeTab == index, onClick = { onTabSelected(index) }, text = { Text(title) })
                }
            }
            ConsultationTabContent(activeTab, inputs)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveDraft) { Text("Save Draft") }
                Button(onClick = onFinalize) { Text("Finalize") }
            }
        }
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ConsultationTabContent(activeTab: Int, inputs: ConsultationInputs) {
    when (activeTab) {
        0 -> HistoryTab(inputs)
        1 -> ExamTab(inputs)
        2 -> DiagnosisTab(inputs)
        3 -> PlanTab(inputs)
        4 -> AiReportsTab(inputs)
    }
}

@Composable
private fun HistoryTab(inputs: ConsultationInputs) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(inputs.chiefComplaint, inputs.onChiefComplaintChange, modifier = Modifier.fillMaxWidth(), label = { Text("Chief complaint") })
        OutlinedTextField(inputs.hpi, inputs.onHpiChange, modifier = Modifier.fillMaxWidth(), label = { Text("HPI") }, minLines = 3)
    }
}

@Composable
private fun ExamTab(inputs: ConsultationInputs) {
    OutlinedTextField(inputs.examNotes, inputs.onExamNotesChange, modifier = Modifier.fillMaxWidth(), label = { Text("Examination notes") }, minLines = 4)
}

@Composable
private fun DiagnosisTab(inputs: ConsultationInputs) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(inputs.diagnosisText, inputs.onDiagnosisTextChange, modifier = Modifier.fillMaxWidth(), label = { Text("Diagnosis") })
        OutlinedTextField(inputs.diagnosisCode, inputs.onDiagnosisCodeChange, modifier = Modifier.fillMaxWidth(), label = { Text("ICD code") })
    }
}

@Composable
private fun PlanTab(inputs: ConsultationInputs) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(inputs.planAdvice, inputs.onPlanAdviceChange, modifier = Modifier.fillMaxWidth(), label = { Text("Plan / advice") }, minLines = 3)
        OutlinedTextField(inputs.followUpDate, inputs.onFollowUpDateChange, modifier = Modifier.fillMaxWidth(), label = { Text("Follow-up date (ISO)") })
        CompactLabOrderDropdown(
            selected = inputs.labOrderName,
            onSelectedChange = inputs.onLabOrderNameChange,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(inputs.imagingStudy, inputs.onImagingStudyChange, modifier = Modifier.fillMaxWidth(), label = { Text("Imaging study") })
        OutlinedTextField(inputs.rxName, inputs.onRxNameChange, modifier = Modifier.fillMaxWidth(), label = { Text("Prescription") })
        OutlinedTextField(inputs.rxDose, inputs.onRxDoseChange, modifier = Modifier.fillMaxWidth(), label = { Text("Dose") })
    }
}


@Composable
private fun CompactLabOrderDropdown(
    selected: String,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        "Complete blood count (CBC)",
        "Malaria RDT / blood slide",
        "Urinalysis",
        "Random blood sugar",
        "HIV test",
        "Pregnancy test",
        "Liver function tests",
        "Renal function tests",
        "Other / type manually",
    )
    var expanded by remember { mutableStateOf(false) }
    val isCustom = selected.isNotBlank() && selected !in options

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.ifBlank { "Choose lab order" })
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onSelectedChange(if (option.startsWith("Other")) "" else option)
                        }
                    )
                }
            }
        }
        if (isCustom || selected.isBlank()) {
            OutlinedTextField(
                value = selected,
                onValueChange = onSelectedChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Other lab order / notes") },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun AiReportsTab(inputs: ConsultationInputs) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ConsultationReportTypeDropdown(
            selected = inputs.reportType,
            onSelectedChange = inputs.onReportTypeChange,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = inputs.onGenerateReportDraft) { Text("AI draft report") }
            OutlinedButton(onClick = inputs.onGenerateReportDraft) { Text("Refresh PDF preview") }
        }
        OutlinedTextField(
            value = inputs.reportDraft,
            onValueChange = inputs.onReportDraftChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            label = { Text("AI-assisted draft - clinician must review") },
            minLines = 6,
        )
        ConsultationPdfPreview(inputs.reportType, inputs.reportDraft)
    }
}

@Composable
private fun ConsultationReportTypeDropdown(
    selected: ConsultationReportType,
    onSelectedChange: (ConsultationReportType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Document type: ${selected.label}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ConsultationReportType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        expanded = false
                        onSelectedChange(type)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConsultationPdfPreview(reportType: ConsultationReportType, reportDraft: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Egesa Medical Clinic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Facility logo / letterhead", style = MaterialTheme.typography.bodySmall)
                    Text("Phone: +254 20 XXX XXXX | License: MCL-2024-001", style = MaterialTheme.typography.bodySmall)
                }
                Text("PDF PREVIEW", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            Divider()
            Text(reportType.documentTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                reportDraft.ifBlank { "Generate or type a report draft to preview it on the facility letterhead before exporting to PDF." },
                style = MaterialTheme.typography.bodyMedium,
            )
            Divider()
            Text("Clinician signature: ____________________", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun buildAiAssistedConsultationReport(
    reportType: ConsultationReportType,
    patient: Patient?,
    session: SessionState,
    chiefComplaint: String,
    hpi: String,
    examNotes: String,
    diagnosisText: String,
    diagnosisCode: String,
    planAdvice: String,
    labOrderName: String,
    imagingStudy: String,
    rxName: String,
    rxDose: String,
): String {
    val patientName = patient?.fullName ?: "Selected patient"
    val patientMeta = listOfNotNull(patient?.id, patient?.age?.let { "$it years" }, patient?.sex?.code).joinToString(" • ")
    return when (reportType) {
        ConsultationReportType.MedicalReport -> """
            Patient: $patientName ${if (patientMeta.isNotBlank()) "($patientMeta)" else ""}
            Clinician: ${session.fullName}

            Clinical summary:
            ${chiefComplaint.ifBlank { "Chief complaint not documented." }}
            ${hpi.ifBlank { "History of presenting illness pending." }}

            Examination:
            ${examNotes.ifBlank { "Examination notes pending." }}

            Assessment:
            ${diagnosisText.ifBlank { "Diagnosis pending." }}${diagnosisCode.ifBlank { "" }.let { if (it.isBlank()) "" else " ($it)" }}

            Plan:
            ${planAdvice.ifBlank { "Plan pending clinician completion." }}
        """.trimIndent()
        ConsultationReportType.PharmacyReport -> """
            Patient: $patientName
            Prescriber: ${session.fullName}

            Diagnosis / indication:
            ${diagnosisText.ifBlank { "Indication pending." }}

            Medication requested:
            ${rxName.ifBlank { "No medication entered." }} ${rxDose.ifBlank { "" }}

            Related orders:
            Lab: ${labOrderName.ifBlank { "None" }}
            Imaging: ${imagingStudy.ifBlank { "None" }}

            Pharmacy note:
            Review allergies, contraindications, stock availability, counselling needs, and substitution policy before dispensing.
        """.trimIndent()
        ConsultationReportType.PharmacyPrescription -> """
            Patient: $patientName
            Prescription generated by: ${session.fullName}

            Rx:
            ${rxName.ifBlank { "Medication name pending." }}
            Dose / frequency / duration:
            ${rxDose.ifBlank { "Dose instructions pending." }}

            Clinical notes:
            ${planAdvice.ifBlank { "Counselling and follow-up instructions pending." }}

            Dispensing instruction:
            Dispense as written unless pharmacist confirms an approved substitution with the prescriber.
        """.trimIndent()
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
