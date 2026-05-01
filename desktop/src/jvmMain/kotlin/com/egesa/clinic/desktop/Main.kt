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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.WorkflowArea
import java.time.LocalDate

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
                    }
                }
            }
        }
    }
}
