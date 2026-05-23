package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.*
import com.egesa.clinic.shared.data.LocalRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun AdminScreen(localRepository: LocalRepository) {
    var staffList by remember { mutableStateOf(emptyList<StaffMember>()) }
    var auditTrail by remember { mutableStateOf(emptyList<AuditEvent>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        staffList = localRepository.getAllStaff()
        // In a real app, this would come from the repository
        // For now, we'll use a local state or fetch from a mock
    }

    var reportType by remember { mutableStateOf("moh204_monthly_opd") }
    var fromMonth by remember { mutableStateOf("") }
    var toMonth by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }
    var exportFormat by remember { mutableStateOf("json") }
    var generatedPath by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Staff Management", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { showAddDialog = true }) {
                Text("Add Staff")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Monthly MOH Reporting", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = reportType, onValueChange = { reportType = it }, label = { Text("Report view") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fromMonth, onValueChange = { fromMonth = it }, label = { Text("From (YYYY-MM)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = toMonth, onValueChange = { toMonth = it }, label = { Text("To (YYYY-MM)") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department filter") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = program, onValueChange = { program = it }, label = { Text("Program filter") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportFormat = "json" }) { Text("JSON") }
                    Button(onClick = { exportFormat = "csv" }) { Text("CSV") }
                    Button(onClick = {
                        generatedPath = buildString {
                            append("/reports/")
                            append(reportType)
                            append("?fromMonth=")
                            append(fromMonth)
                            append("&toMonth=")
                            append(toMonth)
                            if (department.isNotBlank()) append("&department=$department")
                            if (program.isNotBlank()) append("&program=$program")
                            append("&format=$exportFormat")
                        }
                    }) { Text("Generate / Download") }
                }
                if (generatedPath.isNotBlank()) {
                    Text("Use endpoint: $generatedPath", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Audit Trail", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            if (auditTrail.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No audit events recorded", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LazyColumn(Modifier.padding(8.dp)) {
                    items(auditTrail) { event ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${event.action}: ${event.module}", style = MaterialTheme.typography.labelMedium)
                                Text(event.timestamp, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(event.user, style = MaterialTheme.typography.labelSmall)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(staffList) { staff ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(staff.fullName) },
                        supportingContent = { Text("${staff.role} - ${staff.department}") },
                        trailingContent = { Text(staff.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddStaffDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newStaff, pin ->
                scope.launch {
                    localRepository.insertStaff(newStaff, pin)
                    staffList = localRepository.getAllStaff()
                    
                    // Add audit event
                    val event = AuditEvent(
                        user = "Admin", // Should be current user
                        action = "CREATE_STAFF",
                        module = "STAFF_MANAGEMENT",
                        timestamp = Clock.System.now().toString(),
                        contextReference = newStaff.id,
                        permission = Permission.STAFF_MANAGE
                    )
                    auditTrail = listOf(event) + auditTrail

                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun AddStaffDialog(onDismiss: () -> Unit, onConfirm: (StaffMember, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.RECEPTIONIST) }
    var id by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Staff") },
        text = {
            Column {
                OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Staff ID") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
                OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") })
                OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("Initial PIN") })

                Spacer(modifier = Modifier.height(8.dp))
                Text("Role", style = MaterialTheme.typography.labelMedium)
                UserRole.entries.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == r, onClick = { role = r })
                        Text(r.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(StaffMember(id, name, role, department), pin)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
