package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.data.MohHtsTally
import com.egesa.clinic.shared.ServiceIndicatorSummary
import com.egesa.clinic.shared.ui.theme.*

@Composable
fun MohReportScreen(localRepository: LocalRepository) {
    var tallies by remember { mutableStateOf<List<MohHtsTally>>(emptyList()) }
    var indicators by remember { mutableStateOf<List<ServiceIndicatorSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Load tallies for the current month
        // In a real app, add date pickers
        tallies = localRepository.getHtsReportSummary("2024-01-01", "2024-12-31")
        indicators = localRepository.getServiceIndicatorSummary("2024-01-01", "2030-12-31")
        isLoading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("MOH Reporting Workbench", style = MaterialTheme.typography.headlineSmall, color = Navy800)
        Text(
            "Service events saved during care are rolled up into monthly reporting indicators.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            ServiceIndicatorTable(indicators)
            Spacer(Modifier.height(16.dp))
            Text("MOH 362: HTS summary", style = MaterialTheme.typography.titleMedium, color = Slate700)
            Spacer(Modifier.height(8.dp))
            HtsSummaryTable(tallies)
        }
    }
}

@Composable
fun ServiceIndicatorTable(indicators: List<ServiceIndicatorSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Monthly service indicator rollup", fontWeight = FontWeight.Bold, color = Slate700)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.background(Slate100).padding(8.dp)) {
                Text("Program", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Indicator", Modifier.weight(1.4f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Records", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Qty", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            if (indicators.isEmpty()) {
                Text(
                    "No service events captured yet. Save an OPD, ANC, HTS, NCD, CCC, maternity, or pharmacy encounter first.",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            } else {
                indicators.forEach { row ->
                    Row(Modifier.border(0.5.dp, Slate200).padding(8.dp)) {
                        Text(row.program, Modifier.weight(1f), fontSize = 12.sp)
                        Text(row.indicatorCategory, Modifier.weight(1.4f), fontSize = 12.sp)
                        Text(row.count.toString(), Modifier.weight(0.7f), fontSize = 12.sp)
                        Text(row.quantity.toString(), Modifier.weight(0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HtsSummaryTable(tallies: List<MohHtsTally>) {
    val ageGroups = listOf("0-1", "1-9", "10-14", "15-19", "20-24", "25+")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(Modifier.background(Slate100).padding(8.dp)) {
                Text("Age Group", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Male (+)", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Female (+)", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Total", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            ageGroups.forEach { group ->
                val malePos = tallies.find { it.ageGroup == group && it.gender.code == "male" && it.result.code == "positive" }?.count ?: 0
                val femalePos = tallies.find { it.ageGroup == group && it.gender.code == "female" && it.result.code == "positive" }?.count ?: 0
                
                Row(Modifier.border(0.5.dp, Slate200).padding(8.dp)) {
                    Text(group, Modifier.weight(1f), fontSize = 12.sp)
                    Text(malePos.toString(), Modifier.weight(1f), fontSize = 12.sp)
                    Text(femalePos.toString(), Modifier.weight(1f), fontSize = 12.sp)
                    Text((malePos + femalePos).toString(), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
