package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.StaffMember
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.data.LocalRepository

@Composable
fun AdminScreen(localRepository: LocalRepository) {
    var staffList by remember { mutableStateOf(localRepository.getAllStaff()) }
    var showAddDialog by remember { mutableStateOf(false) }

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

        LazyColumn {
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
                localRepository.insertStaff(newStaff, pin)
                staffList = localRepository.getAllStaff()
                showAddDialog = false
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
