package com.egesa.clinic.shared.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.WorkflowArea
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.ui.components.Avatar
import com.egesa.clinic.shared.ui.components.RoleBadge
import com.egesa.clinic.shared.ui.navigation.ClinicNavItem
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.navigation.initials
import com.egesa.clinic.shared.ui.navigation.navItemsFor
import com.egesa.clinic.shared.ui.screens.AreaScreen
import com.egesa.clinic.shared.ui.theme.*

@Composable
fun DesktopShell(session: SessionState, localRepository: LocalRepository, onLogout: () -> Unit) {
    val navItems = remember(session.role) { navItemsFor(session.role) }
    var activeArea    by remember { mutableStateOf(navItems.first().area) }
    var sidebarOpen   by remember { mutableStateOf(true) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarOpen) 240.dp else 64.dp,
        animationSpec = tween(200),
        label = "sidebarWidth",
    )

    Row(Modifier.fillMaxSize()) {
        // ── Sidebar ────────────────────────────────────────────────────────────
        Sidebar(
            items       = navItems,
            activeArea  = activeArea,
            session     = session,
            expanded    = sidebarOpen,
            width       = sidebarWidth.value,
            onToggle    = { sidebarOpen = !sidebarOpen },
            onSelect    = { activeArea = it },
            onLogout    = onLogout,
            modifier    = Modifier.width(sidebarWidth).fillMaxHeight(),
        )

        // ── Main content ───────────────────────────────────────────────────────
        Column(Modifier.weight(1f).fillMaxHeight()) {
            TopBar(activeArea = activeArea, session = session)
            HorizontalDivider(color = Slate200, thickness = 1.dp)
            Box(
                Modifier.weight(1f).fillMaxWidth().background(Slate50),
                contentAlignment = Alignment.TopStart,
            ) {
                AreaScreen(area = activeArea, session = session, localRepository = localRepository)
            }
        }
    }
}

// ── Sidebar ────────────────────────────────────────────────────────────────────

@Composable
private fun Sidebar(
    items: List<ClinicNavItem>,
    activeArea: WorkflowArea,
    session: SessionState,
    expanded: Boolean,
    width: Float,
    onToggle: () -> Unit,
    onSelect: (WorkflowArea) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(SidebarBg)) {
        // Logo row
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 14.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (expanded) {
                Column {
                    Text("EGESA", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = White, letterSpacing = 2.sp)
                    Text("CLINIC", fontSize = 9.sp, color = Navy200, letterSpacing = 2.sp)
                }
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                Text(if (expanded) "«" else "»", color = Navy200, fontSize = 14.sp)
            }
        }

        HorizontalDivider(color = SidebarBorder, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))

        // Nav items
        items.forEach { item ->
            SidebarItem(
                item       = item,
                selected   = activeArea == item.area,
                expanded   = expanded,
                onClick    = { onSelect(item.area) },
            )
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = SidebarBorder, thickness = 1.dp)

        // User row
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Avatar(initials = session.initials, size = 34, bg = Navy600)
            if (expanded) {
                Column(Modifier.weight(1f)) {
                    Text(
                        session.fullName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(session.shiftLabel, fontSize = 11.sp, color = Navy200)
                }
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                        .background(SidebarActive).clickable(onClick = onLogout),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⏻", color = Navy200, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(
    item: ClinicNavItem,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue   = if (selected) SidebarActive else Color.Transparent,
        animationSpec = tween(150),
        label         = "navBg",
    )
    val textColor = if (selected) White else Navy200

    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp).background(bg).clickable(onClick = onClick),
    ) {
        // Left accent bar
        Box(
            Modifier.width(3.dp).fillMaxHeight()
                .background(if (selected) Teal500 else Color.Transparent)
        )
        // Icon area (using first 2 chars as a label placeholder)
        Box(
            Modifier.size(42.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (selected) Teal600.copy(alpha = 0.25f) else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.shortLabel.take(2),
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (selected) Teal500 else Navy200,
                    letterSpacing = 0.5.sp,
                )
            }
        }
        if (expanded) {
            Text(
                item.label,
                modifier   = Modifier.align(Alignment.CenterVertically),
                fontSize   = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = textColor,
            )
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(activeArea: WorkflowArea, session: SessionState) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(White).padding(horizontal = 20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Breadcrumb
        Row(
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Home", style = MaterialTheme.typography.bodySmall, color = Slate400)
            Text("›", color = Slate300, fontSize = 12.sp)
            Text(
                activeArea.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge,
                color = Slate700,
            )
        }

        // Right actions
        Row(
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Search stub
            Box(
                Modifier.width(220.dp).height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Slate100),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("⌕", color = Slate400, fontSize = 14.sp)
                    Text("Search patients, staff…", style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
            }

            // Role badge
            RoleBadge(session.role.name)

            // Avatar
            Avatar(initials = session.initials, size = 32, bg = Navy700)
        }
    }
}
