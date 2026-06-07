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
import com.egesa.clinic.shared.Disposition
import com.egesa.clinic.shared.HivStatus
import com.egesa.clinic.shared.PatientVisitSummary
import com.egesa.clinic.shared.Sex
import com.egesa.clinic.shared.VisitType
import com.egesa.clinic.shared.data.DiagnosisInput
import com.egesa.clinic.shared.data.EncounterInput
import com.egesa.clinic.shared.data.EncounterOutcomeInput
import com.egesa.clinic.shared.data.HtsRegisterInput
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.data.MedicationOrderInput
import com.egesa.clinic.shared.data.PatientRegistrationInput
import com.egesa.clinic.shared.data.ServiceEventInput
import com.egesa.clinic.shared.ui.components.ClinicDropdownField
import com.egesa.clinic.shared.ui.components.FormActionRow
import com.egesa.clinic.shared.ui.components.HtsRegisterRow
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.theme.Navy800
import com.egesa.clinic.shared.ui.theme.Slate500
import com.egesa.clinic.shared.ui.theme.Slate700
import com.egesa.clinic.shared.ui.theme.White
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun ClinicalProgramsScreen(localRepository: LocalRepository, session: SessionState) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("OPD", "ANC", "Maternity", "CCC", "NCD", "HTS")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Clinical Registers", style = MaterialTheme.typography.headlineSmall, color = Navy800)
        Text(
            "Every saved record creates a visit, service event, and pending sync item.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500,
        )
        Spacer(Modifier.height(12.dp))
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
            }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            0 -> EncounterCaptureForm(localRepository, session, "OPD", VisitType.OUTPATIENT, "OPD_VISIT", "Diagnosis", "Treatment / plan")
            1 -> EncounterCaptureForm(localRepository, session, "ANC", VisitType.ANC, "ANC_VISIT", "Gestational age", "IFA / IPTp / counselling")
            2 -> EncounterCaptureForm(localRepository, session, "Maternity", VisitType.INPATIENT, "MATERNITY_DELIVERY", "Delivery mode", "Maternal / infant outcome")
            3 -> EncounterCaptureForm(localRepository, session, "CCC", VisitType.FOLLOW_UP, "CCC_FOLLOW_UP", "Current regimen", "Viral load / adherence")
            4 -> EncounterCaptureForm(localRepository, session, "NCD", VisitType.FOLLOW_UP, "NCD_FOLLOW_UP", "BP / glucose", "Medication refill")
            5 -> HtsForm(localRepository, session)
        }
    }
}

@Composable
private fun EncounterCaptureForm(
    localRepository: LocalRepository,
    session: SessionState,
    program: String,
    visitType: VisitType,
    indicatorCategory: String,
    primaryLabel: String,
    secondaryLabel: String
) {
    var patientId by remember { mutableStateOf("PT-${Clock.System.now().toEpochMilliseconds()}") }
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("female") }
    var primary by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }
    var medication by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf("DISCHARGED") }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$program encounter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(patientId, { patientId = it }, label = { Text("Patient ID") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(age, { age = it.filter { ch -> ch.isDigit() } }, label = { Text("Age") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(fullName, { fullName = it }, label = { Text("Patient full name") }, modifier = Modifier.fillMaxWidth())
                ClinicDropdownField(
                    value = sex,
                    onValueChange = { sex = it },
                    label = "Sex",
                    options = listOf("male", "female", "intersex", "unknown"),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(primary, { primary = it }, label = { Text(primaryLabel) }, modifier = Modifier.weight(1f))
                    OutlinedTextField(secondary, { secondary = it }, label = { Text(secondaryLabel) }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(medication, { medication = it }, label = { Text("Prescription / medication") }, modifier = Modifier.fillMaxWidth())
                ClinicDropdownField(
                    value = outcome,
                    onValueChange = { outcome = it },
                    label = "Outcome",
                    options = listOf("DISCHARGED", "REFERRED", "ADMITTED", "TRANSFERRED", "DECEASED"),
                    modifier = Modifier.fillMaxWidth(),
                    displayFormatter = { it }
                )

                FormActionRow(
                    cancelLabel = "Clear",
                    onCancel = {
                        patientId = "PT-${Clock.System.now().toEpochMilliseconds()}"
                        fullName = ""
                        age = ""
                        sex = "female"
                        primary = ""
                        secondary = ""
                        medication = ""
                        outcome = "DISCHARGED"
                        status = ""
                    },
                    primaryLabel = "Save encounter",
                    onPrimary = {
                        scope.launch {
                            status = saveProgramEncounter(
                                localRepository = localRepository,
                                session = session,
                                patientId = patientId.trim(),
                                fullName = fullName.trim(),
                                age = age.toIntOrNull() ?: 0,
                                sex = sex.toSex(),
                                program = program,
                                visitType = visitType,
                                indicatorCategory = indicatorCategory,
                                primary = primary.trim(),
                                secondary = secondary.trim(),
                                medication = medication.trim(),
                                outcome = outcome.trim()
                            )
                        }
                    },
                )
                if (status.isNotBlank()) {
                    Text(status, style = MaterialTheme.typography.bodySmall, color = Slate700)
                }
            }
        }
    }
}

@Composable
fun HtsForm(localRepository: LocalRepository, session: SessionState) {
    var patientId by remember { mutableStateOf("PT-${Clock.System.now().toEpochMilliseconds()}") }
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("female") }
    var entry by remember {
        mutableStateOf(
            HtsRegisterInput(
                htsId = "HTS-${Clock.System.now().toEpochMilliseconds()}",
                encounterId = "ENC-${Clock.System.now().toEpochMilliseconds()}",
                populationType = "General Pop",
                testingPoint = "OPD",
                finalResult = HivStatus.NEGATIVE
            )
        )
    }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("HTS encounter", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(patientId, { patientId = it }, label = { Text("Patient ID") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(age, { age = it.filter { ch -> ch.isDigit() } }, label = { Text("Age") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(fullName, { fullName = it }, label = { Text("Patient full name") }, modifier = Modifier.fillMaxWidth())
                ClinicDropdownField(
                    value = sex,
                    onValueChange = { sex = it },
                    label = "Sex",
                    options = listOf("male", "female", "intersex", "unknown"),
                    modifier = Modifier.fillMaxWidth()
                )
                HtsRegisterRow(entry = entry) { entry = it }

                FormActionRow(
                    cancelLabel = "Clear",
                    onCancel = {
                        patientId = "PT-${Clock.System.now().toEpochMilliseconds()}"
                        fullName = ""
                        age = ""
                        sex = "female"
                        entry = HtsRegisterInput(
                            htsId = "HTS-${Clock.System.now().toEpochMilliseconds()}",
                            encounterId = "ENC-${Clock.System.now().toEpochMilliseconds()}",
                            populationType = "General Pop",
                            testingPoint = "OPD",
                            finalResult = HivStatus.NEGATIVE,
                        )
                        status = ""
                    },
                    primaryLabel = "Save HTS",
                    onPrimary = {
                        scope.launch {
                            val now = Clock.System.now().toString()
                            val encounterId = "ENC-${Clock.System.now().toEpochMilliseconds()}"
                            localRepository.upsertPatient(
                                PatientRegistrationInput(
                                    id = patientId.trim(),
                                    fullName = fullName.ifBlank { patientId.trim() },
                                    age = age.toIntOrNull() ?: 0,
                                    sex = sex.toSex(),
                                )
                            )
                            localRepository.createEncounter(
                                EncounterInput(
                                    encounterId = encounterId,
                                    patientId = patientId.trim(),
                                    encounterDatetime = now,
                                    department = "HTS",
                                    visitType = VisitType.OUTPATIENT,
                                    providerId = session.staffId,
                                    facilityId = "EGESA-CLINIC",
                                )
                            )
                            localRepository.upsertHtsEntry(entry.copy(encounterId = encounterId))
                            localRepository.upsertServiceEvent(
                                ServiceEventInput(
                                    serviceEventId = "SVC-${Clock.System.now().toEpochMilliseconds()}",
                                    encounterId = encounterId,
                                    program = "HTS",
                                    indicatorCategory = "HTS_${entry.finalResult.name}",
                                    serviceCode = entry.testingPoint,
                                    valueText = entry.finalResult.code,
                                    eventDatetime = now,
                                )
                            )
                            status = "Saved locally and queued for sync."
                            entry = entry.copy(htsId = "HTS-${Clock.System.now().toEpochMilliseconds()}")
                        }
                    },
                )
                if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = Slate700)
            }
        }
    }
}

@Composable
private fun PharmacyDispensingScreen(localRepository: LocalRepository, session: SessionState) {
    var visits by remember { mutableStateOf<List<PatientVisitSummary>>(emptyList()) }
    var selectedEncounterId by remember { mutableStateOf("") }
    var medication by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        visits = localRepository.getActiveVisitSummaries()
        selectedEncounterId = visits.firstOrNull()?.encounterId.orEmpty()
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pharmacy dispensing", style = MaterialTheme.typography.headlineSmall, color = Navy800)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Recent encounters", style = MaterialTheme.typography.titleMedium)
                visits.take(4).forEach { visit ->
                    Text(
                        "${visit.encounterId} - ${visit.fullName} - ${visit.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
                ClinicDropdownField(
                    value = selectedEncounterId,
                    onValueChange = { selectedEncounterId = it },
                    label = "Encounter",
                    options = visits.map { it.encounterId },
                    modifier = Modifier.fillMaxWidth(),
                    displayFormatter = { encId ->
                        visits.find { it.encounterId == encId }?.let { "${it.encounterId} - ${it.fullName} (${it.department})" } ?: encId
                    }
                )
                OutlinedTextField(medication, { medication = it }, label = { Text("Medication dispensed") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(quantity, { quantity = it.filter { ch -> ch.isDigit() } }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
                FormActionRow(
                    cancelLabel = "Clear",
                    onCancel = {
                        medication = ""
                        quantity = "1"
                        status = ""
                        selectedEncounterId = visits.firstOrNull()?.encounterId.orEmpty()
                    },
                    primaryLabel = "Save dispense",
                    onPrimary = {
                        scope.launch {
                            val nowMs = Clock.System.now().toEpochMilliseconds()
                            localRepository.upsertServiceEvent(
                                ServiceEventInput(
                                    serviceEventId = "RX-$nowMs",
                                    encounterId = selectedEncounterId.trim(),
                                    program = "PHARMACY",
                                    indicatorCategory = "DISPENSED",
                                    serviceCode = medication.trim(),
                                    valueText = "Dispensed by ${session.staffId}",
                                    quantity = quantity.toLongOrNull() ?: 1,
                                )
                            )
                            localRepository.upsertMedicationOrder(
                                MedicationOrderInput(
                                    medicationOrderId = "MED-$nowMs",
                                    encounterId = selectedEncounterId.trim(),
                                    medicationName = medication.trim(),
                                    dose = quantity.ifBlank { "1" },
                                    instructions = "Dispensed",
                                )
                            )
                            status = "Dispensing saved locally and queued for sync."
                        }
                    },
                )
                if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = Slate700)
            }
        }
    }
}

private suspend fun saveProgramEncounter(
    localRepository: LocalRepository,
    session: SessionState,
    patientId: String,
    fullName: String,
    age: Int,
    sex: Sex,
    program: String,
    visitType: VisitType,
    indicatorCategory: String,
    primary: String,
    secondary: String,
    medication: String,
    outcome: String
): String {
    if (patientId.isBlank()) return "Patient ID is required."
    val now = Clock.System.now()
    val encounterId = "ENC-${now.toEpochMilliseconds()}"
    localRepository.upsertPatient(
        PatientRegistrationInput(
            id = patientId,
            fullName = fullName.ifBlank { patientId },
            age = age,
            sex = sex
        )
    )
    localRepository.createEncounter(
        EncounterInput(
            encounterId = encounterId,
            patientId = patientId,
            encounterDatetime = now.toString(),
            department = program,
            visitType = visitType,
            providerId = session.staffId,
            facilityId = "EGESA-CLINIC"
        )
    )
    if (primary.isNotBlank()) {
        localRepository.upsertDiagnosis(
            DiagnosisInput(
                diagnosisId = "DX-${now.toEpochMilliseconds()}",
                encounterId = encounterId,
                diagnosisText = primary,
                isPrimary = true,
                codeSystem = program,
                diagnosisCode = indicatorCategory
            )
        )
    }
    if (medication.isNotBlank()) {
        localRepository.upsertMedicationOrder(
            MedicationOrderInput(
                medicationOrderId = "MED-${now.toEpochMilliseconds()}",
                encounterId = encounterId,
                medicationName = medication,
                instructions = secondary.ifBlank { null }
            )
        )
    }
    localRepository.upsertEncounterOutcome(
        EncounterOutcomeInput(
            outcomeId = "OUT-${now.toEpochMilliseconds()}",
            encounterId = encounterId,
            disposition = outcome.toDisposition(),
            admitted = outcome.equals("ADMITTED", ignoreCase = true),
            dischargeNotes = secondary.ifBlank { null }
        )
    )
    localRepository.upsertServiceEvent(
        ServiceEventInput(
            serviceEventId = "SVC-${now.toEpochMilliseconds()}",
            encounterId = encounterId,
            program = program,
            indicatorCategory = indicatorCategory,
            serviceCode = primary.ifBlank { indicatorCategory },
            valueText = secondary.ifBlank { null },
            eventDatetime = now.toString()
        )
    )
    return "Saved $program encounter $encounterId locally. It will sync when online."
}

private fun String.toSex(): Sex = when (trim().lowercase()) {
    "m", "male" -> Sex.MALE
    "f", "female" -> Sex.FEMALE
    "intersex" -> Sex.INTERSEX
    else -> Sex.UNKNOWN
}

private fun String.toDisposition(): Disposition = when (trim().uppercase()) {
    "ADMITTED" -> Disposition.ADMITTED
    "TRANSFERRED" -> Disposition.TRANSFERRED
    "REFERRED" -> Disposition.REFERRED
    "DECEASED" -> Disposition.DECEASED
    else -> Disposition.DISCHARGED
}
