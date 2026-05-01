package com.egesa.clinic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.egesa.clinic.shared.BottleneckCell
import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.WorkflowArea
import java.time.LocalDate

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

        if (activeArea == WorkflowArea.BILLING) {
            BillingPanel()
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
private fun ReceptionWorkspace(state: HospitalState) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1.25f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QueueSummaryCards()
            QueueBoard(state.allPatients())
            QueueActionsPanel()
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NewPatientRegistrationForm()
            QuickTriage()
        }
    }
}

@Composable
private fun BillingPanel() {
    var category by remember { mutableStateOf(BillingCategory.SERVICES) }
    var phone by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(TxStatus.PENDING) }

    val billItems = remember(category) {
        if (category == BillingCategory.SERVICES) {
            listOf(BillItem("Consultation", 1, 1000.0), BillItem("Lab Panel", 1, 2500.0), BillItem("Ultrasound", 1, 3000.0))
        } else {
            listOf(BillItem("Amoxicillin", 2, 250.0), BillItem("Pain Relief", 1, 450.0), BillItem("Syringe", 3, 50.0))
        }
    }
    val total = billItems.sumOf { it.qty * it.unitPrice }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TabRow(selectedTabIndex = BillingCategory.entries.indexOf(category)) {
            BillingCategory.entries.forEach { entry ->
                Tab(selected = category == entry, onClick = { category = entry }, text = { Text(entry.title) })
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bill Items", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Item"); Text("Qty"); Text("Unit"); Text("Subtotal")
                }
                billItems.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.name)
                        Text(item.qty.toString())
                        Text("KES ${item.unitPrice}")
                        Text("KES ${item.qty * item.unitPrice}")
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Amount", fontWeight = FontWeight.Bold)
                Text("KES $total", style = MaterialTheme.typography.headlineSmall)
            }
        }

        val validPhone = phone.matches(Regex("2547\\d{8}"))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(12) },
            label = { Text("Phone Number (2547XXXXXXXX)") },
            isError = phone.isNotBlank() && !validPhone,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                status = when (category) {
                    BillingCategory.SERVICES -> TxStatus.SUCCESS
                    BillingCategory.PHARMACY -> TxStatus.FAILED
                }
            }, enabled = validPhone) {
                Text("Send STK Push")
            }
            OutlinedButton(onClick = {
                status = if (status == TxStatus.PENDING) TxStatus.CANCELLED else TxStatus.PENDING
            }) {
                Text("Refresh / Query")
            }
            StatusBadge(status)
        }

        Text(
            when (status) {
                TxStatus.PENDING -> "Pending prompt: Awaiting patient authorization on phone."
                TxStatus.SUCCESS -> "Success: M-Pesa receipt number QW12RT34 confirmed."
                TxStatus.FAILED -> "Failed: Insufficient funds on subscriber account."
                TxStatus.CANCELLED -> "Cancelled: Patient dismissed STK prompt."
            }
        )

        TransactionHistoryPanel(category)
private fun QueueSummaryCards() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard("Waiting", "12", Color(0xFF1E88E5), Modifier.weight(1f))
        SummaryCard("Priority", "3", Color(0xFFFB8C00), Modifier.weight(1f))
        SummaryCard("Avg Wait", "18 min", Color(0xFF43A047), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Box(
                Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(value, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QueueBoard(patients: List<Patient>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Live Patient Queue", style = MaterialTheme.typography.titleMedium)
            QueueTableHeader()

            when {
                patients.isEmpty() -> EmptyState("No patients in queue.")
                patients.any { it.fullName.contains("Error", true) } -> ErrorState("Queue feed unavailable. Retry in 30 seconds.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(patients) { patient ->
                        QueueRow(patient)
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTableHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF3F6FA), RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text("Token", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text("Patient", Modifier.weight(2f), fontWeight = FontWeight.SemiBold)
        Text("Status", Modifier.weight(2f), fontWeight = FontWeight.SemiBold)
        Text("Priority", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QueueRow(patient: Patient) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(patient.id, Modifier.weight(1f))
        Text(patient.fullName, Modifier.weight(2f))
        Text(patient.status, Modifier.weight(2f))
        AssistChip(onClick = {}, label = { Text(if (patient.age > 55) "High" else "Normal") }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QueueActionsPanel() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Queue Actions", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Assign Doctor") }
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Send to Consultation") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Escalate Priority") }
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Print Token") }
            }
        }
    }
}

@Composable
private fun NewPatientRegistrationForm() {
    var fullName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var insuranceProvider by remember { mutableStateOf("") }
    var consentAccepted by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("New Patient Registration", style = MaterialTheme.typography.titleMedium)

            SectionLabel("Demographics")
            RequiredField("Full Name *", fullName, { fullName = it }, submitAttempted)
            RequiredField("Date of Birth *", dateOfBirth, { dateOfBirth = it }, submitAttempted)
            OutlinedTextField(value = "Female", onValueChange = {}, label = { Text("Sex") }, modifier = Modifier.fillMaxWidth())

            SectionLabel("Contact")
            RequiredField("Primary Phone *", phone, { phone = it }, submitAttempted)
            OutlinedTextField(value = "Kakamega", onValueChange = {}, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())

            SectionLabel("Emergency Contact")
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Contact Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Relationship") }, modifier = Modifier.fillMaxWidth())

            SectionLabel("Insurance / Payment")
            RequiredField("Insurance Provider *", insuranceProvider, { insuranceProvider = it }, submitAttempted)
            OutlinedTextField(value = "Cash", onValueChange = {}, label = { Text("Preferred Payment Method") }, modifier = Modifier.fillMaxWidth())

            SectionLabel("Consent")
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = consentAccepted, onClick = { consentAccepted = true })
                Text("Consent granted for treatment and data use")
            }
            if (submitAttempted && !consentAccepted) {
                Text("Consent is required.", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { submitAttempted = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Register Patient")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun RequiredField(label: String, value: String, onChange: (String) -> Unit, attempted: Boolean) {
    val isError = attempted && value.isBlank()
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )
        if (isError) {
            Text("This field is required.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun QuickTriage() {
    var vitals by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Routine") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Quick Triage", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = vitals,
                onValueChange = { vitals = it },
                label = { Text("Vitals (BP, Temp, Pulse)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Routine", "Urgent", "Critical").forEach { level ->
                    AssistChip(onClick = { urgency = level }, label = { Text(level) })
                }
            }
            Text("Selected urgency: $urgency", style = MaterialTheme.typography.labelMedium)

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Triage Notes") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray)
    }
}

@Composable
private fun StatusBadge(status: TxStatus) {
    val (label, tint) = when (status) {
        TxStatus.PENDING -> "Pending" to Color(0xFFE6A700)
        TxStatus.SUCCESS -> "Success" to Color(0xFF2E7D32)
        TxStatus.FAILED -> "Failed" to Color(0xFFC62828)
        TxStatus.CANCELLED -> "Cancelled" to Color(0xFF757575)
    }
    AssistChip(onClick = {}, label = { Text(label) })
    Box(Modifier.background(tint).padding(4.dp))
}

@Composable
private fun TransactionHistoryPanel(activeCategory: BillingCategory) {
    var patientFilter by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf("2026-05") }

    val history = listOf(
        Transaction("Jane Doe", BillingCategory.SERVICES, LocalDate.of(2026, 5, 1), 6500.0, TxStatus.SUCCESS, "RY1244XY"),
        Transaction("John Kimani", BillingCategory.PHARMACY, LocalDate.of(2026, 5, 1), 1100.0, TxStatus.FAILED, reason = "Insufficient funds"),
        Transaction("Faith Auma", BillingCategory.SERVICES, LocalDate.of(2026, 4, 29), 4000.0, TxStatus.CANCELLED, reason = "Prompt dismissed")
    )

    val filtered = history.filter {
        it.category == activeCategory &&
            (patientFilter.isBlank() || it.patient.contains(patientFilter, ignoreCase = true)) &&
            it.date.toString().startsWith(dateFilter)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Transaction History", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = patientFilter, onValueChange = { patientFilter = it }, label = { Text("Patient") })
                OutlinedTextField(value = dateFilter, onValueChange = { dateFilter = it }, label = { Text("Date (YYYY-MM)") })
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered) { tx ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text("${tx.date} • ${tx.patient} • KES ${tx.amount}")
                            Text("${tx.category.title} • ${tx.status}")
                            tx.receiptNo?.let { Text("Receipt: $it") }
                            tx.reason?.let { Text("Reason: $it") }
                        }
private fun ErrorState(message: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun AdminMetrics(state: HospitalState) {
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
    PatientList(state.allPatients(), "No admin records found.")
}

@Composable
private fun PatientList(patients: List<Patient>, emptyMessage: String) {
    if (patients.isEmpty()) {
        EmptyState(emptyMessage)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(patients) { patient ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${patient.id} • ${patient.fullName}")
                    Text("${patient.age} yrs, ${patient.sex}")
                    Text("Status: ${patient.status}")
                    patient.assignedWard?.let { Text("Ward: $it") }
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

@Composable
private fun BreadcrumbHeader(path: List<String>) {
    Text(path.joinToString(" > "), style = MaterialTheme.typography.titleMedium)
}
