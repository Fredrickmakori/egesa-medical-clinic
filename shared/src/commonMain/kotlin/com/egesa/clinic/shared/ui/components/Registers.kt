package com.egesa.clinic.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.HivStatus
import com.egesa.clinic.shared.data.HtsRegisterInput
import com.egesa.clinic.shared.ui.theme.Slate200
import com.egesa.clinic.shared.ui.theme.Slate700
import com.egesa.clinic.shared.ui.theme.White

@Composable
fun HtsRegisterRow(
    entry: HtsRegisterInput,
    onUpdate: (HtsRegisterInput) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Slate200)
            .background(White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Serial / HTS Number
        RegisterCell(entry.serialNumber ?: "", "Serial", weight = 1f) { onUpdate(entry.copy(serialNumber = it)) }
        RegisterCell(entry.htsNumber ?: "", "HTS No", weight = 1.5f) { onUpdate(entry.copy(htsNumber = it)) }
        
        // Pop Type & Testing Point
        RegisterCell(entry.populationType, "Pop Type", weight = 1.5f) { onUpdate(entry.copy(populationType = it)) }
        RegisterCell(entry.testingPoint, "Point", weight = 1.5f) { onUpdate(entry.copy(testingPoint = it)) }
        
        // Results
        ResultDropdown(entry.test1Result, "Test 1", allowEmpty = true) { onUpdate(entry.copy(test1Result = it)) }
        ResultDropdown(entry.test2Result, "Test 2", allowEmpty = true) { onUpdate(entry.copy(test2Result = it)) }
        ResultDropdown(entry.finalResult, "Final", allowEmpty = false) {
            onUpdate(entry.copy(finalResult = it ?: HivStatus.UNKNOWN))
        }
        
        // Referral
        RegisterCell(entry.referredTo ?: "", "Referred To", weight = 2f) { onUpdate(entry.copy(referredTo = it)) }
    }
}

@Composable
private fun RowScope.RegisterCell(
    value: String,
    label: String,
    weight: Float,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.weight(weight).padding(horizontal = 4.dp)) {
        Text(label, fontSize = 10.sp, color = Slate700, fontWeight = FontWeight.Bold)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().border(0.5.dp, Slate200).padding(4.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun RowScope.ResultDropdown(
    value: HivStatus?,
    label: String,
    allowEmpty: Boolean,
    onSelect: (HivStatus?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val results = buildList<HivStatus?> {
        if (allowEmpty) add(null)
        add(HivStatus.NEGATIVE)
        add(HivStatus.POSITIVE)
        add(HivStatus.EXPOSED)
        add(HivStatus.UNKNOWN)
    }
    val selectedText = value?.code?.replaceFirstChar { it.uppercase() } ?: "Select"

    Column(modifier = Modifier.weight(1.5f).padding(horizontal = 4.dp)) {
        Text(label, fontSize = 10.sp, color = Slate700, fontWeight = FontWeight.Bold)
        Box {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().border(0.5.dp, Slate200),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(selectedText, fontSize = 11.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                results.forEach { res ->
                    DropdownMenuItem(
                        text = {
                            Text(res?.code?.replaceFirstChar { it.uppercase() } ?: "Select")
                        },
                        onClick = {
                            onSelect(res)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
