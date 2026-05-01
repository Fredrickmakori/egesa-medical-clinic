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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
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
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.WorkflowArea

fun main() = application {
    val state = remember { HospitalState() }
    Window(onCloseRequest = ::exitApplication, title = "Hospital Manager") {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                DesktopAppShell(state)
            }
        }
    }
}

@Composable
private fun DesktopAppShell(state: HospitalState) {
    var currentRole by remember { mutableStateOf(UserRole.ADMIN) }
    val visibleNavItems = state.globalNavItemsFor(currentRole)
    var activeArea by remember { mutableStateOf(visibleNavItems.first().area) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Egesa Hospital Management") },
                actions = {
                    Text(state.globalActions().joinToString(" • ") { it.label }, Modifier.padding(end = 12.dp))
                }
            )
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            NavigationRail {
                visibleNavItems.forEach { item ->
                    NavigationRailItem(
                        selected = activeArea == item.area,
                        onClick = { activeArea = item.area },
                        icon = { Text(item.label.take(1)) },
                        label = { Text(item.label) }
                    )
                }
            }

            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Role: ${currentRole.name} | ${visibleNavItems.firstOrNull { it.area == activeArea }?.visibilityAnnotation.orEmpty()}")
                BreadcrumbHeader(state.breadcrumbFor(activeArea))
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
    }
}

@Composable
private fun BreadcrumbHeader(path: List<String>) {
    Text(path.joinToString(" > "), style = MaterialTheme.typography.titleMedium)
}
