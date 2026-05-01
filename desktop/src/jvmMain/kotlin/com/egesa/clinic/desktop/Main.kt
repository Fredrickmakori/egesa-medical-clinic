package com.egesa.clinic.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.egesa.clinic.shared.HospitalState
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
}
