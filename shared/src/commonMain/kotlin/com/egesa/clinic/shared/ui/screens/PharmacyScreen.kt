package com.egesa.clinic.shared.ui.screens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.domain.Prescription
import com.egesa.clinic.shared.ui.navigation.SessionState
// Pharmacy management screen with prescription dispensing and printing
@Composable
fun PharmacyScreen(localRepository: LocalRepository, session: SessionState) {
    var prescriptions by remember { mutableStateOf<List<Prescription>>(emptyList()) }
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var activeTab by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        patients = localRepository.getAllPatients()
    }
    val tabs = listOf("Pending Dispensal", "Dispensed", "External Purchase")
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Pharmacy Management", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        TabRow(activeTab, modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = { Text(title) }
                )
            }
        }
        Text("Module with prescription dispensing and printing for external pharmacies", 
            style = MaterialTheme.typography.bodySmall)
    }
}
