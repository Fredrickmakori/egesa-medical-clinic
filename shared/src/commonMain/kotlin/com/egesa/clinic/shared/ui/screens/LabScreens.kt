package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.Permission
import com.egesa.clinic.shared.data.LabWorklistQuery
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.domain.LabOrder
import com.egesa.clinic.shared.domain.LabOrderItem
import com.egesa.clinic.shared.domain.LabOrderStatus
import com.egesa.clinic.shared.domain.LabPriority
import com.egesa.clinic.shared.domain.LabResult
import com.egesa.clinic.shared.ui.navigation.SessionState
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun LabWorklistScreen(
    localRepository: LocalRepository,
    session: SessionState,
    onSelectOrder: (LabOrder) -> Unit = {},
) {
    var orders by remember { mutableStateOf<List<LabOrder>>(emptyList()) }
    var statusFilter by remember { mutableStateOf(LabOrderStatus.ORDERED.name) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            orders = localRepository.getLabWorklist(
                LabWorklistQuery(
                    department = "LAB",
                    status = runCatching { LabOrderStatus.valueOf(statusFilter) }.getOrDefault(LabOrderStatus.ORDERED),
                )
            )
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Lab worklist", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = statusFilter,
            onValueChange = { statusFilter = it.uppercase() },
            label = { Text("Status filter") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { refresh() }) { Text("Refresh") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orders) { order ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Order ${order.id} · ${order.status}", style = MaterialTheme.typography.titleMedium)
                        Text("Patient: ${order.patientId} · Priority: ${order.priority}")
                        Text("Tests: ${order.items.joinToString { it.testName }}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onSelectOrder(order) }) { Text("Open") }
                            if (session.hasPermission(Permission.LAB_RESULT_MANAGE) && order.status == LabOrderStatus.ORDERED) {
                                Button(onClick = {
                                    scope.launch {
                                        localRepository.updateLabOrderStatus(order.id, LabOrderStatus.SAMPLE_COLLECTED, session.staffId)
                                        refresh()
                                    }
                                }) { Text("Collect sample") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabResultEntryScreen(
    localRepository: LocalRepository,
    session: SessionState,
    order: LabOrder?,
) {
    var selectedOrder by remember { mutableStateOf(order) }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Lab result entry", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(selectedOrder?.id.orEmpty(), { }, readOnly = true, label = { Text("Order ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value, { value = it }, label = { Text("Result value") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f))
            OutlinedTextField(reference, { reference = it }, label = { Text("Reference range") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(comment, { comment = it }, label = { Text("Comment") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val current = selectedOrder ?: return@Button
                val item = current.items.firstOrNull() ?: return@Button
                scope.launch {
                    localRepository.saveLabResults(
                        orderId = current.id,
                        results = listOf(
                            LabResult(
                                id = "LRES-${Clock.System.now().toEpochMilliseconds()}",
                                orderId = current.id,
                                orderItemId = item.id,
                                patientId = current.patientId,
                                testId = item.testId,
                                testCode = item.testCode,
                                testName = item.testName,
                                value = value,
                                unit = unit.ifBlank { null },
                                referenceRange = reference.ifBlank { null },
                                comment = comment.ifBlank { null },
                                enteredBy = session.staffId,
                                enteredAt = Clock.System.now().toString(),
                                createdAt = Clock.System.now().toString(),
                                updatedAt = Clock.System.now().toString(),
                            )
                        ),
                        actorId = session.staffId,
                    )
                    localRepository.updateLabOrderStatus(current.id, LabOrderStatus.IN_PROCESS, session.staffId)
                    localRepository.updateLabOrderStatus(current.id, LabOrderStatus.VERIFIED, session.staffId)
                    localRepository.updateLabOrderStatus(current.id, LabOrderStatus.REPORTED, session.staffId)
                    status = "Results saved and order progressed to REPORTED."
                }
            },
            enabled = session.hasPermission(Permission.LAB_RESULT_MANAGE),
        ) { Text("Save Result") }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LabOrderDetailsScreen(
    localRepository: LocalRepository,
    patientId: String?,
) {
    var orders by remember { mutableStateOf<List<LabOrder>>(emptyList()) }
    LaunchedEffect(patientId) {
        orders = if (patientId.isNullOrBlank()) emptyList() else localRepository.getLabOrdersForPatient(patientId)
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Lab order details", style = MaterialTheme.typography.headlineSmall)
        if (orders.isEmpty()) {
            Text("No lab orders for this patient.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(orders) { order ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Order ${order.id}", style = MaterialTheme.typography.titleMedium)
                            Text("Status: ${order.status}")
                            Text("Verified: ${order.verifiedBy ?: "-"} @ ${order.verifiedAt ?: "-"}")
                            Text("Reported: ${order.reportedBy ?: "-"} @ ${order.reportedAt ?: "-"}")
                            Spacer(Modifier.height(4.dp))
                            order.items.forEach { item ->
                                Text("- ${item.testName} (${item.billingCode}) KES ${item.price}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabModuleScreen(localRepository: LocalRepository, session: SessionState) {
    var selectedOrder by remember { mutableStateOf<LabOrder?>(null) }
    LaunchedEffect(Unit) {
        if (session.hasPermission(Permission.CONSULTATION_WRITE)) {
            val now = Clock.System.now().toString()
            val orderId = "LAB-${Clock.System.now().toEpochMilliseconds()}"
            localRepository.createLabOrder(
                LabOrder(
                    id = orderId,
                    patientId = "PT-DEMO",
                    encounterId = "ENC-DEMO",
                    orderedBy = session.staffId,
                    department = "LAB",
                    priority = LabPriority.ROUTINE,
                    items = listOf(
                        LabOrderItem(
                            id = "LIT-$orderId",
                            orderId = orderId,
                            testId = "CBC",
                            testCode = "CBC",
                            testName = "Complete Blood Count",
                            billingCode = "LAB-CBC",
                            price = 400.0,
                            orderedAt = now,
                            updatedAt = now,
                        )
                    ),
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            LabWorklistScreen(localRepository, session) { selectedOrder = it }
        }
        Column(Modifier.weight(1f)) {
            if (session.role.name == "DOCTOR") {
                LabOrderDetailsScreen(localRepository, selectedOrder?.patientId)
            } else {
                LabResultEntryScreen(localRepository, session, selectedOrder)
            }
        }
    }
}
