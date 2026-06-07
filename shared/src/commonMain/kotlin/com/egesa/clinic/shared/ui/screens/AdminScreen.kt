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
import com.egesa.clinic.shared.ui.components.ClinicDropdownField
import com.egesa.clinic.shared.ui.components.DestructiveTextButton
import com.egesa.clinic.shared.ui.components.ModuleHeader
import com.egesa.clinic.shared.ui.components.QuickActionButton
import com.egesa.clinic.shared.ui.theme.Navy800
import com.egesa.clinic.shared.ui.theme.Slate500
import com.egesa.clinic.shared.ui.theme.Slate600
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun AdminScreen(localRepository: LocalRepository) {
    var staffList by remember { mutableStateOf(emptyList<StaffMember>()) }
    var auditTrail by remember { mutableStateOf(emptyList<AuditEvent>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffMember?>(null) }
    var staffToDelete by remember { mutableStateOf<StaffMember?>(null) }
    val scope = rememberCoroutineScope()

    fun refreshStaff() {
        scope.launch {
            staffList = localRepository.getAllStaff()
        }
    }

    LaunchedEffect(Unit) {
        staffList = localRepository.getAllStaff()
    }

    var reportType by remember { mutableStateOf("moh204_monthly_opd") }
    var fromMonth by remember { mutableStateOf("") }
    var toMonth by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }
    var exportFormat by remember { mutableStateOf("json") }
    var generatedPath by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ModuleHeader(
            title = "Staff Management",
            subtitle = "Add, edit, and remove clinic staff accounts.",
            primaryActionLabel = "New staff",
            onPrimaryAction = { showAddDialog = true },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Monthly MOH Reporting", style = MaterialTheme.typography.titleMedium)
                ClinicDropdownField(
                    value = reportType,
                    onValueChange = { reportType = it },
                    label = "Report view",
                    options = listOf(
                        "moh204_monthly_opd",
                        "moh405_monthly_anc",
                        "moh333_monthly_maternity",
                        "moh361b_monthly_ccc",
                        "moh272_273_monthly_ncd"
                    ),
                    displayFormatter = { code ->
                        when (code) {
                            "moh204_monthly_opd" -> "MOH 204 (Monthly OPD)"
                            "moh405_monthly_anc" -> "MOH 405 (Monthly ANC)"
                            "moh333_monthly_maternity" -> "MOH 333 (Monthly Maternity)"
                            "moh361b_monthly_ccc" -> "MOH 361B (Monthly CCC)"
                            "moh272_273_monthly_ncd" -> "MOH 272/273 (Monthly NCD)"
                            else -> code
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fromMonth, onValueChange = { fromMonth = it }, label = { Text("From (YYYY-MM)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = toMonth, onValueChange = { toMonth = it }, label = { Text("To (YYYY-MM)") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClinicDropdownField(
                        value = department,
                        onValueChange = { department = it },
                        label = "Department filter",
                        options = listOf("", "OPD", "ANC", "Maternity", "CCC", "NCD", "HTS", "Pharmacy", "Billing", "Admin"),
                        modifier = Modifier.weight(1f),
                        displayFormatter = { if (it.isBlank()) "All" else it }
                    )
                    ClinicDropdownField(
                        value = program,
                        onValueChange = { program = it },
                        label = "Program filter",
                        options = listOf("", "OPD", "ANC", "Maternity", "CCC", "NCD", "HTS"),
                        modifier = Modifier.weight(1f),
                        displayFormatter = { if (it.isBlank()) "All" else it }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton("JSON", onClick = { exportFormat = "json" })
                    QuickActionButton("CSV", onClick = { exportFormat = "csv" })
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

        Text("Staff directory", style = MaterialTheme.typography.titleMedium, color = Navy800)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(staffList) { staff ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(staff.fullName, style = MaterialTheme.typography.titleSmall)
                            Text("${staff.role.name} · ${staff.department}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                            Text(staff.id, style = MaterialTheme.typography.labelSmall, color = Slate600)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { editingStaff = staff }) {
                                Text("Edit", color = Navy800)
                            }
                            DestructiveTextButton(
                                label = "Delete",
                                onClick = { staffToDelete = staff },
                                enabled = staff.role != UserRole.ADMIN,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        StaffFormDialog(
            title = "Add New Staff",
            onDismiss = { showAddDialog = false },
            onConfirm = { newStaff, pin ->
                scope.launch {
                    localRepository.insertStaff(newStaff, pin)
                    auditTrail = listOf(
                        AuditEvent(
                            user = "Admin",
                            action = "CREATE_STAFF",
                            module = "STAFF_MANAGEMENT",
                            timestamp = Clock.System.now().toString(),
                            contextReference = newStaff.id,
                            permission = Permission.STAFF_MANAGE,
                        )
                    ) + auditTrail
                    showAddDialog = false
                    refreshStaff()
                }
            },
        )
    }

    editingStaff?.let { staff ->
        StaffFormDialog(
            title = "Edit Staff",
            initialStaff = staff,
            requirePin = false,
            onDismiss = { editingStaff = null },
            onConfirm = { updatedStaff, pin ->
                scope.launch {
                    localRepository.insertStaff(updatedStaff, pin.ifBlank { null })
                    auditTrail = listOf(
                        AuditEvent(
                            user = "Admin",
                            action = "UPDATE_STAFF",
                            module = "STAFF_MANAGEMENT",
                            timestamp = Clock.System.now().toString(),
                            contextReference = updatedStaff.id,
                            permission = Permission.STAFF_MANAGE,
                        )
                    ) + auditTrail
                    editingStaff = null
                    refreshStaff()
                }
            },
        )
    }

    staffToDelete?.let { staff ->
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            title = { Text("Delete staff member?") },
            text = { Text("Remove ${staff.fullName} (${staff.id}) from this device. This cannot be undone locally.") },
            confirmButton = {
                DestructiveTextButton(
                    label = "Delete",
                    onClick = {
                        scope.launch {
                            localRepository.deleteStaff(staff.id)
                            auditTrail = listOf(
                                AuditEvent(
                                    user = "Admin",
                                    action = "DELETE_STAFF",
                                    module = "STAFF_MANAGEMENT",
                                    timestamp = Clock.System.now().toString(),
                                    contextReference = staff.id,
                                    permission = Permission.STAFF_MANAGE,
                                )
                            ) + auditTrail
                            staffToDelete = null
                            refreshStaff()
                        }
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = { staffToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun StaffFormDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (StaffMember, String) -> Unit,
    initialStaff: StaffMember? = null,
    requirePin: Boolean = true,
) {
    var name by remember(initialStaff) { mutableStateOf(initialStaff?.fullName ?: "") }
    var department by remember(initialStaff) { mutableStateOf(initialStaff?.department ?: "") }
    var role by remember(initialStaff) { mutableStateOf(initialStaff?.role ?: UserRole.RECEPTIONIST) }
    var id by remember(initialStaff) { mutableStateOf(initialStaff?.id ?: "") }
    var pin by remember { mutableStateOf("") }
    val isEdit = initialStaff != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { if (!isEdit) id = it },
                    label = { Text("Staff ID") },
                    enabled = !isEdit,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                ClinicDropdownField(
                    value = department,
                    onValueChange = { department = it },
                    label = "Department",
                    options = listOf("OPD", "ANC", "Maternity", "CCC", "NCD", "HTS", "Pharmacy", "Billing", "Admin"),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text(if (requirePin) "Initial PIN" else "New PIN (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
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
            Button(
                onClick = {
                    if (id.isBlank() || name.isBlank()) return@Button
                    if (requirePin && pin.isBlank()) return@Button
                    onConfirm(StaffMember(id, name, role, department), pin)
                },
                enabled = id.isNotBlank() && name.isNotBlank() && (!requirePin || pin.isNotBlank()),
            ) {
                Text(if (isEdit) "Save changes" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** @deprecated Use [StaffFormDialog] */
@Composable
fun AddStaffDialog(onDismiss: () -> Unit, onConfirm: (StaffMember, String) -> Unit) {
    StaffFormDialog(title = "Add New Staff", onDismiss = onDismiss, onConfirm = onConfirm)
}
