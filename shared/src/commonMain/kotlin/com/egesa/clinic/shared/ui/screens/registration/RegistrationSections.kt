package com.egesa.clinic.shared.ui.screens.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.ui.components.ClinicCard
import com.egesa.clinic.shared.ui.components.LabeledDivider
import com.egesa.clinic.shared.ui.components.SectionHeader
import com.egesa.clinic.shared.ui.components.TextBadge
import com.egesa.clinic.shared.ui.responsive.isWideLayout
import com.egesa.clinic.shared.ui.theme.Indigo700
import com.egesa.clinic.shared.ui.theme.Navy800
import com.egesa.clinic.shared.ui.theme.Slate200
import com.egesa.clinic.shared.ui.theme.Slate400
import com.egesa.clinic.shared.ui.theme.Slate500
import com.egesa.clinic.shared.ui.theme.Slate600
import com.egesa.clinic.shared.ui.theme.Slate700
import com.egesa.clinic.shared.ui.theme.Slate900
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun RegistrationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = MaterialTheme.shapes.medium,
        colors = registrationFieldColors(),
    )
}

@Composable
private fun registrationFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Indigo700,
    unfocusedBorderColor = Slate200,
    focusedContainerColor = com.egesa.clinic.shared.ui.theme.White,
    unfocusedContainerColor = com.egesa.clinic.shared.ui.theme.White,
)

@Composable
fun RegistrationSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ClinicCard(modifier.fillMaxWidth()) {
        SectionHeader(title)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun RegistrationFieldRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (isWideLayout()) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    } else {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
fun RegistrationDemographicsSection(
    state: RegistrationFormState,
    onStateChange: (RegistrationFormState) -> Unit,
) {
    RegistrationSectionCard("Patient identity & demographics") {
        RegistrationField(state.id, { onStateChange(state.copy(id = it)) }, "Patient ID")
        Spacer(Modifier.height(8.dp))
        RegistrationField(state.fullName, { onStateChange(state.copy(fullName = it)) }, "Full name")
        Spacer(Modifier.height(8.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.age,
                { onStateChange(state.copy(age = it.digitsOnly())) },
                "Age",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.triageLevel,
                { onStateChange(state.copy(triageLevel = it.digitsOnly().take(1))) },
                "Triage (1–5)",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
        Spacer(Modifier.height(8.dp))
        RegistrationField(state.sex, { onStateChange(state.copy(sex = it)) }, "Sex (male, female, intersex, unknown)")
        Spacer(Modifier.height(8.dp))
        LabeledDivider("Location & acuity")
        Spacer(Modifier.height(8.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.assignedWard,
                { onStateChange(state.copy(assignedWard = it)) },
                "Ward",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.roomBed,
                { onStateChange(state.copy(roomBed = it)) },
                "Bed",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
        Spacer(Modifier.height(8.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.acuity,
                { onStateChange(state.copy(acuity = it)) },
                "Acuity",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.isolation,
                { onStateChange(state.copy(isolation = it)) },
                "Isolation",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
    }
}

@Composable
fun RegistrationVitalsSection(
    state: RegistrationFormState,
    onStateChange: (RegistrationFormState) -> Unit,
) {
    RegistrationSectionCard("Clinical vitals (optional)") {
        Text(
            "Capture vitals now or leave blank to record later in triage.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500,
        )
        Spacer(Modifier.height(10.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.weightKg,
                { onStateChange(state.copy(weightKg = it.decimalInput())) },
                "Weight (kg)",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.heightCm,
                { onStateChange(state.copy(heightCm = it.decimalInput())) },
                "Height (cm)",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
        Spacer(Modifier.height(8.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.temperatureC,
                { onStateChange(state.copy(temperatureC = it.decimalInput())) },
                "Temp (°C)",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.muacCm,
                { onStateChange(state.copy(muacCm = it.decimalInput())) },
                "MUAC (cm)",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
        Spacer(Modifier.height(8.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.systolicBp,
                { onStateChange(state.copy(systolicBp = it.digitsOnly())) },
                "BP systolic",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.diastolicBp,
                { onStateChange(state.copy(diastolicBp = it.digitsOnly())) },
                "BP diastolic",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
        Spacer(Modifier.height(8.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.pulseBpm,
                { onStateChange(state.copy(pulseBpm = it.digitsOnly())) },
                "Pulse",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.respiratoryRate,
                { onStateChange(state.copy(respiratoryRate = it.digitsOnly())) },
                "Respiratory rate",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            if (isWideLayout()) {
                RegistrationField(
                    state.spo2Percent,
                    { onStateChange(state.copy(spo2Percent = it.decimalInput())) },
                    "SpO₂ %",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (!isWideLayout()) {
            Spacer(Modifier.height(8.dp))
            RegistrationField(
                state.spo2Percent,
                { onStateChange(state.copy(spo2Percent = it.decimalInput())) },
                "SpO₂ %",
            )
        }
        val bmi = calculateBmi(state.weightKg.toDoubleOrNull(), state.heightCm.toDoubleOrNull())
        if (bmi != null) {
            Spacer(Modifier.height(10.dp))
            TextBadge("BMI $bmi", Slate700, com.egesa.clinic.shared.ui.theme.Slate100)
        }
    }
}

@Composable
fun RegistrationQueueSection(
    state: RegistrationFormState,
    onStateChange: (RegistrationFormState) -> Unit,
) {
    RegistrationSectionCard("Queue & triage routing") {
        Text(
            "Forward the patient to the next workflow step after registration.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500,
        )
        Spacer(Modifier.height(10.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.queueDestination,
                { onStateChange(state.copy(queueDestination = it)) },
                "Next step",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.queuePriority,
                { onStateChange(state.copy(queuePriority = it)) },
                "Priority",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
        Spacer(Modifier.height(8.dp))
        RegistrationField(
            state.queueNote,
            { onStateChange(state.copy(queueNote = it)) },
            "Queue note",
            singleLine = false,
        )
    }
}

@Composable
fun RegistrationDocumentSection(
    state: RegistrationFormState,
    onStateChange: (RegistrationFormState) -> Unit,
    documentCaptureGateway: DocumentCaptureGateway,
    scope: CoroutineScope,
) {
    fun runDocumentAction(action: suspend () -> com.egesa.clinic.shared.data.DocumentCaptureResult) {
        scope.launch {
            onStateChange(state.copy(documentActionBusy = true))
            val next = runCatching { action() }
                .fold(
                    onSuccess = { state.applyCaptureResult(it) },
                    onFailure = {
                        state.copy(documentActionMessage = it.message ?: "Document action failed.")
                    },
                )
            onStateChange(next.copy(documentActionBusy = false))
        }
    }

    RegistrationSectionCard("Document capture & verification") {
        state.documentActionMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Slate500)
            Spacer(Modifier.height(8.dp))
        }
        RegistrationFieldRow {
            RegistrationField(
                state.documentType,
                { onStateChange(state.copy(documentType = it)) },
                "Document type",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.verificationStatus,
                { onStateChange(state.copy(verificationStatus = it)) },
                "Verification status",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
        Spacer(Modifier.height(8.dp))
        RegistrationField(
            state.documentPhotoUri,
            { onStateChange(state.copy(documentPhotoUri = it)) },
            "Photo URI or file path",
        )
        Spacer(Modifier.height(10.dp))
        if (isWideLayout()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DocumentActionButtons(state, documentCaptureGateway, ::runDocumentAction, Modifier.weight(1f))
            }
        } else {
            DocumentActionButtons(state, documentCaptureGateway, ::runDocumentAction, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(10.dp))
        LabeledDivider("Extracted fields")
        Spacer(Modifier.height(8.dp))
        RegistrationField(
            state.extractedFullName,
            { onStateChange(state.copy(extractedFullName = it)) },
            "Extracted full name",
        )
        Spacer(Modifier.height(8.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.extractedIdentifier,
                { onStateChange(state.copy(extractedIdentifier = it)) },
                "Extracted ID",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.extractedBirthDate,
                { onStateChange(state.copy(extractedBirthDate = it)) },
                "Birth date",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
        Spacer(Modifier.height(8.dp))
        RegistrationFieldRow {
            RegistrationField(
                state.extractedSex,
                { onStateChange(state.copy(extractedSex = it)) },
                "Extracted sex",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
            RegistrationField(
                state.extractedGuardianName,
                { onStateChange(state.copy(extractedGuardianName = it)) },
                "Guardian",
                modifier = if (isWideLayout()) Modifier.weight(1f) else Modifier,
            )
        }
        Spacer(Modifier.height(8.dp))
        RegistrationField(
            state.documentNotes,
            { onStateChange(state.copy(documentNotes = it)) },
            "Document notes",
            singleLine = false,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { onStateChange(state.applyExtractedToDemographics()) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        ) {
            Text("Apply extracted data to identity")
        }
    }
}

@Composable
private fun DocumentActionButtons(
    state: RegistrationFormState,
    gateway: DocumentCaptureGateway,
    runAction: (suspend () -> com.egesa.clinic.shared.data.DocumentCaptureResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    runAction {
                        gateway.capturePhoto(state.documentType.ifBlank { "Document" })
                    }
                },
                enabled = !state.documentActionBusy && gateway.canCapturePhoto,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
            ) {
                Text("Take photo")
            }
            OutlinedButton(
                onClick = {
                    runAction {
                        gateway.attachImage(state.documentType.ifBlank { "Document" })
                    }
                },
                enabled = !state.documentActionBusy && gateway.canAttachImage,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text("Attach image")
            }
        }
        OutlinedButton(
            onClick = {
                runAction {
                    gateway.extractData(
                        imageUri = state.documentPhotoUri,
                        documentType = state.documentType.ifBlank { "Document" },
                    )
                }
            },
            enabled = !state.documentActionBusy && state.documentPhotoUri.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        ) {
            Text("Extract from image (OCR)")
        }
    }
}

@Composable
fun RegistrationReviewSection(state: RegistrationFormState) {
    val payload = buildRegistrationPayload(state)
    RegistrationSectionCard("Review & submit") {
        if (payload == null) {
            Text(
                "Complete identity fields before registering.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
            )
            return@RegistrationSectionCard
        }
        val patient = payload.patient
        val clinical = payload.clinical
        ReviewRow("Patient ID", patient.id)
        ReviewRow("Full name", patient.fullName)
        ReviewRow("Age / sex", "${patient.age} yrs, ${patient.sex.code}")
        ReviewRow("Triage level", payload.triageLevel.toString())
        patient.assignedWard?.let { ReviewRow("Ward / bed", listOfNotNull(it, patient.roomBed).joinToString(" • ")) }
        ReviewRow("Acuity", patient.acuity)
        patient.isolation?.let { ReviewRow("Isolation", it) }
        Spacer(Modifier.height(8.dp))
        LabeledDivider("Routing")
        Spacer(Modifier.height(8.dp))
        ReviewRow("Next step", clinical.queueDestination ?: "Triage")
        ReviewRow("Priority", clinical.queuePriority ?: "Routine")
        clinical.queueNote?.let { ReviewRow("Note", it) }
        if (clinical.hasVitals()) {
            Spacer(Modifier.height(8.dp))
            LabeledDivider("Vitals")
            Spacer(Modifier.height(8.dp))
            clinical.weightKg?.let { ReviewRow("Weight", "$it kg") }
            clinical.heightCm?.let { ReviewRow("Height", "$it cm") }
            calculateBmi(clinical.weightKg, clinical.heightCm)?.let { ReviewRow("BMI", it.toString()) }
            clinical.temperatureC?.let { ReviewRow("Temperature", "$it °C") }
            if (clinical.systolicBp != null || clinical.diastolicBp != null) {
                ReviewRow("Blood pressure", "${clinical.systolicBp ?: "—"}/${clinical.diastolicBp ?: "—"}")
            }
        }
        if (payload.document != null) {
            Spacer(Modifier.height(8.dp))
            LabeledDivider("Document")
            Spacer(Modifier.height(8.dp))
            ReviewRow("Type", payload.document.documentType)
            ReviewRow("Status", payload.document.verificationStatus)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Registering saves locally, queues sync, and forwards to the selected destination.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate400,
        )
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Slate500)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Slate900)
    }
}
