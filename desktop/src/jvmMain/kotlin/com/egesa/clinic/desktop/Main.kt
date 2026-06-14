package com.egesa.clinic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.egesa.clinic.shared.*
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.DocumentCaptureResult
import com.egesa.clinic.shared.ui.navigation.navItemsFor
import java.awt.FileDialog
import java.awt.Frame
import java.time.LocalDate

enum class OrdersBoardTab { LAB, IMAGING, PROCEDURES }
enum class OrderStatus(val label: String) {
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

enum class SaveState { DRAFT_SAVED, UNSAVED_CHANGES, FINAL_SIGN_OFF }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopAppShell(state: HospitalState) {
    var currentRole by remember { mutableStateOf(UserRole.ADMIN) }
    val visibleNavItems = navItemsFor(currentRole)
    var activeArea by remember { mutableStateOf(visibleNavItems.first().area) }

    Scaffold(
        topBar = {
            TopAppBar(
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

            Box(Modifier.fillMaxSize()) {
                when (activeArea) {
                    WorkflowArea.CONSULTATION -> ConsultationWorkbench(state)
                    WorkflowArea.WARDS -> WardOperationsScreen(state)
                    else -> DefaultAreaView(state, activeArea)
                }
            }
        }
    }
}

@Composable
private fun DefaultAreaView(state: HospitalState, activeArea: WorkflowArea) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (activeArea == WorkflowArea.ADMIN) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.adminKpis().forEach { metric ->
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(metric.title)
                            Text(metric.value, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }

        Text("All Patients", style = MaterialTheme.typography.titleLarge)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConsultationWorkbench(state: HospitalState) {
    val patients = state.allPatients()
    var filterQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var selectedPatient by remember { mutableStateOf(patients.firstOrNull() ?: Patient("MOCK", "No Patients", 0, "Unknown", "None")) }

    var saveState by remember { mutableStateOf(SaveState.DRAFT_SAVED) }

    val filteredPatients = patients.filter {
        val queryMatches = filterQuery.isBlank() || it.fullName.contains(filterQuery, true) || it.id.contains(filterQuery, true)
        val statusMatches = statusFilter == "All" || it.status.contains(statusFilter, true)
        queryMatches && statusMatches
    }

    Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        onClick = { selectedPatient = patient },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (patient.id == selectedPatient.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text("${patient.id} • ${patient.fullName}")
                            Text(patient.status)
                        }
                    }
                }
            }
        }
        
        Column(Modifier.weight(1.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Selected Patient: ${selectedPatient.fullName}", style = MaterialTheme.typography.titleLarge)
                    Text("Age: ${selectedPatient.age} | Sex: ${selectedPatient.sex}")
                    Text("Active Diagnosis: ${selectedPatient.status}")
                    
                    Divider()
                    
                    Text("Workbench tools would go here...")
                }
            }
        }
    }
}

@Composable
private fun WardOperationsScreen(state: HospitalState) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
private fun RowScope.OverviewChip(label: String, value: String) {
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
    var saveState by remember { mutableStateOf(SaveState.DRAFT_SAVED) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Bed Board", style = MaterialTheme.typography.titleMedium)
        
        Box(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(bottom = 4.dp)) {
                items(beds) { bed ->
                    Card(Modifier.padding(4.dp)) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${bed.ward} • ${bed.roomBed}", style = MaterialTheme.typography.titleSmall)
                            Text(bed.patientName)
                            Text("Status: ${bed.status}")
                            Text("Acuity: ${bed.acuity}")
                            Text("Isolation: ${bed.isolation ?: "None"}")
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { saveState = SaveState.DRAFT_SAVED }) { Text("Save Draft") }
                                Button(onClick = { saveState = SaveState.FINAL_SIGN_OFF }) { Text("Final") }
                            }
                        }
                    }
                }
            }
        }

        val saveMessage = when (saveState) {
            SaveState.DRAFT_SAVED -> "Draft saved"
            SaveState.UNSAVED_CHANGES -> "Unsaved changes warning"
            SaveState.FINAL_SIGN_OFF -> "Final sign-off completed"
        }
        Text("Status: $saveMessage", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ClinicalTimeline(events: List<TimelineEvent>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        events.forEach { event ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(event.title)
                    Text(event.details)
                    Text("${event.type.name.lowercase()} • ${event.timestamp}")
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
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Shift Handoff Summary", style = MaterialTheme.typography.titleMedium)
            Text("Day Shift", style = MaterialTheme.typography.labelLarge)
            state.shiftHandoffSummary(Shift.DAY).forEach { Text("• $it") }
            Text("Night Shift", style = MaterialTheme.typography.labelLarge)
            state.shiftHandoffSummary(Shift.NIGHT).forEach { Text("• $it") }
        }
    }
}

private object DesktopDocumentCaptureGateway : DocumentCaptureGateway {
    override val canCapturePhoto: Boolean = false
    override val canAttachImage: Boolean = true

    override suspend fun capturePhoto(documentType: String): DocumentCaptureResult =
        DocumentCaptureResult(message = "Camera capture is only available on supported tablet devices.")

    override suspend fun attachImage(documentType: String): DocumentCaptureResult {
        val dialog = FileDialog(null as Frame?, "Attach $documentType image", FileDialog.LOAD)
        dialog.isVisible = true
        val file = dialog.file
        val directory = dialog.directory
        dialog.dispose()
        return if (file == null || directory == null) {
            DocumentCaptureResult(message = "No document image selected.")
        } else {
            DocumentCaptureResult(
                imageUri = java.io.File(directory, file).absolutePath,
                message = "Document image attached. Review or extract document data before registering."
            )
        }
    }

    override suspend fun extractData(imageUri: String, documentType: String): DocumentCaptureResult =
        DocumentCaptureResult(
            imageUri = imageUri,
            message = "Image saved for verification. OCR extraction is ready to connect to the configured OCR service."
        )
}
