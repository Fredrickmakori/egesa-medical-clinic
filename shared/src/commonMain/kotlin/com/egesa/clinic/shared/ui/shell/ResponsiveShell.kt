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
import com.egesa.clinic.shared.ui.responsive.*
import com.egesa.clinic.shared.ui.screens.AreaScreen
import com.egesa.clinic.shared.ui.theme.*

/**
 * Adaptive shell that responds to screen size changes
 * - COMPACT: Mobile view with bottom navigation
 * - MEDIUM: Tablet view with collapsible sidebar
 * - EXPANDED: Desktop view with full sidebar
 */
@Composable
fun ResponsiveShell(session: SessionState, localRepository: LocalRepository, onLogout: () -> Unit) {
    val windowSize = currentWindowSizeClass()
    val shouldShowSidebar = shouldShowSidebar()
    val shouldUseCompact = shouldUseCompactUI()
    val navItems = remember(session.role) { navItemsFor(session.role) }
    
    var activeArea by remember { mutableStateOf(navItems.firstOrNull()?.area ?: WorkflowArea.DASHBOARD) }
    var sidebarOpen by remember { mutableStateOf(true) }
    var bottomNavVisible by remember { mutableStateOf(true) }

    when {
        shouldUseCompact -> CompactMobileShell(
            session = session,
            localRepository = localRepository,
            navItems = navItems,
            activeArea = activeArea,
            onAreaSelected = { activeArea = it },
            bottomNavVisible = bottomNavVisible,
            onLogout = onLogout,
        )
        shouldShowSidebar -> AdaptiveDesktopShell(
            session = session,
            localRepository = localRepository,
            navItems = navItems,
            activeArea = activeArea,
            onAreaSelected = { activeArea = it },
            sidebarOpen = sidebarOpen,
            onSidebarToggle = { sidebarOpen = !sidebarOpen },
            onLogout = onLogout,
        )
        else -> CompactMobileShell(
            session = session,
            localRepository = localRepository,
            navItems = navItems,
            activeArea = activeArea,
            onAreaSelected = { activeArea = it },
            bottomNavVisible = bottomNavVisible,
            onLogout = onLogout,
        )
    }
}

/**
 * Compact mobile shell with bottom navigation
 * Used on COMPACT screen sizes (phones < 600 dp width)
 */
@Composable
private fun CompactMobileShell(
    session: SessionState,
    localRepository: LocalRepository,
    navItems: List<ClinicNavItem>,
    activeArea: WorkflowArea,
    onAreaSelected: (WorkflowArea) -> Unit,
    bottomNavVisible: Boolean,
    onLogout: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // ── Header ──────────────────────────────────────────────────────────────
        MobileTopBar(session = session, activeArea = activeArea, onLogout = onLogout)
        HorizontalDivider(color = Slate200, thickness = 1.dp)

        // ── Content ─────────────────────────────────────────────────────────────
        Box(
            Modifier.weight(1f).fillMaxWidth().background(Slate50),
            contentAlignment = Alignment.TopStart,
        ) {
            AreaScreen(area = activeArea, session = session, localRepository = localRepository)
        }

        // ── Bottom Navigation ───────────────────────────────────────────────────
        if (bottomNavVisible) {
            HorizontalDivider(color = Slate200, thickness = 1.dp)
            BottomNavigationBar(
                items = navItems,
                activeArea = activeArea,
                onAreaSelected = onAreaSelected,
            )
        }
    }
}

/**
 * Adaptive desktop shell with sidebar
 * Used on MEDIUM (600-839 dp) and EXPANDED (>= 840 dp) screen sizes
 */
@Composable
private fun AdaptiveDesktopShell(
    session: SessionState,
    localRepository: LocalRepository,
    navItems: List<ClinicNavItem>,
    activeArea: WorkflowArea,
    onAreaSelected: (WorkflowArea) -> Unit,
    sidebarOpen: Boolean,
    onSidebarToggle: () -> Unit,
    onLogout: () -> Unit,
) {
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarOpen) responsiveSidebarWidth().dp else 64.dp,
        animationSpec = tween(200),
        label = "sidebarWidth",
    )

    Row(Modifier.fillMaxSize()) {
        // ── Sidebar ────────────────────────────────────────────────────────────
        AdaptiveSidebar(
            items = navItems,
            activeArea = activeArea,
            session = session,
            expanded = sidebarOpen,
            width = sidebarWidth.value,
            onToggle = onSidebarToggle,
            onSelect = onAreaSelected,
            onLogout = onLogout,
            modifier = Modifier.width(sidebarWidth).fillMaxHeight(),
        )

        // ── Main content ───────────────────────────────────────────────────────
        Column(Modifier.weight(1f).fillMaxHeight()) {
            DesktopTopBar(activeArea = activeArea, session = session)
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

// ── Mobile Top Bar ──────────────────────────────────────────────────────────

@Composable
private fun MobileTopBar(
    session: SessionState,
    activeArea: WorkflowArea,
    onLogout: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(White).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("EGESA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)
            Text("CLINIC", fontSize = 8.sp, color = Navy400)
        }
        Text(
            activeArea.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color = Slate700,
        )
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                .background(Navy100).clickable(onClick = onLogout),
            contentAlignment = Alignment.Center,
        ) {
            Avatar(initials = session.initials, size = 28, bg = Navy600)
        }
    }
}

// ── Desktop Top Bar ─────────────────────────────────────────────────────────

@Composable
private fun DesktopTopBar(activeArea: WorkflowArea, session: SessionState) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(White).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Breadcrumb
        Row(
            verticalAlignment = Alignment.CenterVertically,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoleBadge(session.role.name)
            Avatar(initials = session.initials, size = 32, bg = Navy700)
        }
    }
}

// ── Adaptive Sidebar ────────────────────────────────────────────────────────

@Composable
private fun AdaptiveSidebar(
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
            verticalAlignment = Alignment.CenterVertically,
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
                item = item,
                selected = activeArea == item.area,
                expanded = expanded,
                onClick = { onSelect(item.area) },
            )
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = SidebarBorder, thickness = 1.dp)

        // User row
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
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
        targetValue = if (selected) SidebarActive else Color.Transparent,
        animationSpec = tween(150),
        label = "navBg",
    )
    val textColor = if (selected) White else Navy200

    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp).background(bg).clickable(onClick = onClick),
    ) {
        Box(
            Modifier.width(3.dp).fillMaxHeight()
                .background(if (selected) Teal500 else Color.Transparent)
        )
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
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Teal500 else Navy200,
                    letterSpacing = 0.5.sp,
                )
            }
        }
        if (expanded) {
            Text(
                item.label,
                modifier = Modifier.align(Alignment.CenterVertically),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
            )
        }
    }
}

// ── Bottom Navigation Bar ───────────────────────────────────────────────────

@Composable
private fun BottomNavigationBar(
    items: List<ClinicNavItem>,
    activeArea: WorkflowArea,
    onAreaSelected: (WorkflowArea) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        containerColor = White,
        tonalElevation = 1.dp,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Box(
                        Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                            .background(
                                if (activeArea == item.area) Teal200 else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.shortLabel.take(1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeArea == item.area) Teal700 else Navy400,
                        )
                    }
                },
                label = {
                    Text(
                        item.shortLabel,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selected = activeArea == item.area,
                onClick = { onAreaSelected(item.area) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Teal700,
                    selectedTextColor = Teal700,
                    unselectedIconColor = Navy400,
                    unselectedTextColor = Navy400,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

