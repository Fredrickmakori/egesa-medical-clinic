package com.egesa.clinic.shared.ui.screens.lab

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.data.CompleteLaboratoryDashboard
import com.egesa.clinic.shared.data.IntegratedLabRepository
import com.egesa.clinic.shared.data.TATComplianceMetrics
import com.egesa.clinic.shared.domain.*
import com.egesa.clinic.shared.ui.components.*
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Comprehensive Lab Dashboard Screen
 * Shows all critical metrics, alerts, and status indicators
 * Uses the clinic design system for consistent visual polish.
 */
@Composable
fun LabDashboardScreen(
    repository: IntegratedLabRepository,
    session: SessionState,
) {
    var dashboard by remember { mutableStateOf<CompleteLaboratoryDashboard?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            dashboard = repository.getCompleteDashboard()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(color = Navy800, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                Text("Loading dashboard…", style = MaterialTheme.typography.bodySmall, color = Slate400)
            }
        }
        return
    }

    dashboard?.let { dash ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                ModuleHeader(
                    title = "Lab Dashboard",
                    subtitle = "Equipment, QC, TAT compliance, and alert monitoring",
                )
            }

            // ── KPI Summary Strip ─────────────────────────────────────────────
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricCard(
                        title = "Total Orders",
                        value = dash.summary?.totalOrders?.toString() ?: "–",
                        subtitle = "All statuses",
                        accentColor = Indigo700,
                        modifier = Modifier.width(170.dp),
                    )
                    MetricCard(
                        title = "Pending Results",
                        value = dash.summary?.pendingResults?.toString() ?: "–",
                        subtitle = "Awaiting entry",
                        accentColor = Amber700,
                        modifier = Modifier.width(170.dp),
                    )
                    MetricCard(
                        title = "Critical Alerts",
                        value = dash.criticalAlerts.size.toString(),
                        subtitle = if (dash.criticalAlerts.isEmpty()) "None active" else "Needs attention",
                        accentColor = Rose700,
                        modifier = Modifier.width(170.dp),
                    )
                    MetricCard(
                        title = "TAT Compliance",
                        value = "${formatPercent(dash.tatMetrics?.compliancePercent ?: 0.0)}%",
                        subtitle = "Last 7 days",
                        accentColor = Teal700,
                        modifier = Modifier.width(170.dp),
                    )
                }
            }

            // ── Critical Alerts Section ───────────────────────────────────────
            if (dash.criticalAlerts.isNotEmpty()) {
                item {
                    SectionHeader("Critical Alerts (${dash.criticalAlerts.size})")
                }
                items(dash.criticalAlerts.take(5)) { alert ->
                    CriticalAlertCard(alert, repository, session)
                }
            }

            // ── Equipment Status Section ──────────────────────────────────────
            if (dash.equipmentStatus.isNotEmpty()) {
                item {
                    SectionHeader("Equipment Status")
                }
                items(dash.equipmentStatus) { status ->
                    EquipmentStatusCard(status)
                }
            } else {
                item {
                    SectionHeader("Equipment Status")
                }
                item {
                    EmptyStateCard(
                        title = "No equipment registered",
                        detail = "Equipment status will appear here once instruments are configured.",
                    )
                }
            }

            // ── Reagent Alerts Section ────────────────────────────────────────
            if (dash.reagentAlerts.isNotEmpty()) {
                item {
                    SectionHeader("Reagent Alerts (${dash.reagentAlerts.size})")
                }
                items(dash.reagentAlerts.take(5)) { reagent ->
                    ReagentAlertCard(reagent)
                }
            }

            // ── TAT Performance Section ───────────────────────────────────────
            dash.tatMetrics?.let { metrics ->
                item {
                    SectionHeader("TAT Performance (Last 7 Days)")
                }
                item {
                    TATMetricsCard(metrics)
                }
            }

            // Bottom spacer
            item { Spacer(Modifier.height(16.dp)) }
        }
    } ?: run {
        // Fallback when dashboard is null after loading
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                title = "Dashboard unavailable",
                detail = "Could not load lab dashboard data. Try again later.",
            )
        }
    }
}

// ── Critical Alert Card ───────────────────────────────────────────────────────

@Composable
private fun CriticalAlertCard(
    alert: LabCriticalAlert,
    repository: IntegratedLabRepository,
    session: SessionState,
) {
    var isAcknowledging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ClinicCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(StatusCritical, 10)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            alert.testName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                        )
                        Text(
                            "Value: ${alert.criticalValue} · ${alert.thresholdType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                        )
                    }
                }
                TextBadge(
                    text = alert.thresholdType,
                    fg = Color.White,
                    bg = StatusCritical,
                )
            }

            // Acknowledge button or status
            if (alert.acknowledgedAt == null) {
                Button(
                    onClick = {
                        isAcknowledging = true
                        scope.launch {
                            repository.acknowledgeCriticalAlert(alert.id, session.staffId, "Acknowledged")
                            isAcknowledging = false
                        }
                    },
                    enabled = !isAcknowledging,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                ) {
                    if (isAcknowledging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (isAcknowledging) "Acknowledging…" else "Acknowledge Alert",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = StatusStable,
                    )
                    Text(
                        "Acknowledged by ${alert.acknowledgedBy ?: "–"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusStable,
                    )
                }
            }
        }
    }
}

// ── Equipment Status Card ─────────────────────────────────────────────────────

@Composable
private fun EquipmentStatusCard(status: LabEquipmentStatusSummary) {
    ClinicCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(status.name, style = MaterialTheme.typography.titleSmall, color = Slate900)
                    Text(status.code, style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
                EquipmentStatusBadge(status.status.name)
            }

            HorizontalDivider(color = Slate100)

            // Calibration & Maintenance
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Calibration
                Column(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Slate50)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Calibration", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Slate600)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusDot(
                            color = statusColor(status.calibrationStatus),
                            size = 8,
                        )
                        Text(
                            formatStatusLabel(status.calibrationStatus),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor(status.calibrationStatus),
                        )
                    }
                }

                // Maintenance
                Column(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Slate50)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Maintenance", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Slate600)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusDot(
                            color = statusColor(status.maintenanceStatus),
                            size = 8,
                        )
                        Text(
                            formatStatusLabel(status.maintenanceStatus),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor(status.maintenanceStatus),
                        )
                    }
                }
            }
        }
    }
}

// ── Reagent Alert Card ────────────────────────────────────────────────────────

@Composable
private fun ReagentAlertCard(reagent: LabReagentExpirySummary) {
    ClinicCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(reagent.name, style = MaterialTheme.typography.titleSmall, color = Slate900)
                    Text("Code: ${reagent.code}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
                TextBadge(
                    text = formatStatusLabel(reagent.expiryStatus),
                    fg = Color.White,
                    bg = when (reagent.expiryStatus) {
                        "EXPIRED" -> StatusCritical
                        "EXPIRING_SOON" -> StatusWarning
                        else -> StatusStable
                    },
                )
            }

            HorizontalDivider(color = Slate100)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Quantity", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text(
                        "${reagent.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Stock Status", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    TextBadge(
                        text = formatStatusLabel(reagent.stockStatus),
                        fg = if (reagent.stockStatus == "REORDER_NEEDED") StatusWarning else StatusStable,
                        bg = if (reagent.stockStatus == "REORDER_NEEDED") Color(0xFFFEF9C3) else Color(0xFFD1FAE5),
                    )
                }
            }
        }
    }
}

// ── TAT Metrics Card ──────────────────────────────────────────────────────────

@Composable
private fun TATMetricsCard(metrics: TATComplianceMetrics) {
    ClinicCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Metric tiles row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TATStatTile(
                    label = "On-Time",
                    value = "${metrics.onTimeOrders}/${metrics.totalOrders}",
                    color = StatusStable,
                    modifier = Modifier.weight(1f),
                )
                TATStatTile(
                    label = "Late",
                    value = "${metrics.lateOrders}",
                    color = StatusCritical,
                    modifier = Modifier.weight(1f),
                )
                TATStatTile(
                    label = "Avg TAT",
                    value = "${formatDecimal(metrics.averageTATHours)}h",
                    color = StatusInfo,
                    modifier = Modifier.weight(1f),
                )
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Compliance", style = MaterialTheme.typography.labelMedium, color = Slate600)
                    Text(
                        "${formatDecimal(metrics.compliancePercent)}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (metrics.compliancePercent >= 90) StatusStable else StatusWarning,
                    )
                }
                LinearProgressIndicator(
                    progress = { (metrics.compliancePercent / 100f).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (metrics.compliancePercent >= 90) StatusStable else StatusWarning,
                    trackColor = Slate100,
                )
            }
        }
    }
}

@Composable
private fun TATStatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Slate50)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Slate500)
    }
}

// ── Equipment Status Badge ────────────────────────────────────────────────────

@Composable
private fun EquipmentStatusBadge(status: String) {
    val (fg, bg) = when (status) {
        "ACTIVE" -> Color.White to StatusStable
        "INACTIVE" -> Slate600 to Slate200
        "MAINTENANCE" -> Color.White to StatusWarning
        else -> Color.White to StatusCritical
    }
    TextBadge(text = formatStatusLabel(status), fg = fg, bg = bg)
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun statusColor(status: String): Color = when (status) {
    "OVERDUE" -> StatusCritical
    "DUE_SOON" -> StatusWarning
    "OK" -> StatusStable
    else -> Slate400
}

private fun formatStatusLabel(raw: String): String =
    raw.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

private fun formatPercent(value: Double): String =
    value.toInt().toString()

private fun formatDecimal(value: Double): String {
    val intPart = value.toInt()
    val decPart = ((value - intPart) * 10).toInt()
    return "$intPart.$decPart"
}
