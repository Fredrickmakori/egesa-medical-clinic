package com.egesa.clinic.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.WorkflowArea

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = HospitalState()
        setContent {
            MaterialTheme {
                MobileOverview(state)
            }
        }
    }
}

@Composable
private fun MobileOverview(state: HospitalState) {
    var activeArea by remember { mutableStateOf(WorkflowArea.RECEPTION) }
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Egesa Hospital Operations", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search patient") },
            modifier = Modifier.fillMaxWidth()
        )

        TabRow(selectedTabIndex = WorkflowArea.entries.indexOf(activeArea)) {
            WorkflowArea.entries.forEach { area ->
                Tab(
                    selected = area == activeArea,
                    onClick = { activeArea = area },
                    text = { Text(area.name.take(3)) }
                )
            }
        }

        when (activeArea) {
            WorkflowArea.RECEPTION -> Text("Queue: ${state.receptionQueue().size} waiting")
            WorkflowArea.WARD -> Text("Beds: ${state.wardBeds().count { it.occupiedBy == null }} available")
            WorkflowArea.ADMIN -> state.metrics().forEach { Text("${it.title}: ${it.value}") }
            else -> Text("${activeArea.name.lowercase().replaceFirstChar { it.uppercase() }} list")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.allPatients(searchText)) { patient ->
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text(patient.fullName)
                        Text("${patient.id} • ${patient.status}")
                    }
                }
            }
        }
    }
}
