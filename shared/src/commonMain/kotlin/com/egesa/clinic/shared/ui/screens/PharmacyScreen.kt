package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.data.Medication
import com.egesa.clinic.shared.data.MedicationLookup
import com.egesa.clinic.shared.data.InventoryItem
import com.egesa.clinic.shared.data.SupabasePharmacyRepository
import com.egesa.clinic.shared.domain.Prescription
import com.egesa.clinic.shared.ui.navigation.SessionState
import kotlinx.coroutines.launch

/**
 * Enhanced Pharmacy management screen with real Supabase data integration.
 * Handles prescription dispensing, inventory management, medical supplies, and search.
 */
@Composable
fun PharmacyScreen(
    localRepository: LocalRepository,
    session: SessionState,
    supabasePharmacyRepo: SupabasePharmacyRepository? = null
) {
    var activeTab by remember { mutableStateOf(0) }
    var medications by remember { mutableStateOf<List<MedicationLookup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Load data on compose
    LaunchedEffect(Unit) {
        patients = localRepository.getAllPatients()
        if (supabasePharmacyRepo != null) {
            isLoading = true
            try {
                medications = supabasePharmacyRepo.getMedicationLookupList()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val tabs = listOf("Inventory", "Pending Dispensal", "Medical Supplies", "Low Stock")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            "Pharmacy Management",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search medications...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        // Tabs
        TabRow(selectedTabIndex = activeTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Content area
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            when (activeTab) {
                0 -> MedicationInventoryTab(
                    medications = medications.filter { med ->
                        searchQuery.isEmpty() || med.medication_name.contains(searchQuery, ignoreCase = true)
                    },
                    onDispense = { med -> /* Handle dispense */ }
                )
                1 -> PendingDispensingTab(patients = patients)
                2 -> MedicalSuppliesTab()
                3 -> LowStockTab(medications = medications.filter { it.quantity_available <= 20 })
            }
        }
    }
}

/**
 * Tab showing current medication inventory with search and lookup from Supabase.
 */
@Composable
private fun MedicationInventoryTab(
    medications: List<MedicationLookup>,
    onDispense: (MedicationLookup) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Available Medications (${medications.size})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(medications) { med ->
                MedicationInventoryCard(med, onDispense)
            }
        }
    }
}

/**
 * Card showing single medication with availability and dispense option.
 */
@Composable
private fun MedicationInventoryCard(
    med: MedicationLookup,
    onDispense: (MedicationLookup) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                med.quantity_available == 0 -> MaterialTheme.colorScheme.errorContainer
                med.quantity_available <= 10 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        med.medication_name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (med.generic_name != null) {
                        Text(
                            "Generic: ${med.generic_name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }
                // Availability badge
                if (med.quantity_available > 0) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "In Stock",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Dosage and Stock info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Strength: ${med.strength ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                    Text(
                        "Form: ${med.form ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Available: ${med.quantity_available}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (med.quantity_available > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    if (med.quantity_available == 0) {
                        Text(
                            "OUT OF STOCK",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Dispensing button
            if (med.quantity_available > 0) {
                Button(
                    onClick = { onDispense(med) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Dispense", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Tab for prescriptions pending dispensing.
 */
@Composable
private fun PendingDispensingTab(patients: List<Patient>) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Prescriptions Awaiting Dispensing",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (patients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text("No pending prescriptions", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(patients) { patient ->
                    PendingPrescriptionCard(patient)
                }
            }
        }
    }
}

/**
 * Card for a pending prescription.
 */
@Composable
private fun PendingPrescriptionCard(patient: Patient) {
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(patient.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Patient ID: ${patient.id}", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier =Modifier.weight(1f), onClick = {}) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Dispense")
                }
                Button(modifier = Modifier.weight(1f), onClick = {}) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Print")
                }
            }
        }
    }
}

/**
 * Tab for medical supplies management.
 */
@Composable
private fun MedicalSuppliesTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Medical Supplies Inventory",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Integrate with Supabase medical_supplies tables for supplies like gloves, gauze, syringes, etc.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Tab showing medications below reorder level.
 */
@Composable
private fun LowStockTab(medications: List<MedicationLookup>) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Low Stock Medications (${medications.size})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (medications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("All stocks are at healthy levels", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(medications) { med ->
                    LowStockCard(med)
                }
            }
        }
    }
}

/**
 * Card showing medication in low stock.
 */
@Composable
private fun LowStockCard(med: MedicationLookup) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(med.medication_name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("⚠️  Only ${med.quantity_available} units available", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
