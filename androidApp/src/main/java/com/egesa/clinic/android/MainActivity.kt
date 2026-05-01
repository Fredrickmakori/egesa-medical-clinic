package com.egesa.clinic.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.HospitalState

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
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Egesa Hospital Operations", style = MaterialTheme.typography.headlineSmall)
        state.metrics().forEach { metric ->
            Text("${metric.title}: ${metric.value}")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.allPatients()) { patient ->
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
