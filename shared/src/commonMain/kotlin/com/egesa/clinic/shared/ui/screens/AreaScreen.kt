package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.egesa.clinic.shared.ui.components.*
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.theme.*

// ── Router ─────────────────────────────────────────────────────────────────────

@Composable
fun AreaScreen(area: WorkflowArea, session: SessionState, localRepository: LocalRepository) {
    when (area) {
        WorkflowArea.ADMIN        -> AdminScreen(localRepository)
        WorkflowArea.RECEPTION    -> ReceptionScreen()
        WorkflowArea.WARDS        -> WardsScreen()
        WorkflowArea.CONSULTATION -> ClinicalProgramsScreen()
        else                      -> PlaceholderScreen(area)
    }
}

// ── Loading indicator ──────────────────────────────────────────────────────────

@Composable
private fun ScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = Navy800, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            Text("Loading…", style = MaterialTheme.typography.bodySmall, color = Slate400)
        }
    }
}

// ── Dashboard ──────────────────────────────────────────────────────────────────

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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Good morning, ${session.fullName.split(" ").first()}",
                    style = MaterialTheme.typography.headlineMedium, color = Slate900,
                )
                Text("Here's what's happening at the clinic today.",
                    style = MaterialTheme.typography.bodyMedium, color = Slate500)
            }
        }

        // KPI grid — 3-column chunked Rows (no nested LazyGrid!)
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
                bottlenecks!!.forEachIndexed { i, cell ->
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
                    if (i < bottlenecks!!.lastIndex) HorizontalDivider(color = Slate100)
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
                    SectionHeader("Shift Handoff — Day")
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

// ── Reception ──────────────────────────────────────────────────────────────────

@Composable
private fun ReceptionScreen() {
    var patients by remember { mutableStateOf<List<Patient>?>(null) }
    var queue    by remember { mutableStateOf<List<QueueItem>?>(null) }

    LaunchedEffect(Unit) {
        queue    = FakeRepository.getQueue()
        patients = FakeRepository.getPatients()
    }

    if (patients == null) { ScreenLoading(); return }

    Row(Modifier.fillMaxSize()) {
        // ── Queue column ────────────────────────────────────────────────────
        Column(
            Modifier.width(300.dp).fillMaxHeight().background(White).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(
                "Reception Queue",
                action = {
                    Button(
                        onClick        = {},
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors         = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape          = MaterialTheme.shapes.small,
                    ) { Text("+ Register", fontSize = 12.sp) }
                }
            )

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
                                    Text("${entry.waitMinutes} min  •  ${entry.patientId}",
                                        style = MaterialTheme.typography.bodySmall, color = Slate500)
                                }
                            }
                            StatusBadge(level)
                        }
                    }
                }
            }
        }

        // Vertical divider
        Box(Modifier.width(1.dp).fillMaxHeight().background(Slate200))

        // ── Patient list ────────────────────────────────────────────────────
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding      = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("All Patients")
                    Text("${patients!!.size} total", style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
            }
            items(patients!!) { p -> PatientCard(p) }
        }
    }
}

// ── Wards ──────────────────────────────────────────────────────────────────────

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
        // ── Overview stats ──────────────────────────────────────────────────
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

        // ── Alerts row ──────────────────────────────────────────────────────
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

        // ── Bed board — 2-column grid using chunked Rows ────────────────────
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

        // ── Ward census table ───────────────────────────────────────────────
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

        // ── ATD flow ────────────────────────────────────────────────────────
        atd?.let { a ->
            item {
                SectionHeader("Admission / Transfer / Discharge")
                Spacer(Modifier.height(10.dp))
                ClinicCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Assign Bed", style = MaterialTheme.typography.titleSmall, color = Slate700)
                            Text("${a.selectedPatientId} → ${a.selectedBed}", style = MaterialTheme.typography.bodyMedium, color = Navy800)
                            Spacer(Modifier.height(4.dp))
                            Text("Transfer Ward", style = MaterialTheme.typography.titleSmall, color = Slate700)
                            Text("${a.selectedPatientId} → ${a.transferWard}", style = MaterialTheme.typography.bodyMedium, color = Navy800)
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
                                        if (done) Text("✓", fontSize = 10.sp, color = White, fontWeight = FontWeight.Bold)
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

        // ── Nursing tasks ───────────────────────────────────────────────────
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
                    Text("${bed.ward}  ·  ${bed.roomBed}", style = MaterialTheme.typography.titleSmall, color = Slate800)
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
private fun ConsultationScreen() {
    var patients   by remember { mutableStateOf<List<Patient>?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        patients   = FakeRepository.getPatients()
        selectedId = patients?.firstOrNull()?.id
    }

    if (patients == null) { ScreenLoading(); return }

    val selected = patients!!.find { it.id == selectedId } ?: patients!!.first()

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
                    onClick  = { selectedId = p.id },
                )
            }
        }

        // Vertical divider
        Box(Modifier.width(1.dp).fillMaxHeight().background(Slate200))

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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(acuityLevel(selected.acuity))
                        TextBadge(selected.status, Slate600, Slate100)
                    }
                }
            }

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
                    if (selected.currentMedications.isEmpty()) {
                        Text("No active medications", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    } else {
                        selected.currentMedications.forEachIndexed { i, med ->
                            Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Teal100),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("Rx", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Teal700)
                                }
                                Text(med, style = MaterialTheme.typography.bodyMedium, color = Slate700)
                            }
                            if (i < selected.currentMedications.lastIndex) HorizontalDivider(color = Slate100)
                        }
                    }
                }
            }

            // Clinical timeline
            item {
                SectionHeader("Clinical Timeline")
                Spacer(Modifier.height(8.dp))
                if (selected.timeline.isEmpty()) {
                    ClinicCard(Modifier.fillMaxWidth()) {
                        Text("No timeline events recorded", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                } else {
                    selected.timeline.forEachIndexed { i, event ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Teal600))
                                if (i < selected.timeline.lastIndex) {
                                    Box(Modifier.width(1.dp).height(40.dp).background(Slate200))
                                }
                            }
                            Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(event.title, style = MaterialTheme.typography.titleSmall, color = Slate900)
                                Text(event.details, style = MaterialTheme.typography.bodySmall, color = Slate500)
                                Text("${event.type.name.lowercase()}  ·  ${event.timestamp}",
                                    style = MaterialTheme.typography.labelSmall, color = Slate400)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Placeholder ────────────────────────────────────────────────────────────────

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
