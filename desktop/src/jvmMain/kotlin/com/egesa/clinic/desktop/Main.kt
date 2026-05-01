package com.egesa.clinic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import com.egesa.clinic.shared.BottleneckCell
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
            AdminDashboard(state)
        } else {
            PatientList(state)
        }
    }
}

@Composable
private fun AdminDashboard(state: HospitalState) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Admin Dashboard", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.adminKpis().take(3).forEach { metric ->
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(metric.title)
                            Text(metric.value, style = MaterialTheme.typography.headlineSmall)
                            metric.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.adminKpis().drop(3).forEach { metric ->
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(metric.title)
                            Text(metric.value, style = MaterialTheme.typography.headlineSmall)
                            metric.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }

        item {
            Text("Analytics Widgets", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trend Line: Registrations This Week")
                    state.registrationTrend().forEach { point ->
                        Text("${point.label}: ${point.value}")
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Department Comparison")
                    state.departmentComparison().forEach { item ->
                        Text("${item.department} • throughput ${item.throughput} • avg ${item.avgTurnaroundMinutes} min")
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bottleneck Heatmap")
                    state.bottleneckHeatmap().forEach { cell ->
                        BottleneckRow(cell)
                    }
                }
            }
        }

        item { Text("User & Role Management", style = MaterialTheme.typography.titleMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Actions: Create User • Assign Role • Reset Password • Activate/Deactivate")
                    HorizontalDivider()
                    state.users().forEach { user ->
                        Text("${user.id} • ${user.fullName} • ${user.role}")
                        Text("Status: ${if (user.active) "Active" else "Inactive"} | Reset Required: ${if (user.passwordResetRequired) "Yes" else "No"}")
                        HorizontalDivider()
                    }
                }
            }
        }

        item { Text("Audit Log", style = MaterialTheme.typography.titleMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("User | Action | Module | Timestamp | Patient/Context")
                    HorizontalDivider()
                    state.auditLog().forEach { event ->
                        Text("${event.user} | ${event.action} | ${event.module} | ${event.timestamp} | ${event.contextReference}")
                    }
                }
            }
        }

        item { Text("Configuration", style = MaterialTheme.typography.titleMedium) }
        items(state.configurationSets()) { config ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(config.title)
                    Text(config.entries.joinToString(" • "))
                }
            }
        }
    }
}

@Composable
private fun BottleneckRow(cell: BottleneckCell) {
    val severityColor = when (cell.severity) {
        "Critical" -> Color(0xFFFFCDD2)
        "High" -> Color(0xFFFFE0B2)
        "Medium" -> Color(0xFFFFF9C4)
        else -> Color(0xFFC8E6C9)
    }

    Row(
        Modifier.fillMaxWidth().background(severityColor).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(cell.workflowStage)
        Text("${cell.pendingCount} pending (${cell.severity})")
    }
}

@Composable
private fun PatientList(state: HospitalState) {
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
