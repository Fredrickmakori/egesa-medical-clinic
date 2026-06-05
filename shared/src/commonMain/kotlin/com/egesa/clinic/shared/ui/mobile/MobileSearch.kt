package com.egesa.clinic.shared.ui.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.WorkflowArea
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.navigation.roleCanAccessArea
import com.egesa.clinic.shared.ui.theme.Amber700
import com.egesa.clinic.shared.ui.theme.Indigo700
import com.egesa.clinic.shared.ui.theme.Rose700
import com.egesa.clinic.shared.ui.theme.Sky700
import com.egesa.clinic.shared.ui.theme.Slate100
import com.egesa.clinic.shared.ui.theme.Slate200
import com.egesa.clinic.shared.ui.theme.Slate500
import com.egesa.clinic.shared.ui.theme.Slate900
import com.egesa.clinic.shared.ui.theme.StatusWarning
import com.egesa.clinic.shared.ui.theme.White

internal data class SearchResult(
    val title: String,
    val detail: String,
    val type: String,
    val icon: ImageVector,
    val color: Color,
)

@Composable
internal fun MobileGlobalSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    session: SessionState,
    modifier: Modifier = Modifier,
    placeholder: String,
) {
    val results = remember(value, session.role) { globalSearchResults(value, session.role) }
    Box(modifier.zIndex(10f)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            singleLine = true,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = MaterialTheme.shapes.medium,
        )
        if (results.isNotEmpty()) {
            MobileSearchResults(
                results = results,
                modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
            )
        }
    }
}

@Composable
private fun MobileSearchResults(results: List<SearchResult>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(6.dp),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            results.forEachIndexed { index, result ->
                Row(
                    Modifier.fillMaxWidth().clickable { }.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(result.icon, contentDescription = null, tint = result.color, modifier = Modifier.size(18.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(result.title, style = MaterialTheme.typography.titleSmall, color = Slate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(result.detail, style = MaterialTheme.typography.bodySmall, color = Slate500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(result.type, style = MaterialTheme.typography.labelSmall, color = result.color)
                }
                if (index < results.lastIndex) HorizontalDivider(color = Slate100)
            }
        }
    }
}

private fun globalSearchResults(query: String, role: UserRole): List<SearchResult> {
    if (query.isBlank()) return emptyList()
    val pool = buildList {
        add(SearchResult("Achieng Mary", "PT-1042 - waiting in reception queue", "Patient", Icons.Filled.People, Indigo700))
        if (roleCanAccessArea(role, WorkflowArea.LAB_IMAGING)) {
            add(SearchResult("Otieno Brian", "PT-1178 - lab result pending review", "Patient", Icons.Filled.Science, Rose700))
        }
        if (roleCanAccessArea(role, WorkflowArea.BILLING)) {
            add(SearchResult("INV-1008", "M-Pesa STK sent - KES 2,500", "Invoice", Icons.Filled.Payments, Amber700))
        }
        if (roleCanAccessArea(role, WorkflowArea.APPOINTMENTS)) {
            add(SearchResult("Room 2 schedule", "Clinical officer follow-up appointments", "Calendar", Icons.Filled.CalendarMonth, Sky700))
        }
        if (roleCanAccessArea(role, WorkflowArea.INVENTORY)) {
            add(SearchResult("Amoxicillin 500mg", "Low stock - reorder required", "Stock", Icons.Filled.Inventory2, StatusWarning))
        }
    }
    if (pool.isEmpty()) return emptyList()
    return pool.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.detail.contains(query, ignoreCase = true) ||
            it.type.contains(query, ignoreCase = true)
    }.ifEmpty { pool.take(3) }.take(4)
}
