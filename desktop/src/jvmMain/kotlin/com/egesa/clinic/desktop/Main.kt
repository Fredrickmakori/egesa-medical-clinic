package com.egesa.clinic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.WorkflowArea
import java.time.LocalDate

private enum class OrdersBoardTab { LAB, IMAGING, PROCEDURES }
private enum class OrderStatus(val label: String) {
    PENDING("Pending"),
    COLLECTED("Collected"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    CRITICAL_RESULT("Critical Result")
}

data class OrderItem(
    val id: String,
    val patientName: String,
    val category: OrdersBoardTab,
    val title: String,
    val urgency: String,
    val status: OrderStatus,
    val orderedBy: String,
    val notes: String,
    val abnormal: Boolean,
    val trendPoints: List<Int>,
    val attachments: List<String>,
    val reviewedBy: String? = null,
    val acknowledgedCritical: Boolean = false
)

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

        OrdersBoard()

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

@Composable
private fun OrdersBoard() {
    var tab by remember { mutableStateOf(OrdersBoardTab.LAB) }
    val orders = remember {
        mutableStateListOf(
            OrderItem("ORD-1001", "Amina Yusuf", OrdersBoardTab.LAB, "CBC", "STAT", OrderStatus.PROCESSING, "Dr. Oduor", "Rule out sepsis", true, listOf(8, 9, 11, 13), listOf("cbc-report.pdf"), null),
            OrderItem("ORD-1002", "John Ouma", OrdersBoardTab.IMAGING, "Chest X-Ray", "Routine", OrderStatus.COMPLETED, "Dr. Maina", "Persistent cough", false, listOf(2, 2, 1, 1), listOf("xray-image.png"), "Dr. Maina"),
            OrderItem("ORD-1003", "Martha Wekesa", OrdersBoardTab.PROCEDURES, "Lumbar puncture", "Urgent", OrderStatus.CRITICAL_RESULT, "Dr. Naliaka", "Neuro decline overnight", true, listOf(5, 7, 9, 12), listOf("consent-form.pdf", "op-note.pdf"), "Dr. Naliaka", false)
        )
    }
    var showEntryDialog by remember { mutableStateOf(false) }
    var selectedStatuses by remember { mutableStateOf(setOf<OrderStatus>()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Orders Board", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { showEntryDialog = true }) { Text("New Order") }
            }

            TabRow(selectedTabIndex = tab.ordinal) {
                OrdersBoardTab.entries.forEach { entry ->
                    Tab(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        text = { Text(entry.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderStatus.entries.forEach { status ->
                    FilterChip(
                        selected = status in selectedStatuses,
                        onClick = {
                            selectedStatuses = if (status in selectedStatuses) selectedStatuses - status else selectedStatuses + status
                        },
                        label = { Text(status.label) }
                    )
                }
            }

            val filteredOrders = orders.filter {
                it.category == tab && (selectedStatuses.isEmpty() || it.status in selectedStatuses)
            }

            filteredOrders.forEach { order ->
                OrderCard(order = order) { acknowledgedId ->
                    val idx = orders.indexOfFirst { it.id == acknowledgedId }
                    if (idx != -1) {
                        orders[idx] = orders[idx].copy(acknowledgedCritical = true, reviewedBy = "Charge Nurse")
                    }
                }
            }
        }
    }

    if (showEntryDialog) {
        OrderEntryDialog(
            onDismiss = { showEntryDialog = false },
            onSave = { newOrder ->
                orders.add(0, newOrder.copy(id = "ORD-${1000 + orders.size + 1}"))
                showEntryDialog = false
            }
        )
    }
}

@Composable
private fun OrderCard(order: OrderItem, onAcknowledgeCritical: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${order.id} • ${order.patientName}", style = MaterialTheme.typography.titleMedium)
                StatusChip(order.status)
            }
            Text("Order: ${order.title} (${order.urgency})")
            Text("Ordering Clinician: ${order.orderedBy}")
            Text("Notes: ${order.notes}")

            ResultViewer(order)

            if (order.status == OrderStatus.CRITICAL_RESULT) {
                Card(Modifier.fillMaxWidth().background(Color(0xFFFFEAEA))) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (order.acknowledgedCritical) "Critical result acknowledged" else "Critical result escalation required")
                        Button(onClick = { onAcknowledgeCritical(order.id) }, enabled = !order.acknowledgedCritical) {
                            Text(if (order.acknowledgedCritical) "Acknowledged" else "Acknowledge")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val color = when (status) {
        OrderStatus.PENDING -> Color(0xFFE3F2FD)
        OrderStatus.COLLECTED -> Color(0xFFE8F5E9)
        OrderStatus.PROCESSING -> Color(0xFFFFF8E1)
        OrderStatus.COMPLETED -> Color(0xFFEDE7F6)
        OrderStatus.CRITICAL_RESULT -> Color(0xFFFFCDD2)
    }
    AssistChip(onClick = {}, label = { Text(status.label) }, modifier = Modifier.background(color))
}

@Composable
private fun ResultViewer(order: OrderItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Result Viewer", style = MaterialTheme.typography.titleSmall)
        if (order.abnormal) {
            Text("Abnormal findings detected", color = Color(0xFFC62828))
        }
        Text("Trend: ${order.trendPoints.joinToString(" → ")}")
        Text("Attachments: ${order.attachments.joinToString()}")
        Text("Reviewer Sign-off: ${order.reviewedBy ?: "Pending"}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderEntryDialog(onDismiss: () -> Unit, onSave: (OrderItem) -> Unit) {
    var patientName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CBC") }
    var urgency by remember { mutableStateOf("Routine") }
    var notes by remember { mutableStateOf("") }
    var clinician by remember { mutableStateOf("") }
    var selectorOpen by remember { mutableStateOf(false) }

    val testOptions = listOf("CBC", "Renal Panel", "Chest CT", "ECG", "Wound Debridement")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = patientName, onValueChange = { patientName = it }, label = { Text("Patient Name") })
                ExposedDropdownMenuBox(expanded = selectorOpen, onExpandedChange = { selectorOpen = it }) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Test/Procedure") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = selectorOpen) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = selectorOpen, onDismissRequest = { selectorOpen = false }) {
                        testOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                selectedType = option
                                selectorOpen = false
                            })
                        }
                    }
                }
                OutlinedTextField(value = urgency, onValueChange = { urgency = it }, label = { Text("Urgency") })
                OutlinedTextField(value = clinician, onValueChange = { clinician = it }, label = { Text("Ordering Clinician") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val typeCategory = when {
                    selectedType.contains("CT") || selectedType.contains("X-Ray") || selectedType.contains("ECG") -> OrdersBoardTab.IMAGING
                    selectedType.contains("Debridement") || selectedType.contains("Procedure") -> OrdersBoardTab.PROCEDURES
                    else -> OrdersBoardTab.LAB
                }
                onSave(
                    OrderItem(
                        id = "",
                        patientName = patientName.ifBlank { "Unknown Patient" },
                        category = typeCategory,
                        title = selectedType,
                        urgency = urgency,
                        status = OrderStatus.PENDING,
                        orderedBy = clinician.ifBlank { "Unassigned" },
                        notes = notes,
                        abnormal = false,
                        trendPoints = listOf(LocalDate.now().dayOfMonth % 10),
                        attachments = emptyList()
                    )
                )
            }) { Text("Save Order") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
