package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.*
import com.egesa.clinic.shared.data.FakeRepository
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.data.PatientChart
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.DocumentCaptureResult
import com.egesa.clinic.shared.data.EncounterInput
import com.egesa.clinic.shared.data.NoopDocumentCaptureGateway
import com.egesa.clinic.shared.data.PatientDocumentInput
import com.egesa.clinic.shared.data.PatientRegistrationInput
import com.egesa.clinic.shared.data.RegistrationClinicalInput
import com.egesa.clinic.shared.data.ServiceEventInput
import com.egesa.clinic.shared.data.VitalSignsInput
import com.egesa.clinic.shared.data.DiagnosisInput
import com.egesa.clinic.shared.data.MedicationOrderInput
import com.egesa.clinic.shared.data.EncounterOutcomeInput
import com.egesa.clinic.shared.ui.components.*
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

// â”€â”€ Router â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun AreaScreen(
    area: WorkflowArea,
    session: SessionState,
    localRepository: LocalRepository,
    documentCaptureGateway: DocumentCaptureGateway = NoopDocumentCaptureGateway
) {
    if (!RolePermissionMap.hasPermission(session.role, permissionForArea(area))) {
        AccessDeniedScreen(area)
        return
    }

    when (area) {
        WorkflowArea.ADMIN        -> AdminScreen(localRepository)
        WorkflowArea.RECEPTION    -> ReceptionScreen(localRepository, session, documentCaptureGateway)
        WorkflowArea.APPOINTMENTS -> AppointmentsScreen(localRepository, session)
        WorkflowArea.WARDS        -> WardsScreen()
        WorkflowArea.CONSULTATION -> ClinicalProgramsScreen(localRepository, session)
        WorkflowArea.DIAGNOSIS    -> ClinicalProgramsScreen(localRepository, session)
        WorkflowArea.LAB_IMAGING  -> LabImagingScreen()
        WorkflowArea.PHARMACY     -> PharmacyScreen(localRepository, session)
        WorkflowArea.BILLING      -> BillingScreen()
        WorkflowArea.INVENTORY    -> InventoryScreen()
        WorkflowArea.NOTIFICATIONS -> NotificationsScreen()
        WorkflowArea.REPORTS      -> ReportsScreen()
        WorkflowArea.SETTINGS     -> SettingsScreen()
        WorkflowArea.MOH_REPORTS  -> MohReportScreen(localRepository)
        WorkflowArea.DASHBOARD     -> DashboardScreen(session)
    }
}

private fun permissionForArea(area: WorkflowArea): Permission = when (area) {
    WorkflowArea.ADMIN -> Permission.SYSTEM_CONFIG
    WorkflowArea.RECEPTION -> Permission.QUEUE_MANAGE
    WorkflowArea.APPOINTMENTS -> Permission.APPOINTMENT_MANAGE
    WorkflowArea.WARDS -> Permission.WARD_ADMISSION
    WorkflowArea.CONSULTATION -> Permission.CONSULTATION_WRITE
    WorkflowArea.DIAGNOSIS -> Permission.DIAGNOSIS_WRITE
    WorkflowArea.LAB_IMAGING -> Permission.LAB_RESULT_MANAGE
    WorkflowArea.PHARMACY -> Permission.PHARMACY_DISPENSE
    WorkflowArea.BILLING -> Permission.PAYMENT_INITIATE
    WorkflowArea.INVENTORY -> Permission.INVENTORY_MANAGE
    WorkflowArea.NOTIFICATIONS -> Permission.NOTIFICATION_MANAGE
    WorkflowArea.REPORTS -> Permission.AUDIT_VIEW
    WorkflowArea.MOH_REPORTS -> Permission.AUDIT_VIEW
    else -> Permission.PATIENT_READ // Default safe permission
}

@Composable
private fun AccessDeniedScreen(area: WorkflowArea) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Access Denied", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
            Text("You do not have permission to access ${area.name}")
        }
    }
}

// â”€â”€ Loading indicator â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = Navy800, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            Text("Loadingâ€¦", style = MaterialTheme.typography.bodySmall, color = Slate400)
        }
    }
}

// â”€â”€ Dashboard â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun DashboardScreen(session: SessionState) {
    var kpis        by remember { mutableStateOf<List<DashboardMetric>?>(null) }
    var bottlenecks by remember { mutableStateOf<List<BottleneckCell>?>(null) }
    var patients    by remember { mutableStateOf<List<Patient>?>(null) }
    var wardOv      by remember { mutableStateOf<WardOverview?>(null) }
    var handoff     by remember { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(Unit) {
        kpis        = FakeRepository.getKpis()
        bottlenecks = FakeRepository.getBottlenecks()
        patients    = FakeRepository.getPatients()
        wardOv      = FakeRepository.getWardOverview()
        handoff     = FakeRepository.getShiftHandoff(Shift.DAY)
    }

    if (kpis == null) { ScreenLoading(); return }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding      = PaddingValues(vertical = 20.dp),
    ) {
        // Greeting
        item {
            ModuleHeader(
                title = "Command Center",
                subtitle = "Good morning, ${session.fullName.split(" ").first()}. Track patient flow, revenue, wards, stock, and reminders from one console.",
                primaryActionLabel = "Register",
            )
        }

        item {
            ModuleKpiStrip(
                listOf(
                    DashboardMetricUi("Patients today", "${patients?.size ?: 0}", "Active local + server list", Indigo700),
                    DashboardMetricUi("Appointments", "18", "6 waiting confirmation", Sky700),
                    DashboardMetricUi("Lab queue", "9", "3 results ready", Rose700),
                    DashboardMetricUi("M-Pesa", "KES 42k", "Pending reconciliation", Amber700),
                )
            )
        }

        // KPI grid â€” 3-column chunked Rows (no nested LazyGrid!)
        item {
            SectionHeader("Today's Overview")
            Spacer(Modifier.height(10.dp))
            val accentColors = listOf(Navy800, Teal700, StatusInfo, StatusStable, StatusWarning)
            kpis!!.chunked(3).forEachIndexed { rowIdx, row ->
                if (rowIdx > 0) Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { colIdx, m ->
                        MetricCard(
                            title       = m.title,
                            value       = m.value,
                            subtitle    = m.subtitle,
                            accentColor = accentColors[(rowIdx * 3 + colIdx) % accentColors.size],
                            modifier    = Modifier.weight(1f),
                        )
                    }
                    // Fill empty cells in last row
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // Bottleneck heatmap
        item {
            SectionHeader("Workflow Bottlenecks")
            Spacer(Modifier.height(10.dp))
            ClinicCard(Modifier.fillMaxWidth()) {
                val workflowCells = if (bottlenecks!!.isEmpty()) listOf(
                    BottleneckCell("Reception queue", "High", 7),
                    BottleneckCell("Lab results", "Moderate", 9),
                    BottleneckCell("Billing approval", "Low", 4),
                    BottleneckCell("SMS reminders", "Moderate", 12),
                ) else bottlenecks!!
                workflowCells.forEachIndexed { i, cell ->
                    val (fg, bg) = when (cell.severity) {
                        "Critical" -> StatusCritical to Color(0xFFFEE2E2)
                        "High"     -> StatusWarning  to Color(0xFFFEF9C3)
                        "Low"      -> StatusStable   to Color(0xFFD1FAE5)
                        else       -> StatusInfo     to Color(0xFFDBEAFE)
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(color = fg, size = 9)
                            Text(cell.workflowStage, style = MaterialTheme.typography.bodyMedium, color = Slate700)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${cell.pendingCount} pending", style = MaterialTheme.typography.bodySmall, color = Slate500)
                            TextBadge(cell.severity, fg, bg)
                        }
                    }
                    if (i < workflowCells.lastIndex) HorizontalDivider(color = Slate100)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ClinicCard(Modifier.weight(1f)) {
                    SectionHeader("Operational Queues")
                    WorkflowQueueRow("Appointments", "Provider calendar and room allocations", "18 today", Sky700)
                    HorizontalDivider(color = Slate100)
                    WorkflowQueueRow("Lab / Imaging", "Orders awaiting sample/result updates", "9 open", Rose700)
                    HorizontalDivider(color = Slate100)
                    WorkflowQueueRow("Billing", "Invoices, M-Pesa, and insurance follow-up", "KES 42k", Amber700)
                }
                ClinicCard(Modifier.weight(1f)) {
                    SectionHeader("Quick Actions")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickActionButton("Register patient", Modifier.weight(1f))
                        QuickActionButton("Book visit", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickActionButton("Start invoice", Modifier.weight(1f))
                        QuickActionButton("Send SMS", Modifier.weight(1f))
                    }
                }
            }
        }

        // Side-by-side: patients + ward overview
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Patient list (left)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Active Patients",
                        action = {
                            TextButton(onClick = {}) {
                                Text("View all", style = MaterialTheme.typography.labelMedium, color = Navy800)
                            }
                        }
                    )
                    patients?.forEach { PatientCard(it) }
                }

                // Ward overview (right)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Ward Overview")
                    wardOv?.let { ward ->
                        ClinicCard(Modifier.fillMaxWidth()) {
                            // 3 stat tiles
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    Triple("Occupancy",  "${ward.occupancyPercent}%", Navy800),
                                    Triple("Available",  "${ward.bedsAvailable}",     StatusStable),
                                    Triple("Workload",   ward.nurseWorkload.take(7),  StatusInfo),
                                ).forEach { (label, value, color) ->
                                    Column(
                                        Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Slate50).padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = Slate500)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Alerts", style = MaterialTheme.typography.titleSmall, color = Slate700)
                            Spacer(Modifier.height(6.dp))
                            ward.alerts.forEach { alert ->
                                Row(
                                    Modifier.padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment     = Alignment.Top,
                                ) {
                                    StatusDot(StatusWarning, 7, Modifier.padding(top = 5.dp))
                                    Text(alert, style = MaterialTheme.typography.bodySmall, color = Slate600)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    SectionHeader("Shift Handoff â€” Day")
                    ClinicCard(Modifier.fillMaxWidth()) {
                        handoff?.forEach { line ->
                            Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusDot(Navy700, 6, Modifier.padding(top = 5.dp))
                                Text(line, style = MaterialTheme.typography.bodySmall, color = Slate700)
                            }
                        }
                    }
                }
            }
        }
    }
}

// â”€â”€ Reception â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ReceptionScreen(
    localRepository: LocalRepository,
    session: SessionState,
    documentCaptureGateway: DocumentCaptureGateway
) {
    val scope = rememberCoroutineScope()
    var patients by remember { mutableStateOf<List<Patient>?>(null) }
    var queue    by remember { mutableStateOf<List<QueueItem>?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var showRegister by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var busyPatientId by remember { mutableStateOf<String?>(null) }
    var selectedPatientId by remember { mutableStateOf<String?>(null) }
    var selectedChart by remember { mutableStateOf<PatientChart?>(null) }
    var patientSearch by remember { mutableStateOf("") }

    LaunchedEffect(refreshKey) {
        queue    = FakeRepository.getQueue()
        val remotePatients = FakeRepository.getPatients()
        val localPatients = localRepository.getAllPatients()
        patients = (remotePatients + localPatients).distinctBy { it.id }.sortedBy { it.fullName }
        if (selectedPatientId == null) selectedPatientId = patients?.firstOrNull()?.id
    }

    LaunchedEffect(selectedPatientId, refreshKey) {
        selectedChart = selectedPatientId?.let { localRepository.getPatientChart(it) }
    }

    if (patients == null) { ScreenLoading(); return }

    if (showRegister) {
        RegisterPatientDialog(
            documentCaptureGateway = documentCaptureGateway,
            onDismiss = { showRegister = false },
            onRegister = { input, triageLevel, clinical, document ->
                scope.launch {
                    actionError = null
                    actionMessage = null
                    val nowMs = Clock.System.now().toEpochMilliseconds()
                    val encounterId = "REG-$nowMs-${input.id}"
                    runCatching { localRepository.upsertPatient(input) }
                        .onFailure { actionError = "Local save failed: ${it.message ?: "Unknown error"}" }

                    runCatching {
                        localRepository.createEncounter(
                            EncounterInput(
                                encounterId = encounterId,
                                patientId = input.id,
                                encounterDatetime = Clock.System.now().toString(),
                                department = "RECEPTION",
                                visitType = VisitType.OUTPATIENT,
                                providerId = session.staffId,
                                facilityId = "EGESA-CLINIC"
                            )
                        )
                        val bmi = calculateBmi(clinical.weightKg, clinical.heightCm)
                        if (clinical.hasVitals()) {
                            localRepository.upsertVitalSigns(
                                VitalSignsInput(
                                    vitalSignsId = "VIT-$nowMs-${input.id}",
                                    encounterId = encounterId,
                                    weightKg = clinical.weightKg,
                                    heightCm = clinical.heightCm,
                                    bmi = bmi,
                                    temperatureC = clinical.temperatureC,
                                    systolicBp = clinical.systolicBp,
                                    diastolicBp = clinical.diastolicBp,
                                    pulseBpm = clinical.pulseBpm,
                                    respiratoryRate = clinical.respiratoryRate,
                                    spo2Percent = clinical.spo2Percent,
                                    muacCm = clinical.muacCm
                                )
                            )
                        }
                        if (document != null) localRepository.insertPatientDocument(document)
                        localRepository.upsertServiceEvent(
                            ServiceEventInput(
                                serviceEventId = "QUEUE-$nowMs-${input.id}",
                                encounterId = encounterId,
                                program = "RECEPTION",
                                indicatorCategory = "QUEUE_FORWARD",
                                serviceCode = clinical.queueDestination,
                                valueText = listOfNotNull(clinical.queuePriority, clinical.queueNote).joinToString(" - "),
                                eventDatetime = Clock.System.now().toString()
                            )
                        )
                    }.onFailure {
                        actionError = "Registration details saved, but clinical/document capture failed: ${it.message ?: "Unknown error"}"
                    }

                    val result = FakeRepository.registerPatient(input, triageLevel)
                    if (result.isSuccess) {
                        actionMessage = "Registered ${input.fullName} and forwarded to ${clinical.queueDestination}."
                    } else if (actionError == null) {
                        actionError = "API registration failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                    }
                    showRegister = false
                    refreshKey++
                }
            }
        )
    }

    val filteredPatients = patients!!.filter {
        patientSearch.isBlank() ||
            it.fullName.contains(patientSearch, ignoreCase = true) ||
            it.id.contains(patientSearch, ignoreCase = true)
    }

    Row(Modifier.fillMaxSize()) {
        // â”€â”€ Queue column â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Column(
            Modifier.width(300.dp).fillMaxHeight().background(White).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ModuleHeader(
                "Reception",
                "Register, check in, queue, and forward patients into clinical workflows.",
                modifier = Modifier.fillMaxWidth(),
                primaryActionLabel = "Register",
                onPrimaryAction = { showRegister = true },
            )
            ModuleKpiStrip(
                listOf(
                    DashboardMetricUi("Queue", "${queue?.size ?: 0}", "Active waiting", Indigo700),
                    DashboardMetricUi("Patients", "${patients!!.size}", "Registered", Sky700),
                )
            )
            SectionHeader(
                "Live Queue",
                action = {
                    Button(
                        onClick        = { showRegister = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors         = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape          = MaterialTheme.shapes.small,
                    ) { Text("+ Register", fontSize = 12.sp) }
                }
            )

            Text("Signed in: ${session.fullName}", style = MaterialTheme.typography.labelSmall, color = Slate400)
            actionMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = StatusStable) }
            actionError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = StatusCritical) }

            val q = queue ?: emptyList()
            if (q.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Queue is empty", style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
            } else {
                q.forEach { entry ->
                    val level = when (entry.triageLevel) {
                        1    -> AcuityLevel.CRITICAL
                        2    -> AcuityLevel.HIGH
                        4    -> AcuityLevel.LOW
                        else -> AcuityLevel.MODERATE
                    }
                    ClinicCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(entry.name, style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    StatusDot(Slate300, 6)
                                    Text("${entry.waitMinutes} min  â€¢  ${entry.patientId}",
                                        style = MaterialTheme.typography.bodySmall, color = Slate500)
                                }
                            }
                            StatusBadge(level)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    busyPatientId = entry.patientId
                                    actionError = null
                                    actionMessage = null
                                    val result = FakeRepository.checkOutPatient(entry.patientId)
                                    if (result.isSuccess) {
                                        actionMessage = "Checked out ${entry.name}."
                                    } else {
                                        actionError = result.exceptionOrNull()?.message ?: "Check-out failed"
                                    }
                                    busyPatientId = null
                                    refreshKey++
                                }
                            },
                            enabled = busyPatientId != entry.patientId,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(if (busyPatientId == entry.patientId) "Checking out..." else "Check out")
                        }
                    }
                }
            }
        }

        // Vertical divider
        Box(Modifier.width(1.dp).fillMaxHeight().background(Slate200))

        // â”€â”€ Patient list â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Row(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.weight(0.42f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(vertical = 16.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader("All Patients")
                        Text("${patients!!.size} total", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                }
                if (patients!!.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Text("No patients registered yet", style = MaterialTheme.typography.bodyMedium, color = Slate400)
                        }
                    }
                } else {
                    item {
                        ToolbarSearchField(
                            value = patientSearch,
                            onValueChange = { patientSearch = it },
                            placeholder = "Search by name or patient ID",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickActionButton("Check in", Modifier.weight(1f))
                            QuickActionButton("Appointment", Modifier.weight(1f))
                            QuickActionButton("Billing", Modifier.weight(1f))
                        }
                    }
                    items(filteredPatients) { p ->
                        PatientCard(
                            patient = p,
                            selected = p.id == selectedPatientId,
                            onClick = { selectedPatientId = p.id }
                        )
                    }
                }
            }

            Box(Modifier.width(1.dp).fillMaxHeight().background(Slate200))

            PatientChartPanel(
                chart = selectedChart,
                modifier = Modifier.weight(0.58f).fillMaxHeight().padding(horizontal = 20.dp)
            )
        }
    }
}

// â”€â”€ Wards â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PatientChartPanel(chart: PatientChart?, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (chart == null) {
            item {
                ClinicCard(Modifier.fillMaxWidth()) {
                    Text("Select a patient to view their chart.", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                }
            }
            return@LazyColumn
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(chart.patient.fullName, style = MaterialTheme.typography.headlineSmall, color = Slate900)
                    Text("${chart.patient.id}  -  ${chart.patient.age} yrs, ${chart.patient.sex.code}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    StatusBadge(acuityLevel(chart.patient.acuity))
                    TextBadge(chart.patient.status, Slate600, Slate100)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Encounters", "${chart.encounters.size}", modifier = Modifier.weight(1f), accentColor = Navy800)
                MetricCard("Documents", "${chart.documents.size}", modifier = Modifier.weight(1f), accentColor = StatusInfo)
                MetricCard(
                    "Latest Visit",
                    chart.encounters.firstOrNull()?.department ?: "None",
                    subtitle = chart.encounters.firstOrNull()?.encounterDatetime,
                    modifier = Modifier.weight(1.4f),
                    accentColor = Teal700
                )
            }
        }

        item { SectionHeader("Registration Documents") }

        if (chart.documents.isEmpty()) {
            item {
                ClinicCard(Modifier.fillMaxWidth()) {
                    Text("No registration documents captured locally.", style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
            }
        } else {
            items(chart.documents) { document ->
                ClinicCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(document.documentType, style = MaterialTheme.typography.titleSmall, color = Slate900)
                            Text(document.imageUri, style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                        TextBadge(document.verificationStatus, Navy800, Navy50)
                    }
                    Spacer(Modifier.height(8.dp))
                    ChartSection(
                        "Extracted Data",
                        listOfNotNull(
                            document.extractedFullName?.let { "Name: $it" },
                            document.extractedIdentifier?.let { "Identifier: $it" },
                            document.extractedBirthDate?.let { "Birth date: $it" },
                            document.extractedSex?.let { "Sex: $it" },
                            document.extractedGuardianName?.let { "Guardian: $it" },
                            document.notes?.let { "Notes: $it" }
                        )
                    )
                }
            }
        }

        item { SectionHeader("Clinical Chart") }

        if (chart.encounters.isEmpty()) {
            item {
                ClinicCard(Modifier.fillMaxWidth()) {
                    Text("No encounters recorded locally for this patient.", style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
            }
        } else {
            items(chart.encounters) { encounter ->
                ClinicCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(encounter.department, style = MaterialTheme.typography.titleMedium, color = Slate900)
                            Text("${encounter.visitType.code}  -  ${encounter.encounterDatetime}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                        TextBadge(encounter.syncState, Navy800, Navy50)
                    }
                    Spacer(Modifier.height(10.dp))
                    ChartSection("Vitals", encounter.vitals)
                    ChartSection("Diagnoses", encounter.diagnoses)
                    ChartSection("Medications", encounter.medications)
                    ChartSection("Service Events", encounter.serviceEvents)
                    ChartSection(
                        "Outcome",
                        listOfNotNull(
                            encounter.outcome?.let { "Outcome: $it" },
                            encounter.referralTo?.let { "Referral: $it" }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartSection(title: String, values: List<String>) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = Slate500)
    if (values.isEmpty()) {
        Text("None recorded", style = MaterialTheme.typography.bodySmall, color = Slate400)
    } else {
        values.forEach { value ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                StatusDot(Navy200, 5, Modifier.padding(top = 6.dp))
                Text(value, style = MaterialTheme.typography.bodySmall, color = Slate700)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun RegisterPatientDialog(
    documentCaptureGateway: DocumentCaptureGateway,
    onDismiss: () -> Unit,
    onRegister: (PatientRegistrationInput, Int, RegistrationClinicalInput, PatientDocumentInput?) -> Unit
) {
    val dialogScope = rememberCoroutineScope()
    var id by remember { mutableStateOf("PT-${Clock.System.now().toEpochMilliseconds()}") }
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("female") }
    var assignedWard by remember { mutableStateOf("") }
    var roomBed by remember { mutableStateOf("") }
    var acuity by remember { mutableStateOf("Moderate") }
    var isolation by remember { mutableStateOf("") }
    var triageLevel by remember { mutableStateOf("3") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var documentType by remember { mutableStateOf("National ID") }
    var documentPhotoUri by remember { mutableStateOf("") }
    var verificationStatus by remember { mutableStateOf("PENDING_REVIEW") }
    var extractedFullName by remember { mutableStateOf("") }
    var extractedIdentifier by remember { mutableStateOf("") }
    var extractedBirthDate by remember { mutableStateOf("") }
    var extractedSex by remember { mutableStateOf("") }
    var extractedGuardianName by remember { mutableStateOf("") }
    var documentNotes by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var temperatureC by remember { mutableStateOf("") }
    var systolicBp by remember { mutableStateOf("") }
    var diastolicBp by remember { mutableStateOf("") }
    var pulseBpm by remember { mutableStateOf("") }
    var respiratoryRate by remember { mutableStateOf("") }
    var spo2Percent by remember { mutableStateOf("") }
    var muacCm by remember { mutableStateOf("") }
    var queueDestination by remember { mutableStateOf("Triage") }
    var queuePriority by remember { mutableStateOf("Routine") }
    var queueNote by remember { mutableStateOf("") }
    var documentActionMessage by remember { mutableStateOf<String?>(null) }
    var documentActionBusy by remember { mutableStateOf(false) }

    fun applyCaptureResult(result: DocumentCaptureResult) {
        result.imageUri?.let { documentPhotoUri = it }
        result.extractedData.fullName?.let { extractedFullName = it }
        result.extractedData.identifier?.let { extractedIdentifier = it }
        result.extractedData.birthDate?.let { extractedBirthDate = it }
        result.extractedData.sex?.let { extractedSex = it }
        result.extractedData.guardianName?.let { extractedGuardianName = it }
        documentActionMessage = result.message
    }

    fun runDocumentAction(action: suspend () -> DocumentCaptureResult) {
        dialogScope.launch {
            documentActionBusy = true
            runCatching { action() }
                .onSuccess { applyCaptureResult(it) }
                .onFailure { documentActionMessage = it.message ?: "Document action failed." }
            documentActionBusy = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register patient") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    validationError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = StatusCritical)
                        Spacer(Modifier.height(4.dp))
                    }
                    SectionHeader("Patient Details")
                }
                item { OutlinedTextField(id, { id = it }, label = { Text("Patient ID") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(fullName, { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(age, { age = it.filter { ch -> ch.isDigit() } }, label = { Text("Age") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(triageLevel, { triageLevel = it.filter { ch -> ch.isDigit() }.take(1) }, label = { Text("Triage") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(sex, { sex = it }, label = { Text("Sex") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(assignedWard, { assignedWard = it }, label = { Text("Ward") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(roomBed, { roomBed = it }, label = { Text("Bed") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(acuity, { acuity = it }, label = { Text("Acuity") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(isolation, { isolation = it }, label = { Text("Isolation") }, modifier = Modifier.weight(1f))
                    }
                }
                item { SectionHeader("Document Verification") }
                item {
                    documentActionMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(documentType, { documentType = it }, label = { Text("Document type") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(verificationStatus, { verificationStatus = it }, label = { Text("Status") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(documentPhotoUri, { documentPhotoUri = it }, label = { Text("Photo URI or file path") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                runDocumentAction {
                                    documentCaptureGateway.capturePhoto(documentType.ifBlank { "Document" })
                                }
                            },
                            enabled = !documentActionBusy && documentCaptureGateway.canCapturePhoto,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                        ) {
                            Text("Take photo")
                        }
                        OutlinedButton(
                            onClick = {
                                runDocumentAction {
                                    documentCaptureGateway.attachImage(documentType.ifBlank { "Document" })
                                }
                            },
                            enabled = !documentActionBusy && documentCaptureGateway.canAttachImage,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Attach image")
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            runDocumentAction {
                                documentCaptureGateway.extractData(
                                    imageUri = documentPhotoUri,
                                    documentType = documentType.ifBlank { "Document" }
                                )
                            }
                        },
                        enabled = !documentActionBusy && documentPhotoUri.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Extract from image")
                    }
                }
                item { OutlinedTextField(extractedFullName, { extractedFullName = it }, label = { Text("Extracted full name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(extractedIdentifier, { extractedIdentifier = it }, label = { Text("Extracted ID") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(extractedBirthDate, { extractedBirthDate = it }, label = { Text("Birth date") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(extractedSex, { extractedSex = it }, label = { Text("Extracted sex") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(extractedGuardianName, { extractedGuardianName = it }, label = { Text("Guardian") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(documentNotes, { documentNotes = it }, label = { Text("Document notes") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedButton(
                        onClick = {
                            if (extractedIdentifier.isNotBlank()) id = extractedIdentifier.trim()
                            if (extractedFullName.isNotBlank()) fullName = extractedFullName.trim()
                            if (extractedSex.isNotBlank()) sex = extractedSex.trim()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply extracted data")
                    }
                }
                item { SectionHeader("Vitals at Registration") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(weightKg, { weightKg = it.decimalInput() }, label = { Text("Weight kg") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(heightCm, { heightCm = it.decimalInput() }, label = { Text("Height cm") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(temperatureC, { temperatureC = it.decimalInput() }, label = { Text("Temp C") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(muacCm, { muacCm = it.decimalInput() }, label = { Text("MUAC cm") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(systolicBp, { systolicBp = it.digitsOnly() }, label = { Text("BP systolic") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(diastolicBp, { diastolicBp = it.digitsOnly() }, label = { Text("BP diastolic") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(pulseBpm, { pulseBpm = it.digitsOnly() }, label = { Text("Pulse") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(respiratoryRate, { respiratoryRate = it.digitsOnly() }, label = { Text("RR") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(spo2Percent, { spo2Percent = it.decimalInput() }, label = { Text("SpO2") }, modifier = Modifier.weight(1f))
                    }
                }
                item { SectionHeader("Queue Routing") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(queueDestination, { queueDestination = it }, label = { Text("Next step") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(queuePriority, { queuePriority = it }, label = { Text("Priority") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(queueNote, { queueNote = it }, label = { Text("Queue note") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAge = age.toIntOrNull()
                    val parsedTriage = triageLevel.toIntOrNull()?.coerceIn(1, 5) ?: 3
                    when {
                        id.isBlank() -> validationError = "Patient ID is required."
                        fullName.isBlank() -> validationError = "Full name is required."
                        parsedAge == null -> validationError = "Age is required."
                        else -> {
                            val cleanId = id.trim()
                            val clinical = RegistrationClinicalInput(
                                weightKg = weightKg.toDoubleOrNull(),
                                heightCm = heightCm.toDoubleOrNull(),
                                temperatureC = temperatureC.toDoubleOrNull(),
                                systolicBp = systolicBp.toLongOrNull(),
                                diastolicBp = diastolicBp.toLongOrNull(),
                                pulseBpm = pulseBpm.toLongOrNull(),
                                respiratoryRate = respiratoryRate.toLongOrNull(),
                                spo2Percent = spo2Percent.toDoubleOrNull(),
                                muacCm = muacCm.toDoubleOrNull(),
                                queueDestination = queueDestination.ifBlank { "Triage" },
                                queuePriority = queuePriority.ifBlank { "Routine" },
                                queueNote = queueNote.ifBlank { null }
                            )
                            val document = if (documentPhotoUri.isBlank()) {
                                null
                            } else {
                                PatientDocumentInput(
                                    documentId = "DOC-${Clock.System.now().toEpochMilliseconds()}-$cleanId",
                                    patientId = cleanId,
                                    documentType = documentType.ifBlank { "Unknown" },
                                    imageUri = documentPhotoUri.trim(),
                                    verificationStatus = verificationStatus.ifBlank { "PENDING_REVIEW" },
                                    extractedFullName = extractedFullName.ifBlank { null },
                                    extractedIdentifier = extractedIdentifier.ifBlank { null },
                                    extractedBirthDate = extractedBirthDate.ifBlank { null },
                                    extractedSex = extractedSex.ifBlank { null },
                                    extractedGuardianName = extractedGuardianName.ifBlank { null },
                                    notes = documentNotes.ifBlank { null }
                                )
                            }
                            onRegister(
                                PatientRegistrationInput(
                                    id = cleanId,
                                    fullName = fullName.trim(),
                                    age = parsedAge,
                                    sex = sex.toReceptionSex(),
                                    assignedWard = assignedWard.ifBlank { null },
                                    roomBed = roomBed.ifBlank { null },
                                    acuity = acuity.ifBlank { "Moderate" },
                                    isolation = isolation.ifBlank { null }
                                ),
                                parsedTriage,
                                clinical,
                                document
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun String.toReceptionSex(): Sex = when (trim().lowercase()) {
    "m", "male" -> Sex.MALE
    "f", "female" -> Sex.FEMALE
    "intersex" -> Sex.INTERSEX
    else -> Sex.UNKNOWN
}

private fun String.digitsOnly(): String = filter { it.isDigit() }

private fun String.decimalInput(): String = filterIndexed { index, ch ->
    ch.isDigit() || (ch == '.' && indexOf('.') == index)
}

private fun RegistrationClinicalInput.hasVitals(): Boolean =
    listOf(weightKg, heightCm, temperatureC, systolicBp, diastolicBp, pulseBpm, respiratoryRate, spo2Percent, muacCm)
        .any { it != null }

private fun calculateBmi(weightKg: Double?, heightCm: Double?): Double? {
    if (weightKg == null || heightCm == null || heightCm <= 0.0) return null
    val meters = heightCm / 100.0
    return kotlin.math.round((weightKg / (meters * meters)) * 10.0) / 10.0
}

@Composable
private fun WardsScreen() {
    var overview by remember { mutableStateOf<WardOverview?>(null) }
    var beds     by remember { mutableStateOf<List<BedCard>?>(null) }
    var tasks    by remember { mutableStateOf<List<NursingTask>?>(null) }
    var census   by remember { mutableStateOf<List<WardCensusRow>?>(null) }
    var atd      by remember { mutableStateOf<AdmissionTransferDischargeState?>(null) }

    LaunchedEffect(Unit) {
        overview = FakeRepository.getWardOverview()
        beds     = FakeRepository.getBedBoard()
        tasks    = FakeRepository.getNursingTasks()
        census   = FakeRepository.getWardCensus()
        atd      = FakeRepository.getAtdState()
    }

    if (beds == null) { ScreenLoading(); return }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding      = PaddingValues(vertical = 20.dp),
    ) {
        // â”€â”€ Overview stats â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        item {
            SectionHeader("Ward Operations")
            Spacer(Modifier.height(10.dp))
            overview?.let { ov ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple("Occupancy",   "${ov.occupancyPercent}%", Navy800),
                        Triple("Beds Free",   "${ov.bedsAvailable}",     StatusStable),
                        Triple("Nurse Ratio", ov.nurseWorkload,          StatusInfo),
                    ).forEach { (t, v, c) ->
                        MetricCard(title = t, value = v, accentColor = c, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // â”€â”€ Alerts row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        overview?.let { ov ->
            if (ov.alerts.isNotEmpty()) {
                item {
                    ClinicCard(Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(StatusWarning)
                            Text("Alerts", style = MaterialTheme.typography.titleSmall, color = Slate700)
                        }
                        Spacer(Modifier.height(8.dp))
                        ov.alerts.forEach { alert ->
                            Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                StatusDot(StatusWarning, 6, Modifier.padding(top = 5.dp))
                                Text(alert, style = MaterialTheme.typography.bodySmall, color = Slate600)
                            }
                        }
                    }
                }
            }
        }

        // â”€â”€ Bed board â€” 2-column grid using chunked Rows â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        item {
            SectionHeader("Bed Board",
                action = {
                    TextButton(onClick = {}) {
                        Text("Manage", style = MaterialTheme.typography.labelMedium, color = Navy800)
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            // Chunked Rows avoid nested lazy layout crash
            beds!!.chunked(2).forEach { row ->
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { bed ->
                        BedCell(bed, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // â”€â”€ Ward census table â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        item {
            SectionHeader("Ward Census")
            Spacer(Modifier.height(10.dp))
            ClinicCard(Modifier.fillMaxWidth()) {
                // Header row
                Row(Modifier.fillMaxWidth().background(Slate50).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    listOf("Ward", "Occupied", "Total", "High Acuity", "Isolation")
                        .forEachIndexed { i, h ->
                            Text(
                                h,
                                style    = MaterialTheme.typography.labelMedium,
                                color    = Slate500,
                                modifier = if (i == 0) Modifier.weight(1.5f) else Modifier.weight(1f),
                            )
                        }
                }
                HorizontalDivider(color = Slate200)
                census?.forEachIndexed { i, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(row.ward, style = MaterialTheme.typography.bodyMedium, color = Slate800, modifier = Modifier.weight(1.5f))
                        Text("${row.occupiedBeds}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${row.totalBeds}", style = MaterialTheme.typography.bodySmall, color = Slate500, modifier = Modifier.weight(1f))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (row.highAcuityCount > 0) StatusDot(StatusWarning, 7)
                            Text("${row.highAcuityCount}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (row.isolationCount > 0) StatusDot(StatusWarning, 7)
                            Text("${row.isolationCount}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (i < (census?.lastIndex ?: 0)) HorizontalDivider(color = Slate100)
                }
            }
        }

        // â”€â”€ ATD flow â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        atd?.let { a ->
            item {
                SectionHeader("Admission / Transfer / Discharge")
                Spacer(Modifier.height(10.dp))
                ClinicCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Assign Bed", style = MaterialTheme.typography.titleSmall, color = Slate700)
                            Text("${a.selectedPatientId} â†’ ${a.selectedBed}", style = MaterialTheme.typography.bodyMedium, color = Navy800)
                            Spacer(Modifier.height(4.dp))
                            Text("Transfer Ward", style = MaterialTheme.typography.titleSmall, color = Slate700)
                            Text("${a.selectedPatientId} â†’ ${a.transferWard}", style = MaterialTheme.typography.bodyMedium, color = Navy800)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Discharge Checklist", style = MaterialTheme.typography.titleSmall, color = Slate700)
                            a.dischargeChecklist.forEach { (item, done) ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                                            .background(if (done) StatusStable else Slate200),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (done) Text("âœ“", fontSize = 10.sp, color = White, fontWeight = FontWeight.Bold)
                                    }
                                    Text(item, style = MaterialTheme.typography.bodySmall,
                                        color = if (done) Slate600 else Slate400)
                                }
                            }
                        }
                    }
                }
            }
        }

        // â”€â”€ Nursing tasks â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        item {
            SectionHeader("Nursing Tasks")
            Spacer(Modifier.height(10.dp))
            ClinicCard(Modifier.fillMaxWidth()) {
                tasks?.forEachIndexed { i, task ->
                    val isHigh = task.priority == "High"
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(if (isHigh) StatusCritical else StatusInfo)
                            Column {
                                Text("${task.type}: ${task.detail}", style = MaterialTheme.typography.bodyMedium, color = Slate800)
                                Text("Due ${task.due}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            }
                        }
                        TextBadge(
                            task.priority,
                            if (isHigh) StatusCritical else StatusInfo,
                            if (isHigh) Color(0xFFFEE2E2) else Color(0xFFDBEAFE),
                        )
                    }
                    if (i < (tasks?.lastIndex ?: 0)) HorizontalDivider(color = Slate100)
                }
            }
        }
    }
}

@Composable
private fun BedCell(bed: BedCard, modifier: Modifier = Modifier) {
    val acuity = acuityLevel(bed.acuity)
    val leftBarColor = when (acuity) {
        AcuityLevel.CRITICAL -> StatusCritical
        AcuityLevel.HIGH     -> StatusWarning
        AcuityLevel.LOW,
        AcuityLevel.STABLE   -> StatusStable
        else                 -> StatusInfo
    }
    Card(
        modifier  = modifier,
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = White),
        border    = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(leftBarColor))
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${bed.ward}  Â·  ${bed.roomBed}", style = MaterialTheme.typography.titleSmall, color = Slate800)
                    StatusBadge(acuity)
                }
                Text(bed.patientName, style = MaterialTheme.typography.bodyMedium, color = Slate700)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(Slate300, 5)
                    Text(bed.status, style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
                bed.isolation?.let {
                    TextBadge("Isolation: $it", StatusWarning, Color(0xFFFEF9C3))
                }
            }
        }
    }
}


// ── Consultation ───────────────────────────────────────────────────────────────

@Composable
private fun ConsultationScreen(localRepository: LocalRepository, session: SessionState) {
    var patients   by remember { mutableStateOf<List<Patient>?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showConsultForm by remember { mutableStateOf(false) }

    // Form states
    var weightKg by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var temperatureC by remember { mutableStateOf("") }
    var systolicBp by remember { mutableStateOf("") }
    var diastolicBp by remember { mutableStateOf("") }
    var pulseBpm by remember { mutableStateOf("") }
    var respiratoryRate by remember { mutableStateOf("") }
    var spo2Percent by remember { mutableStateOf("") }
    var muacCm by remember { mutableStateOf("") }

    var subjectiveNotes by remember { mutableStateOf("") }
    var objectiveNotes by remember { mutableStateOf("") }

    var diagnosisText by remember { mutableStateOf("") }
    var diagnosisCode by remember { mutableStateOf("") }

    var medName by remember { mutableStateOf("") }
    var medDose by remember { mutableStateOf("") }
    var medFreq by remember { mutableStateOf("") }
    var medDuration by remember { mutableStateOf("") }

    var outcome by remember { mutableStateOf("DISCHARGED") }
    var referralTo by remember { mutableStateOf("") }

    var validationError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var chart by remember { mutableStateOf<PatientChart?>(null) }

    LaunchedEffect(Unit) {
        patients   = localRepository.getAllPatients()
        selectedId = patients?.firstOrNull()?.id
    }

    LaunchedEffect(selectedId) {
        selectedId?.let { id ->
            chart = localRepository.getPatientChart(id)
        }
    }

    if (patients == null) { ScreenLoading(); return }

    val selected = patients!!.find { it.id == selectedId } ?: patients!!.firstOrNull()

    Row(Modifier.fillMaxSize()) {
        // ── Patient list ────────────────────────────────────────────────────
        LazyColumn(
            Modifier.width(280.dp).fillMaxHeight().background(White).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding      = PaddingValues(bottom = 16.dp),
        ) {
            item { SectionHeader("Patients  (${patients!!.size})") }
            items(patients!!) { p ->
                PatientCard(
                    patient  = p,
                    selected = p.id == selectedId,
                    onClick  = { 
                        selectedId = p.id 
                        showConsultForm = false
                    },
                )
            }
        }

        // Vertical divider
        Box(Modifier.width(1.dp).fillMaxHeight().background(Slate200))

        if (selected == null) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("No patients registered yet. Please go to Reception to add patients.", style = MaterialTheme.typography.bodyMedium, color = Slate500)
            }
            return@Row
        }

        // ── Encounter panel ─────────────────────────────────────────────────
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding      = PaddingValues(vertical = 20.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(selected.fullName, style = MaterialTheme.typography.headlineSmall, color = Slate900)
                        Text("${selected.id}  ·  ${selected.age} yrs, ${selected.sex.code}",
                            style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(acuityLevel(selected.acuity))
                        TextBadge(selected.status, Slate600, Slate100)
                        if (!showConsultForm) {
                            Button(
                                onClick = { showConsultForm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                            ) {
                                Text("New Consultation")
                            }
                        }
                    }
                }
            }

            if (showConsultForm) {
                // ── Consultation Form Card ──
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("New Consultation Record", style = MaterialTheme.typography.titleLarge, color = Navy800, fontWeight = FontWeight.Bold)
                            
                            if (validationError.isNotBlank()) {
                                Text(validationError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            // 1. Vitals Entry
                            Text("1. Patient Vitals", style = MaterialTheme.typography.titleMedium, color = Slate800, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(weightKg, { weightKg = it.decimalInput() }, label = { Text("Weight kg") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(heightCm, { heightCm = it.decimalInput() }, label = { Text("Height cm") }, modifier = Modifier.weight(1f))
                                
                                val weightVal = weightKg.toDoubleOrNull()
                                val heightVal = heightCm.toDoubleOrNull()
                                val bmiVal = calculateBmi(weightVal, heightVal)
                                val bmiDisplay = bmiVal?.toString() ?: "N/A"
                                
                                Card(
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Calculated BMI: $bmiDisplay", style = MaterialTheme.typography.bodyMedium, color = Slate700, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(temperatureC, { temperatureC = it.decimalInput() }, label = { Text("Temp °C") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(muacCm, { muacCm = it.decimalInput() }, label = { Text("MUAC cm") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(spo2Percent, { spo2Percent = it.decimalInput() }, label = { Text("SpO2 %") }, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(systolicBp, { systolicBp = it.digitsOnly() }, label = { Text("BP Systolic") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(diastolicBp, { diastolicBp = it.digitsOnly() }, label = { Text("BP Diastolic") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(pulseBpm, { pulseBpm = it.digitsOnly() }, label = { Text("Pulse bpm") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(respiratoryRate, { respiratoryRate = it.digitsOnly() }, label = { Text("RR /min") }, modifier = Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = Slate200)

                            // 2. SOAP Notes
                            Text("2. Consultation Notes (SOAP)", style = MaterialTheme.typography.titleMedium, color = Slate800, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(subjectiveNotes, { subjectiveNotes = it }, label = { Text("Subjective (Symptoms, patient history)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(objectiveNotes, { objectiveNotes = it }, label = { Text("Objective (Clinical findings, physical assessment)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = Slate200)

                            // 3. Diagnosis
                            Text("3. Diagnosis (ICD-10)", style = MaterialTheme.typography.titleMedium, color = Slate800, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(diagnosisText, { diagnosisText = it }, label = { Text("Diagnosis text") }, modifier = Modifier.weight(2f))
                                OutlinedTextField(diagnosisCode, { diagnosisCode = it }, label = { Text("ICD-10 Code") }, modifier = Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = Slate200)

                            // 4. Prescriptions
                            Text("4. Medication Order (Rx)", style = MaterialTheme.typography.titleMedium, color = Slate800, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(medName, { medName = it }, label = { Text("Drug name") }, modifier = Modifier.weight(2f))
                                OutlinedTextField(medDose, { medDose = it }, label = { Text("Dose (e.g. 500mg)") }, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(medFreq, { medFreq = it }, label = { Text("Frequency (e.g. 3x daily)") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(medDuration, { medDuration = it }, label = { Text("Duration (e.g. 5 days)") }, modifier = Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = Slate200)

                            // 5. Outcome & Referral
                            Text("5. Outcome & Disposition", style = MaterialTheme.typography.titleMedium, color = Slate800, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(outcome, { outcome = it.uppercase() }, label = { Text("Outcome: DISCHARGED, REFERRED, ADMITTED") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(referralTo, { referralTo = it }, label = { Text("Referral to facility / department") }, modifier = Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(8.dp))

                            // Action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showConsultForm = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        if (diagnosisText.isBlank() && medName.isBlank() && subjectiveNotes.isBlank()) {
                                            validationError = "Please enter either clinical notes, diagnosis, or medication prescription to save."
                                            return@Button
                                        }
                                        scope.launch {
                                            val now = Clock.System.now()
                                            val encId = "ENC-${now.toEpochMilliseconds()}"
                                            val encounterTime = now.toString()

                                            // 1. Create Encounter
                                            localRepository.createEncounter(
                                                EncounterInput(
                                                    encounterId = encId,
                                                    patientId = selected.id,
                                                    encounterDatetime = encounterTime,
                                                    department = "Consultation",
                                                    visitType = VisitType.OUTPATIENT,
                                                    providerId = session.staffId,
                                                    facilityId = "EGESA-CLINIC"
                                                )
                                            )

                                            // 2. Insert Vitals if any are filled
                                            val weightVal = weightKg.toDoubleOrNull()
                                            val heightVal = heightCm.toDoubleOrNull()
                                            val bmiVal = calculateBmi(weightVal, heightVal)
                                            if (weightVal != null || heightVal != null || temperatureC.toDoubleOrNull() != null || 
                                                systolicBp.toLongOrNull() != null || diastolicBp.toLongOrNull() != null || 
                                                pulseBpm.toLongOrNull() != null || respiratoryRate.toLongOrNull() != null || 
                                                spo2Percent.toDoubleOrNull() != null || muacCm.toDoubleOrNull() != null) {
                                                localRepository.upsertVitalSigns(
                                                    VitalSignsInput(
                                                        vitalSignsId = "VS-${now.toEpochMilliseconds()}",
                                                        encounterId = encId,
                                                        weightKg = weightVal,
                                                        heightCm = heightVal,
                                                        bmi = bmiVal,
                                                        temperatureC = temperatureC.toDoubleOrNull(),
                                                        systolicBp = systolicBp.toLongOrNull(),
                                                        diastolicBp = diastolicBp.toLongOrNull(),
                                                        pulseBpm = pulseBpm.toLongOrNull(),
                                                        respiratoryRate = respiratoryRate.toLongOrNull(),
                                                        spo2Percent = spo2Percent.toDoubleOrNull(),
                                                        muacCm = muacCm.toDoubleOrNull(),
                                                        recordedAt = encounterTime
                                                    )
                                                )
                                            }

                                            // 3. Insert Diagnosis if filled
                                            if (diagnosisText.isNotBlank()) {
                                                localRepository.upsertDiagnosis(
                                                    DiagnosisInput(
                                                        diagnosisId = "DX-${now.toEpochMilliseconds()}",
                                                        encounterId = encId,
                                                        diagnosisText = diagnosisText.trim(),
                                                        isPrimary = true,
                                                        codeSystem = "ICD-10",
                                                        diagnosisCode = diagnosisCode.trim().ifBlank { null }
                                                    )
                                                )
                                            }

                                            // 4. Insert Medication if filled
                                            if (medName.isNotBlank()) {
                                                localRepository.upsertMedicationOrder(
                                                    MedicationOrderInput(
                                                        medicationOrderId = "MED-${now.toEpochMilliseconds()}",
                                                        encounterId = encId,
                                                        medicationName = medName.trim(),
                                                        dose = medDose.trim().ifBlank { null },
                                                        route = "Oral",
                                                        frequency = medFreq.trim().ifBlank { null },
                                                        duration = medDuration.trim().ifBlank { null },
                                                        instructions = "Prescribed at Consultation"
                                                    )
                                                )
                                            }

                                            // 5. Insert Outcome
                                            localRepository.upsertEncounterOutcome(
                                                EncounterOutcomeInput(
                                                    outcomeId = "OUT-${now.toEpochMilliseconds()}",
                                                    encounterId = encId,
                                                    disposition = when (outcome.uppercase()) {
                                                        "ADMITTED" -> Disposition.ADMITTED
                                                        "TRANSFERRED" -> Disposition.TRANSFERRED
                                                        "REFERRED" -> Disposition.REFERRED
                                                        "DECEASED" -> Disposition.DECEASED
                                                        else -> Disposition.DISCHARGED
                                                    },
                                                    referralTo = referralTo.trim().ifBlank { null },
                                                    admitted = outcome.equals("ADMITTED", ignoreCase = true),
                                                    dischargeNotes = subjectiveNotes.trim().ifBlank { null }
                                                )
                                            )

                                            // 6. Insert Service Event for reporting pipeline
                                            localRepository.upsertServiceEvent(
                                                ServiceEventInput(
                                                    serviceEventId = "SVC-${now.toEpochMilliseconds()}",
                                                    encounterId = encId,
                                                    program = "OPD",
                                                    indicatorCategory = "OPD_VISIT",
                                                    serviceCode = diagnosisText.trim().ifBlank { "OPD_VISIT" },
                                                    valueText = subjectiveNotes.trim().ifBlank { null },
                                                    eventDatetime = encounterTime
                                                )
                                            )

                                            // Refresh screen & exit form
                                            patients = localRepository.getAllPatients()
                                            selectedId?.let { id ->
                                                chart = localRepository.getPatientChart(id)
                                            }
                                            showConsultForm = false
                                            validationError = ""

                                            // Reset fields
                                            weightKg = ""; heightCm = ""; temperatureC = ""
                                            systolicBp = ""; diastolicBp = ""; pulseBpm = ""
                                            respiratoryRate = ""; spo2Percent = ""; muacCm = ""
                                            subjectiveNotes = ""; objectiveNotes = ""
                                            diagnosisText = ""; diagnosisCode = ""
                                            medName = ""; medDose = ""; medFreq = ""; medDuration = ""
                                            outcome = "DISCHARGED"; referralTo = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("Save Consultation")
                                }
                            }
                        }
                    }
                }
            } else {
                // Stats row
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("Visits", "${selected.visits}", modifier = Modifier.weight(1f), accentColor = Navy800)
                        MetricCard(
                            "Active Diagnosis",
                            selected.activeDiagnosis.ifEmpty { "None recorded" },
                            modifier    = Modifier.weight(2f),
                            accentColor = Teal700,
                        )
                        selected.assignedWard?.let { ward ->
                            MetricCard("Ward / Bed", "$ward  ${selected.roomBed ?: ""}".trim(),
                                modifier = Modifier.weight(1f), accentColor = StatusInfo)
                        }
                    }
                }

                // Medications
                item {
                    SectionHeader("Current Medications")
                    Spacer(Modifier.height(8.dp))
                    ClinicCard(Modifier.fillMaxWidth()) {
                        val currentMeds = chart?.encounters?.flatMap { it.medications }?.distinct() ?: emptyList()
                        if (currentMeds.isEmpty()) {
                            Text("No active medications", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        } else {
                            currentMeds.forEachIndexed { i, med ->
                                Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Teal100),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("Rx", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Teal700)
                                    }
                                    Text(med, style = MaterialTheme.typography.bodyMedium, color = Slate700)
                                }
                                if (i < currentMeds.lastIndex) HorizontalDivider(color = Slate100)
                            }
                        }
                    }
                }

                // Clinical timeline
                item {
                    SectionHeader("Clinical Timeline")
                    Spacer(Modifier.height(8.dp))
                    val encounters = chart?.encounters ?: emptyList()
                    if (encounters.isEmpty()) {
                        ClinicCard(Modifier.fillMaxWidth()) {
                            Text("No timeline events recorded", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                    } else {
                        encounters.forEachIndexed { i, enc ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Teal600))
                                    if (i < encounters.lastIndex) {
                                        Box(Modifier.width(1.dp).height(80.dp).background(Slate200))
                                    }
                                }
                                Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${enc.department} Encounter", style = MaterialTheme.typography.titleSmall, color = Slate900)
                                    if (enc.vitals.isNotEmpty()) {
                                        Text("Vitals: " + enc.vitals.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = Slate600)
                                    }
                                    if (enc.diagnoses.isNotEmpty()) {
                                        Text("Diagnoses: " + enc.diagnoses.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = Slate700, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (enc.medications.isNotEmpty()) {
                                        Text("Prescriptions: " + enc.medications.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = Teal700)
                                    }
                                    enc.outcome?.let { outcomeVal ->
                                        Text("Outcome: $outcomeVal" + (enc.referralTo?.let { ref -> " (Referral to $ref)" } ?: ""), style = MaterialTheme.typography.bodySmall, color = Slate500)
                                    }
                                    Text("${enc.visitType.name.lowercase()}  ·  ${enc.encounterDatetime}",
                                        style = MaterialTheme.typography.labelSmall, color = Slate400)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// â”€â”€ Placeholder â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AppointmentsScreen(localRepository: LocalRepository, session: SessionState) {
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var selectedScheduleId by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var showBookDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        var localSchedules = localRepository.getAllSchedules()
        val allPats = (FakeRepository.getPatients() + localRepository.getAllPatients())
            .distinctBy { it.id }
            .sortedBy { it.fullName }
        patients = allPats

        if (localSchedules.isEmpty()) {
            val sch1 = Schedule("SCH-001", "Practitioner", "STAFF-002", "Dr. Achieng (Room 1)", true)
            val sch2 = Schedule("SCH-002", "Practitioner", "STAFF-003", "Clinical Officer (Room 2)", true)
            val sch3 = Schedule("SCH-003", "Practitioner", "STAFF-004", "Dr. Mwangi (Room 3)", true)
            localRepository.insertSchedule(sch1)
            localRepository.insertSchedule(sch2)
            localRepository.insertSchedule(sch3)
            localSchedules = listOf(sch1, sch2, sch3)
        }
        schedules = localSchedules

        val apptsList = mutableListOf<Appointment>()
        for (sch in localSchedules) {
            apptsList.addAll(localRepository.getAppointmentsBySchedule(sch.id))
        }

        if (apptsList.isEmpty() && allPats.isNotEmpty()) {
            val app1 = Appointment(
                id = "APP-001",
                patientId = allPats.first().id,
                scheduleId = "SCH-001",
                slotId = null,
                status = "booked",
                appointmentType = "Routine",
                reason = "General checkup",
                startTime = "2026-06-01 09:00",
                endTime = "2026-06-01 09:30",
                createdAt = Clock.System.now().toString(),
                updatedAt = Clock.System.now().toString()
            )
            val app2 = Appointment(
                id = "APP-002",
                patientId = allPats.getOrNull(1)?.id ?: allPats.first().id,
                scheduleId = "SCH-002",
                slotId = null,
                status = "booked",
                appointmentType = "Follow-up",
                reason = "Follow-up review",
                startTime = "2026-06-01 10:30",
                endTime = "2026-06-01 11:00",
                createdAt = Clock.System.now().toString(),
                updatedAt = Clock.System.now().toString()
            )
            localRepository.insertAppointment(app1)
            localRepository.insertAppointment(app2)
            apptsList.add(app1)
            apptsList.add(app2)
        }

        appointments = apptsList.sortedBy { it.startTime }
        isLoading = false
    }

    ModuleScaffold(
        title = "Appointments",
        subtitle = "Day schedule, provider rooms, confirmations, no-shows, and follow-up bookings.",
        actionLabel = "Book",
        onActionClick = { showBookDialog = true }
    ) {
        if (isLoading) {
            item { ScreenLoading() }
        } else {
            item {
                val todayStr = "2026-06-01"
                val todayAppts = appointments.filter { it.startTime.contains(todayStr) }
                val confirmedCount = todayAppts.filter { it.status == "booked" || it.status == "confirmed" }.size
                val waitingCount = todayAppts.filter { it.status == "waiting" || it.status == "pending" }.size
                val activeProvs = schedules.filter { it.active }.size
                
                ModuleKpiStrip(
                    listOf(
                        DashboardMetricUi("Today's Appts", "${todayAppts.size}", "$confirmedCount confirmed", Indigo700),
                        DashboardMetricUi("Waiting Room", "$waitingCount", "Need consultation", Amber700),
                        DashboardMetricUi("Active Providers", "$activeProvs/3", "On duty", StatusStable),
                        DashboardMetricUi("Total Scheduled", "${appointments.size}", "All-time local queue", Rose700),
                    )
                )
            }
            item { ToolbarSearchField(search, { search = it }, "Search patient name, provider, or reason") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ClinicCard(Modifier.weight(1.2f)) {
                        SectionHeader("Providers & Rooms")
                        Spacer(Modifier.height(8.dp))
                        
                        val isAllSelected = selectedScheduleId == null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedScheduleId = null }
                                .background(if (isAllSelected) Navy50 else Color.Transparent)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusDot(if (isAllSelected) Navy800 else Slate400, size = 10)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "All Providers",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isAllSelected) Navy800 else Slate700,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        HorizontalDivider(color = Slate100)

                        schedules.forEach { sch ->
                            val isSelected = selectedScheduleId == sch.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedScheduleId = sch.id }
                                    .background(if (isSelected) Navy50 else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusDot(if (sch.active) StatusStable else StatusCritical, size = 8)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        sch.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) Navy800 else Slate800,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(sch.actorType, style = MaterialTheme.typography.bodySmall, color = Slate500)
                                }
                            }
                            HorizontalDivider(color = Slate100)
                        }
                    }

                    Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Appointment List")
                        
                        val filteredAppts = appointments.filter { appt ->
                            val provider = schedules.find { it.id == appt.scheduleId }
                            val patient = patients.find { it.id == appt.patientId }
                            
                            val matchesProviderFilter = selectedScheduleId == null || appt.scheduleId == selectedScheduleId
                            
                            val matchesSearch = search.isBlank() ||
                                    (patient?.fullName?.contains(search, ignoreCase = true) == true) ||
                                    (provider?.name?.contains(search, ignoreCase = true) == true) ||
                                    (appt.reason?.contains(search, ignoreCase = true) == true) ||
                                    (appt.patientId.contains(search, ignoreCase = true))
                            
                            matchesProviderFilter && matchesSearch
                        }

                        if (filteredAppts.isEmpty()) {
                            EmptyStateCard(
                                title = "No appointments found",
                                detail = "Use the 'Book' button to schedule an appointment or refine your search filters."
                            )
                        } else {
                            val patientMap = patients.associateBy { it.id }
                            val providerMap = schedules.associateBy { it.id }
                            
                            val headers = listOf("Patient", "Provider", "Time Slot", "Status")
                            val rows = filteredAppts.map { appt ->
                                val patName = patientMap[appt.patientId]?.fullName ?: appt.patientId
                                val provName = providerMap[appt.scheduleId]?.name?.substringBefore(" (") ?: "Unknown"
                                val timeVal = appt.startTime.substringAfter(" ") + " - " + appt.endTime.substringAfter(" ")
                                val dateVal = appt.startTime.substringBefore(" ")
                                
                                listOf(
                                    "$patName\n($dateVal)",
                                    provName,
                                    timeVal,
                                    appt.status.uppercase()
                                )
                            }
                            
                            CompactDataTable(headers = headers, rows = rows)
                        }
                    }
                }
            }
        }
    }

    if (showBookDialog) {
        BookAppointmentDialog(
            patients = patients,
            schedules = schedules,
            localRepository = localRepository,
            onDismiss = { showBookDialog = false },
            onBooked = {
                showBookDialog = false
                refreshTrigger++
            }
        )
    }
}

@Composable
private fun BookAppointmentDialog(
    patients: List<Patient>,
    schedules: List<Schedule>,
    localRepository: LocalRepository,
    onDismiss: () -> Unit,
    onBooked: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedPatientId by remember { mutableStateOf(patients.firstOrNull()?.id ?: "") }
    var selectedScheduleId by remember { mutableStateOf(schedules.firstOrNull()?.id ?: "") }
    var apptType by remember { mutableStateOf("Routine") }
    var reason by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-06-01") }
    
    val timeSlots = listOf(
        "09:00 - 09:30", "09:30 - 10:00", "10:00 - 10:30", "10:30 - 11:00",
        "11:00 - 11:30", "11:30 - 12:00", "14:00 - 14:30", "14:30 - 15:00",
        "15:00 - 15:30", "15:30 - 16:00"
    )
    var selectedTimeSlot by remember { mutableStateOf(timeSlots.first()) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var patientSearchQuery by remember { mutableStateOf("") }
    var showPatientDropdown by remember { mutableStateOf(false) }
    
    var providerSearchQuery by remember { mutableStateOf("") }
    var showProviderDropdown by remember { mutableStateOf(false) }

    val filteredPatients = patients.filter {
        it.fullName.contains(patientSearchQuery, ignoreCase = true) ||
        it.id.contains(patientSearchQuery, ignoreCase = true)
    }

    val selectedPatient = patients.find { it.id == selectedPatientId }
    val selectedSchedule = schedules.find { it.id == selectedScheduleId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book New Appointment") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    validationError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = StatusCritical)
                        Spacer(Modifier.height(4.dp))
                    }
                }
                
                item {
                    Text("Select Patient", style = MaterialTheme.typography.labelMedium, color = Slate700)
                    OutlinedTextField(
                        value = patientSearchQuery.ifEmpty { selectedPatient?.fullName ?: "" },
                        onValueChange = {
                            patientSearchQuery = it
                            showPatientDropdown = true
                        },
                        placeholder = { Text("Type patient name or ID...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { showPatientDropdown = !showPatientDropdown }) {
                                Text(if (showPatientDropdown) "Hide" else "Show All")
                            }
                        }
                    )
                    if (showPatientDropdown) {
                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                            border = BorderStroke(1.dp, Slate200),
                            colors = CardDefaults.cardColors(containerColor = White)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(filteredPatients.size) { index ->
                                    val pat = filteredPatients[index]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPatientId = pat.id
                                                patientSearchQuery = pat.fullName
                                                showPatientDropdown = false
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Text("${pat.fullName} (${pat.id})", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Select Provider", style = MaterialTheme.typography.labelMedium, color = Slate700)
                    OutlinedTextField(
                        value = providerSearchQuery.ifEmpty { selectedSchedule?.name ?: "" },
                        onValueChange = {
                            providerSearchQuery = it
                            showProviderDropdown = true
                        },
                        placeholder = { Text("Type provider name...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { showProviderDropdown = !showProviderDropdown }) {
                                Text(if (showProviderDropdown) "Hide" else "Show All")
                            }
                        }
                    )
                    if (showProviderDropdown) {
                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                            border = BorderStroke(1.dp, Slate200),
                            colors = CardDefaults.cardColors(containerColor = White)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(schedules.size) { index ->
                                    val sch = schedules[index]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedScheduleId = sch.id
                                                providerSearchQuery = sch.name
                                                showProviderDropdown = false
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Text(sch.name, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Appointment Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Select Time Slot", style = MaterialTheme.typography.labelMedium, color = Slate700)
                    timeSlots.chunked(2).forEach { rowSlots ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowSlots.forEach { slot ->
                                val isSelected = slot == selectedTimeSlot
                                Card(
                                    modifier = Modifier.weight(1f).clickable { selectedTimeSlot = slot },
                                    border = BorderStroke(1.dp, if (isSelected) Navy800 else Slate200),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Navy50 else White
                                    )
                                ) {
                                    Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                        Text(slot, style = MaterialTheme.typography.bodySmall, color = if (isSelected) Navy800 else Slate700)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                item {
                    Text("Appointment Type", style = MaterialTheme.typography.labelMedium, color = Slate700)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Routine", "Follow-up", "Specialist").forEach { type ->
                            val isSelected = type == apptType
                            Card(
                                modifier = Modifier.weight(1f).clickable { apptType = type },
                                border = BorderStroke(1.dp, if (isSelected) Navy800 else Slate200),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) Navy50 else White)
                            ) {
                                Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Text(type, style = MaterialTheme.typography.bodySmall, color = if (isSelected) Navy800 else Slate700)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason for booking (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPatientId.isBlank()) {
                        validationError = "Please select a patient"
                        return@Button
                    }
                    if (selectedScheduleId.isBlank()) {
                        validationError = "Please select a provider"
                        return@Button
                    }
                    if (date.isBlank() || !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        validationError = "Please enter date in format YYYY-MM-DD"
                        return@Button
                    }
                    val startStr = selectedTimeSlot.substringBefore(" - ")
                    val endStr = selectedTimeSlot.substringAfter(" - ")
                    val startTime = "$date $startStr"
                    val endTime = "$date $endStr"

                    scope.launch {
                        isSaving = true
                        validationError = null
                        runCatching {
                            val overlap = localRepository.checkOverlappingAppointments(selectedScheduleId, startTime, endTime)
                            if (overlap) {
                                validationError = "⚠️ Double-booking conflict! The provider is already booked at $selectedTimeSlot."
                            } else {
                                val apptId = "APP-${Clock.System.now().toEpochMilliseconds()}"
                                val newAppt = Appointment(
                                    id = apptId,
                                    patientId = selectedPatientId,
                                    scheduleId = selectedScheduleId,
                                    slotId = null,
                                    status = "booked",
                                    appointmentType = apptType,
                                    reason = reason.ifBlank { null },
                                    startTime = startTime,
                                    endTime = endTime,
                                    createdAt = Clock.System.now().toString(),
                                    updatedAt = Clock.System.now().toString()
                                )
                                localRepository.insertAppointment(newAppt)
                                onBooked()
                            }
                        }.onFailure {
                            validationError = it.message ?: "Failed to save appointment"
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Checking..." else "Book")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LabImagingScreen() {
    ModuleScaffold(
        title = "Lab / Imaging",
        subtitle = "Orders, sample status, result review, imaging requests, and patient chart attachments.",
        actionLabel = "New Order",
    ) {
        item {
            ModuleKpiStrip(
                listOf(
                    DashboardMetricUi("Open orders", "9", "3 urgent", Rose700),
                    DashboardMetricUi("Results ready", "3", "Awaiting clinician review", StatusInfo),
                    DashboardMetricUi("Imaging", "4", "2 pending report", Indigo700),
                    DashboardMetricUi("Attached", "12", "This week", StatusStable),
                )
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ClinicCard(Modifier.weight(1f)) {
                    SectionHeader("Orders Queue")
                    WorkflowQueueRow("CBC - PT-1042", "Sample collected 09:15", "Ready", StatusStable)
                    HorizontalDivider(color = Slate100)
                    WorkflowQueueRow("Malaria RDT - PT-1178", "Awaiting sample", "Pending", Amber700)
                    HorizontalDivider(color = Slate100)
                    WorkflowQueueRow("Chest X-ray - PT-1201", "Radiology report pending", "Imaging", Indigo700)
                }
                EmptyStateCard(
                    "Result viewer scaffold",
                    "Selecting an order will show results and attach them to the patient chart once backend result storage is wired.",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BillingScreen() {
    ModuleScaffold(
        title = "Billing / M-Pesa",
        subtitle = "Invoices, payment posting, STK push initiation, callback reconciliation, and insurance claims.",
        actionLabel = "Invoice",
    ) {
        item {
            ModuleKpiStrip(
                listOf(
                    DashboardMetricUi("Open invoices", "14", "KES 42,300", Amber700),
                    DashboardMetricUi("M-Pesa pending", "5", "Awaiting callback", Indigo700),
                    DashboardMetricUi("Paid today", "KES 18k", "9 receipts", StatusStable),
                    DashboardMetricUi("Insurance", "3", "Claims draft", Sky700),
                )
            )
        }
        item {
            CompactDataTable(
                headers = listOf("Invoice", "Patient", "Status", "Amount"),
                rows = listOf(
                    listOf("INV-1008", "PT-1042", "STK sent", "KES 2,500"),
                    listOf("INV-1009", "PT-1178", "Draft", "KES 850"),
                    listOf("INV-1010", "PT-1201", "Insurance", "KES 8,400"),
                ),
            )
        }
    }
}

@Composable
private fun InventoryScreen() {
    ModuleScaffold(
        title = "Inventory",
        subtitle = "Stock levels, reorder alerts, pharmacy issue tracking, supplier movements, and audit trail.",
        actionLabel = "Stock In",
    ) {
        item {
            ModuleKpiStrip(
                listOf(
                    DashboardMetricUi("Low stock", "7", "Needs reorder", Rose700),
                    DashboardMetricUi("Dispensed", "36", "Today", Indigo700),
                    DashboardMetricUi("Adjustments", "2", "Pending approval", Amber700),
                    DashboardMetricUi("Stock value", "KES 510k", "Estimated", StatusStable),
                )
            )
        }
        item {
            CompactDataTable(
                headers = listOf("Item", "On hand", "Reorder", "Status"),
                rows = listOf(
                    listOf("Amoxicillin 500mg", "24", "50", "Low"),
                    listOf("RDT Malaria", "18", "40", "Low"),
                    listOf("Gloves medium", "320", "100", "OK"),
                ),
            )
        }
    }
}

@Composable
private fun NotificationsScreen() {
    ModuleScaffold(
        title = "SMS / Alerts",
        subtitle = "Appointment reminders, lab result alerts, billing prompts, templates, and delivery tracking.",
        actionLabel = "Template",
    ) {
        item {
            ModuleKpiStrip(
                listOf(
                    DashboardMetricUi("Queued", "12", "Reminder batch", Indigo700),
                    DashboardMetricUi("Delivered", "84%", "Last 24 hours", StatusStable),
                    DashboardMetricUi("Failed", "3", "Retry required", Rose700),
                    DashboardMetricUi("Replies", "5", "Confirm/cancel", Sky700),
                )
            )
        }
        item {
            ClinicCard(Modifier.fillMaxWidth()) {
                SectionHeader("Reminder Queue")
                WorkflowQueueRow("PT-1042 appointment reminder", "Tomorrow 09:00 - Dr. Achieng", "Queued", Indigo700)
                HorizontalDivider(color = Slate100)
                WorkflowQueueRow("PT-1178 lab result ready", "Notify patient after clinician review", "Held", Amber700)
                HorizontalDivider(color = Slate100)
                WorkflowQueueRow("INV-1008 payment prompt", "M-Pesa STK follow-up", "Retry", Rose700)
            }
        }
    }
}

@Composable
private fun ReportsScreen() {
    ModuleScaffold(
        title = "Reports",
        subtitle = "Operational reporting for visits, billing, inventory, staff performance, and patient flow.",
        actionLabel = "Export",
    ) {
        item {
            ModuleKpiStrip(
                listOf(
                    DashboardMetricUi("Visits", "128", "This week", Indigo700),
                    DashboardMetricUi("Revenue", "KES 92k", "This week", StatusStable),
                    DashboardMetricUi("No-shows", "6", "This month", Amber700),
                    DashboardMetricUi("Stock alerts", "7", "Open", Rose700),
                )
            )
        }
        item {
            CompactDataTable(
                headers = listOf("Report", "Scope", "State", "Owner"),
                rows = listOf(
                    listOf("Patient visits", "Daily/weekly", "Ready", "Admin"),
                    listOf("Billing summary", "Revenue", "Scaffold", "Finance"),
                    listOf("Inventory alerts", "Stock", "Scaffold", "Pharmacy"),
                    listOf("Staff performance", "Operations", "Scaffold", "Admin"),
                ),
            )
        }
    }
}

@Composable
private fun SettingsScreen() {
    ModuleScaffold(
        title = "Settings",
        subtitle = "Facility profile, departments, permissions, device setup, sync configuration, and integration readiness.",
        actionLabel = "Configure",
    ) {
        item {
            ModuleKpiStrip(
                listOf(
                    DashboardMetricUi("Facility", "Egesa", "Clinic profile", Indigo700),
                    DashboardMetricUi("Devices", "3", "Login-aware clients", Sky700),
                    DashboardMetricUi("Sync", "Ready", "Local queue monitored", StatusStable),
                    DashboardMetricUi("Integrations", "2", "M-Pesa and reports", Amber700),
                )
            )
        }
        item {
            CompactDataTable(
                headers = listOf("Setting", "Area", "State", "Owner"),
                rows = listOf(
                    listOf("Role permissions", "Security", "Active", "Admin"),
                    listOf("Department codes", "Clinical", "Scaffold", "Admin"),
                    listOf("M-Pesa callback", "Billing", "Server-ready", "Finance"),
                    listOf("SMS provider", "Notifications", "Scaffold", "Admin"),
                ),
            )
        }
    }
}

@Composable
private fun ModuleScaffold(
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        content = {
            item { ModuleHeader(title, subtitle, primaryActionLabel = actionLabel, onPrimaryAction = onActionClick) }
            content()
        }
    )
}

@Composable
fun PlaceholderScreen(area: WorkflowArea) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Navy50),
                contentAlignment = Alignment.Center,
            ) {
                Text(area.name.take(2), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy700)
            }
            Text(area.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall, color = Slate900)
            Text("Coming soon.", style = MaterialTheme.typography.bodyMedium, color = Slate400)
        }
    }
}
