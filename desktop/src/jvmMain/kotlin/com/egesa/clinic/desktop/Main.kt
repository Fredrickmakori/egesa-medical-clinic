package com.egesa.clinic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.egesa.clinic.shared.EncounterForm
import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.SaveState
import com.egesa.clinic.shared.TimelineEvent
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
                DesktopAppShell(state)
            }
        }
    }
}

enum class BillingCategory(val title: String) {
    SERVICES("Services Billing"),
    PHARMACY("Pharmacy Billing")
}

data class BillItem(val name: String, val qty: Int, val unitPrice: Double)

enum class TxStatus { PENDING, SUCCESS, FAILED, CANCELLED }

data class Transaction(
    val patient: String,
    val category: BillingCategory,
    val date: LocalDate,
    val amount: Double,
    val status: TxStatus,
    val receiptNo: String? = null,
    val reason: String? = null
)

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

        if (activeArea == WorkflowArea.CONSULTATION) {
            ConsultationWorkbench(state)
        } else {
            DefaultAreaView(state, activeArea)
        }
    }
}

@Composable
private fun DefaultAreaView(state: HospitalState, activeArea: WorkflowArea) {
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

@Composable
private fun ConsultationWorkbench(state: HospitalState) {
    val patients = state.allPatients()
    var filterQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var selectedPatient by remember { mutableStateOf(patients.first()) }

    var form by remember { mutableStateOf(EncounterForm()) }
    var saveState by remember { mutableStateOf(SaveState.DRAFT_SAVED) }

    val filteredPatients = patients.filter {
        val queryMatches = filterQuery.isBlank() || it.fullName.contains(filterQuery, true) || it.id.contains(filterQuery, true)
        val statusMatches = statusFilter == "All" || it.status.contains(statusFilter, true)
        queryMatches && statusMatches
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.weight(1.1f).fillMaxSize()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Patients & Filters", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(filterQuery, { filterQuery = it }, label = { Text("Search patient") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "consultation", "diagnosis", "Admitted").forEach { status ->
                        Button(onClick = { statusFilter = status }) { Text(status) }
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredPatients) { patient ->
                        Card(
                            modifier = Modifier.fillMaxWidth().background(
                                if (patient.id == selectedPatient.id) Color(0xFFE8F5E9) else Color.Transparent
                            )
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${patient.id} • ${patient.fullName}")
                                Text(patient.status)
                                Button(onClick = { selectedPatient = patient }) { Text("Open") }
                            }
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

        OrdersBoard()

        Card(Modifier.weight(1.6f).fillMaxSize()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Encounter Form", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(form.chiefComplaint, {
                    form = form.copy(chiefComplaint = it)
                    saveState = SaveState.UNSAVED_CHANGES
                }, label = { Text("Chief complaint") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(form.history, {
                    form = form.copy(history = it)
                    saveState = SaveState.UNSAVED_CHANGES
                }, label = { Text("History") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(form.examinationFindings, {
                    form = form.copy(examinationFindings = it)
                    saveState = SaveState.UNSAVED_CHANGES
                }, label = { Text("Examination findings") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(form.provisionalDiagnosis, {
                    form = form.copy(provisionalDiagnosis = it)
                    saveState = SaveState.UNSAVED_CHANGES
                }, label = { Text("Provisional diagnosis") }, modifier = Modifier.fillMaxWidth())

                Text("Quick Actions", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Order Labs", "Request Imaging", "Prescribe Meds", "Refer", "Admit/Discharge").forEach { action ->
                        Button(onClick = { saveState = SaveState.UNSAVED_CHANGES }, modifier = Modifier.widthIn(min = 140.dp)) {
                            Text(action)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { saveState = SaveState.DRAFT_SAVED }) { Text("Save Draft") }
                    Button(onClick = { saveState = SaveState.FINAL_SIGN_OFF }) { Text("Final Sign-off") }
                }

                val saveMessage = when (saveState) {
                    SaveState.DRAFT_SAVED -> "Draft saved"
                    SaveState.UNSAVED_CHANGES -> "Unsaved changes warning"
                    SaveState.FINAL_SIGN_OFF -> "Final sign-off completed"
                }
                Text("Status: $saveMessage")
            }
        }

        Card(Modifier.weight(1.3f).fillMaxSize()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Patient Summary", style = MaterialTheme.typography.titleMedium)
                Text("${selectedPatient.fullName} (${selectedPatient.id})")
                Text("${selectedPatient.age} yrs • ${selectedPatient.sex}")
                Text("Visits: ${selectedPatient.visits}")
                Text("Active diagnosis: ${selectedPatient.activeDiagnosis}")
                Text("Current meds: ${selectedPatient.currentMedications.joinToString()}")
                selectedPatient.assignedWard?.let { Text("Ward: $it") }

                Text("Clinical Timeline", style = MaterialTheme.typography.titleSmall)
                ClinicalTimeline(selectedPatient.timeline)
            }
        }
    }
}

@Composable
private fun ClinicalTimeline(events: List<TimelineEvent>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().height(420.dp)) {
        items(events) { event ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(event.title)
                    Text(event.details)
                    Text("${event.type.name.lowercase()} • ${event.timestamp}")
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
