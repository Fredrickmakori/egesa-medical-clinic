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
import com.egesa.clinic.shared.ui.theme.*

@Composable
fun MohReportScreen(localRepository: LocalRepository) {
    var tallies by remember { mutableStateOf<List<MohHtsTally>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Load tallies for the current month
        // In a real app, add date pickers
        tallies = localRepository.getHtsReportSummary("2024-01-01", "2024-12-31")
        isLoading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("MOH 362: HTS LAB REGISTER SUMMARY", style = MaterialTheme.typography.headlineSmall, color = Navy800)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            HtsSummaryTable(tallies)
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
