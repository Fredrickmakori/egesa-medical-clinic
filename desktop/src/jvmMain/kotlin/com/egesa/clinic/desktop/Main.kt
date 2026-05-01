package com.egesa.clinic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.Shift
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

        if (activeArea == WorkflowArea.WARD) {
            WardOperationsScreen(state)
        } else {
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
}

@Composable
private fun WardOperationsScreen(state: HospitalState) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { WardOverviewCard(state) }
        item { BedBoard(state) }
        item { AdmissionTransferDischargeFlow(state) }
        item { NursingTaskList(state) }
        item { PrintableWardCensusAndHandoff(state) }
    }
}

@Composable
private fun WardOverviewCard(state: HospitalState) {
    val overview = state.wardOverview()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Ward Overview", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverviewChip("Occupancy", "${overview.occupancyPercent}%")
                OverviewChip("Beds Available", overview.bedsAvailable.toString())
                OverviewChip("Nurse Workload", overview.nurseWorkload)
            }
            Text("Alerts", style = MaterialTheme.typography.titleSmall)
            overview.alerts.forEach { Text("• $it") }
        }
    }
}

@Composable
private fun OverviewChip(label: String, value: String) {
    Card(Modifier.weight(1f)) {
        Column(Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun BedBoard(state: HospitalState) {
    val beds = state.bedBoard()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Bed Board", style = MaterialTheme.typography.titleMedium)
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            items(beds) { bed ->
                Card(Modifier.padding(4.dp)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${bed.ward} • ${bed.roomBed}", style = MaterialTheme.typography.titleSmall)
                        Text(bed.patientName)
                        Text("Status: ${bed.status}")
                        Text("Acuity: ${bed.acuity}")
                        Text("Isolation: ${bed.isolation ?: "None"}")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdmissionTransferDischargeFlow(state: HospitalState) {
    val atd = state.atdState()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Admission / Transfer / Discharge", style = MaterialTheme.typography.titleMedium)
            Text("Assign Bed: ${atd.selectedPatientId} → ${atd.selectedBed}")
            Text("Transfer Ward: ${atd.selectedPatientId} → ${atd.transferWard}")
            Text("Discharge Checklist")
            atd.dischargeChecklist.forEach { (item, done) ->
                Text("${if (done) "✓" else "☐"} $item")
            }
        }
    }
}

@Composable
private fun NursingTaskList(state: HospitalState) {
    val tasks = state.nursingTasks()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Nursing Task List", style = MaterialTheme.typography.titleMedium)
            tasks.forEach { task ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(if (task.priority == "High") Color(0xFFFFF0F0) else Color.Transparent).padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${task.type}: ${task.detail}")
                    Text("${task.due} • ${task.priority}")
                }
            }
        }
    }
}

@Composable
private fun PrintableWardCensusAndHandoff(state: HospitalState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Printable Ward Census", style = MaterialTheme.typography.titleMedium)
            state.wardCensus().forEach { row ->
                Text("${row.ward}: ${row.occupiedBeds}/${row.totalBeds} occupied | High acuity ${row.highAcuityCount} | Isolation ${row.isolationCount}")
            }
            Divider()
            Text("Shift Handoff Summary", style = MaterialTheme.typography.titleMedium)
            Text("Day Shift")
            state.shiftHandoffSummary(Shift.DAY).forEach { Text("• $it") }
            Text("Night Shift")
            state.shiftHandoffSummary(Shift.NIGHT).forEach { Text("• $it") }
        }
    }
}
