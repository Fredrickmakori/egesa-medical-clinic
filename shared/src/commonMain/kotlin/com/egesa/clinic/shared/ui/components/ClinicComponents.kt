package com.egesa.clinic.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.ui.theme.*

// ── Status / Acuity badge ──────────────────────────────────────────────────────

enum class AcuityLevel(val label: String, val fg: Color, val bg: Color) {
    CRITICAL("Critical", StatusCritical, Color(0xFFFEE2E2)),
    HIGH    ("High",     StatusWarning,  Color(0xFFFEF9C3)),
    MODERATE("Moderate", StatusInfo,     Color(0xFFDBEAFE)),
    LOW     ("Low",      StatusStable,   Color(0xFFD1FAE5)),
    STABLE  ("Stable",   StatusStable,   Color(0xFFD1FAE5)),
}

fun acuityLevel(raw: String): AcuityLevel = when (raw.lowercase()) {
    "critical" -> AcuityLevel.CRITICAL
    "high"     -> AcuityLevel.HIGH
    "low"      -> AcuityLevel.LOW
    "stable"   -> AcuityLevel.STABLE
    else       -> AcuityLevel.MODERATE
}

@Composable
fun StatusBadge(level: AcuityLevel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(level.bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(level.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = level.fg)
    }
}

@Composable
fun TextBadge(text: String, fg: Color, bg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

// ── Role badge ─────────────────────────────────────────────────────────────────

@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (role.uppercase()) {
        "ADMIN"        -> Navy100 to Navy800
        "DOCTOR"       -> Color(0xFFEDE9FE) to Color(0xFF5B21B6)
        "NURSE"        -> Teal100 to Teal700
        "RECEPTIONIST" -> Color(0xFFFEF9C3) to Color(0xFF92400E)
        else           -> Slate100 to Slate500
    }
    TextBadge(role, fg, bg, modifier)
}

// ── Metric card ────────────────────────────────────────────────────────────────

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    trend: String? = null,
    trendUp: Boolean? = null,
    accentColor: Color = Navy800,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier  = modifier,
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = White),
        border    = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Slate500)
                if (trend != null && trendUp != null) {
                    TextBadge(
                        text = trend,
                        fg   = if (trendUp) StatusStable else StatusCritical,
                        bg   = if (trendUp) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                    )
                }
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Slate900)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate400)
            }
            // Thin accent line at bottom
            Spacer(Modifier.height(2.dp))
            Box(Modifier.width(32.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
        }
    }
}

// ── Section header ─────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Slate900)
        action?.invoke()
    }
}

// ── Patient card ───────────────────────────────────────────────────────────────

@Composable
fun PatientCard(
    patient: Patient,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) Navy800 else Slate200
    val borderWidth = if (selected) 1.5.dp else 1.dp
    val bgColor     = if (selected) Navy50 else White

    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        border    = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top,
        ) {
            Column(
                Modifier.weight(1f).padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    patient.fullName,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${patient.id}  •  ${patient.age} yrs, ${patient.sex.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
                patient.assignedWard?.let {
                    Text(
                        "$it  ${patient.roomBed ?: ""}".trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatusBadge(acuityLevel(patient.acuity))
                TextBadge(patient.status, Slate600, Slate100)
            }
        }
    }
}

// ── Avatar ─────────────────────────────────────────────────────────────────────

@Composable
fun Avatar(
    initials: String,
    size: Int = 36,
    bg: Color = Navy700,
    fg: Color = White,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier          = modifier.size(size.dp).clip(CircleShape).background(bg),
        contentAlignment  = Alignment.Center,
    ) {
        Text(initials.take(2).uppercase(), color = fg, fontSize = (size / 2.8).sp, fontWeight = FontWeight.Bold)
    }
}

// ── Generic clinic card ────────────────────────────────────────────────────────

@Composable
fun ClinicCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier  = modifier,
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = White),
        border    = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

// ── Dot indicator ─────────────────────────────────────────────────────────────

@Composable
fun StatusDot(color: Color, size: Int = 8, modifier: Modifier = Modifier) {
    Box(modifier.size(size.dp).clip(CircleShape).background(color))
}

// ── Labeled divider ────────────────────────────────────────────────────────────

@Composable
fun LabeledDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier            = modifier.fillMaxWidth(),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f), color = Slate200)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Slate400)
        HorizontalDivider(Modifier.weight(1f), color = Slate200)
    }
}

// ── Sync status indicator ───────────────────────────────────────────────────

@Composable
fun SyncStatusIndicator(
    syncStatus: com.egesa.clinic.shared.sync.SyncStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (color, label) = when (syncStatus) {
            com.egesa.clinic.shared.sync.SyncStatus.SYNCING ->
                Pair(Color(0xFFFCD34D), "Syncing…")
            com.egesa.clinic.shared.sync.SyncStatus.SUCCESS ->
                Pair(StatusStable, "Synced")
            com.egesa.clinic.shared.sync.SyncStatus.ERROR ->
                Pair(StatusCritical, "Sync Error")
            com.egesa.clinic.shared.sync.SyncStatus.OFFLINE ->
                Pair(Slate500, "Offline")
            com.egesa.clinic.shared.sync.SyncStatus.IDLE ->
                Pair(Navy200, "Ready")
        }
        StatusDot(color, size = 8)
        if (showLabel) {
            Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}
