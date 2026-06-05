package com.egesa.clinic.shared.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.PatientDocumentInput
import com.egesa.clinic.shared.data.PatientRegistrationInput
import com.egesa.clinic.shared.data.RegistrationClinicalInput
import com.egesa.clinic.shared.ui.components.FormActionRow
import com.egesa.clinic.shared.ui.components.ModuleHeader
import com.egesa.clinic.shared.ui.components.TextBadge
import com.egesa.clinic.shared.ui.responsive.responsiveContentMaxWidth
import com.egesa.clinic.shared.ui.responsive.responsiveHorizontalPadding
import com.egesa.clinic.shared.ui.responsive.shouldUseCompactUI
import com.egesa.clinic.shared.ui.theme.Indigo700
import com.egesa.clinic.shared.ui.theme.Navy50
import com.egesa.clinic.shared.ui.theme.Navy800
import com.egesa.clinic.shared.ui.theme.Slate100
import com.egesa.clinic.shared.ui.theme.Slate200
import com.egesa.clinic.shared.ui.theme.Slate50
import com.egesa.clinic.shared.ui.theme.Slate500
import com.egesa.clinic.shared.ui.theme.Slate600
import com.egesa.clinic.shared.ui.theme.StatusCritical
import com.egesa.clinic.shared.ui.theme.White

@Composable
fun PatientRegistrationFlow(
    documentCaptureGateway: DocumentCaptureGateway,
    onDismiss: () -> Unit,
    onRegister: (PatientRegistrationInput, Int, RegistrationClinicalInput, PatientDocumentInput?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var formState by remember { mutableStateOf(RegistrationFormState()) }
    var stepIndex by remember { mutableIntStateOf(0) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val steps = RegistrationStep.entries
    val currentStep = steps[stepIndex]
    val horizontalPad = responsiveHorizontalPadding().dp
    val maxWidth = responsiveContentMaxWidth().dp

    Column(
        Modifier
            .fillMaxSize()
            .background(Slate50),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = horizontalPad, vertical = 16.dp),
        ) {
            ModuleHeader(
                title = "Patient registration",
                subtitle = "Register, capture documents, record vitals, and route to queue.",
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Slate600)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (stepIndex == 0) {
                        TextButton(
                            onClick = {
                                formState = RegistrationFormState()
                                validationError = null
                            },
                        ) {
                            Text("Clear", color = Slate500)
                        }
                    }
                    TextBadge(
                        text = "Step ${stepIndex + 1} of ${steps.size}",
                        fg = Indigo700,
                        bg = Navy50,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (shouldUseCompactUI()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { (stepIndex + 1).toFloat() / steps.size },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Navy800,
                        trackColor = Slate200,
                    )
                    Text(
                        currentStep.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Navy800,
                    )
                    Text(
                        currentStep.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                    )
                }
            } else {
                ScrollableTabRow(
                    selectedTabIndex = stepIndex,
                    edgePadding = 0.dp,
                    containerColor = White,
                    contentColor = Navy800,
                    divider = { Box(Modifier.fillMaxWidth().height(1.dp).background(Slate200)) },
                ) {
                    steps.forEachIndexed { index, step ->
                        Tab(
                            selected = stepIndex == index,
                            onClick = {
                                validationError = null
                                stepIndex = index
                            },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        step.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (stepIndex == index) Navy800 else Slate500,
                                    )
                                    Text(
                                        step.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate500,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                validationError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = StatusCritical)
                }
                when (currentStep) {
                    RegistrationStep.IDENTITY -> RegistrationDemographicsSection(formState) { formState = it }
                    RegistrationStep.VITALS -> RegistrationVitalsSection(formState) { formState = it }
                    RegistrationStep.QUEUE -> RegistrationQueueSection(formState) { formState = it }
                    RegistrationStep.DOCUMENTS -> RegistrationDocumentSection(
                        formState,
                        onStateChange = { formState = it },
                        documentCaptureGateway = documentCaptureGateway,
                        scope = scope,
                    )
                    RegistrationStep.REVIEW -> RegistrationReviewSection(formState)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = horizontalPad, vertical = 14.dp),
        ) {
            when {
                stepIndex == 0 && currentStep != RegistrationStep.REVIEW -> FormActionRow(
                    cancelLabel = "Cancel",
                    onCancel = onDismiss,
                    primaryLabel = "Continue",
                    onPrimary = {
                        validationError = validateRegistrationStep(currentStep, formState)
                        if (validationError == null) stepIndex++
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                currentStep != RegistrationStep.REVIEW -> FormActionRow(
                    cancelLabel = "Previous",
                    onCancel = {
                        validationError = null
                        stepIndex--
                    },
                    primaryLabel = "Continue",
                    onPrimary = {
                        validationError = validateRegistrationStep(currentStep, formState)
                        if (validationError == null) stepIndex++
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> FormActionRow(
                    cancelLabel = "Previous",
                    onCancel = {
                        validationError = null
                        stepIndex--
                    },
                    primaryLabel = "Register patient",
                    onPrimary = {
                        validationError = validateRegistrationStep(RegistrationStep.IDENTITY, formState)
                        val payload = buildRegistrationPayload(formState)
                        if (validationError != null) return@FormActionRow
                        if (payload == null) {
                            validationError = "Unable to build registration. Check required fields."
                            return@FormActionRow
                        }
                        onRegister(
                            payload.patient,
                            payload.triageLevel,
                            payload.clinical,
                            payload.document,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
