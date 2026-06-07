package com.egesa.clinic.shared.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
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
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.data.NoopDocumentCaptureGateway
import com.egesa.clinic.shared.ui.components.Avatar
import com.egesa.clinic.shared.ui.components.RoleBadge
import com.egesa.clinic.shared.ui.components.SyncStatusIndicator
import com.egesa.clinic.shared.ui.mobile.CompactMobileShell
import com.egesa.clinic.shared.ui.mobile.MobileGlobalSearchBox
import com.egesa.clinic.shared.ui.mobile.areaIcon
import com.egesa.clinic.shared.ui.mobile.displayName
import com.egesa.clinic.shared.ui.navigation.ClinicNavItem
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.navigation.navItemsFor
import com.egesa.clinic.shared.ui.navigation.roleCanAccessArea
import com.egesa.clinic.shared.ui.navigation.safeDefaultLandingArea
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
fun ResponsiveShell(
    session: SessionState,
    localRepository: LocalRepository,
    documentCaptureGateway: DocumentCaptureGateway = NoopDocumentCaptureGateway,
    onLogout: () -> Unit
) {
    val shouldShowSidebar = shouldShowSidebar()
    val shouldUseCompact = shouldUseCompactUI()
    val navItems = remember(session.role) { navItemsFor(session.role) }
    val landingArea = remember(session.role) { safeDefaultLandingArea(session.role) }

    var activeArea by remember(session.role) { mutableStateOf(landingArea) }
    var sidebarOpen by remember { mutableStateOf(true) }

    LaunchedEffect(session.role, navItems) {
        if (navItems.isEmpty()) return@LaunchedEffect
        if (!roleCanAccessArea(session.role, activeArea)) {
            activeArea = landingArea
        }
    }

    when {
        shouldUseCompact -> CompactMobileShell(
            session = session,
            localRepository = localRepository,
            documentCaptureGateway = documentCaptureGateway,
            navItems = navItems,
            activeArea = activeArea,
            onAreaSelected = { activeArea = it },
            onLogout = onLogout,
        )
        shouldShowSidebar -> AdaptiveDesktopShell(
            session = session,
            localRepository = localRepository,
            documentCaptureGateway = documentCaptureGateway,
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
            documentCaptureGateway = documentCaptureGateway,
            navItems = navItems,
            activeArea = activeArea,
            onAreaSelected = { activeArea = it },
            onLogout = onLogout,
        )
    }
}

@Composable
private fun AdaptiveDesktopShell(
    session: SessionState,
    localRepository: LocalRepository,
    documentCaptureGateway: DocumentCaptureGateway,
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

        Column(Modifier.weight(1f).fillMaxHeight()) {
            DesktopTopBar(activeArea = activeArea, session = session)
            HorizontalDivider(color = Slate200, thickness = 1.dp)
            Box(
                Modifier.weight(1f).fillMaxWidth().background(Slate50),
                contentAlignment = Alignment.TopStart,
            ) {
                AreaScreen(
                    area = activeArea,
                    session = session,
                    localRepository = localRepository,
                    documentCaptureGateway = documentCaptureGateway
                )
            }
        }
    }
}

@Composable
private fun DesktopTopBar(activeArea: WorkflowArea, session: SessionState) {
    var search by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().height(64.dp).background(White).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(areaIcon(activeArea), contentDescription = null, tint = Indigo700, modifier = Modifier.size(20.dp))
            Text("HIMS", style = MaterialTheme.typography.bodySmall, color = Slate400)
            Text(
                activeArea.displayName(),
                style = MaterialTheme.typography.labelLarge,
                color = Slate700,
            )
        }

        MobileGlobalSearchBox(
            value = search,
            onValueChange = { search = it },
            session = session,
            modifier = Modifier.widthIn(min = 280.dp, max = 420.dp),
            placeholder = "Search patient, queue, invoice",
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Egesa Clinic / ${session.shiftLabel}", style = MaterialTheme.typography.bodySmall, color = Slate500)
            SyncStatusIndicator(session.syncStatus, showLabel = false)
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Slate500, modifier = Modifier.size(20.dp))
            RoleBadge(session.role.name)
            Avatar(initials = session.initials, size = 32, bg = Indigo700)
        }
    }
}

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
                Icon(
                    imageVector = if (expanded) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                    contentDescription = if (expanded) "Collapse sidebar" else "Expand sidebar",
                    tint = Navy200,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        HorizontalDivider(color = SidebarBorder, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items.groupBy { it.group }.forEach { (group, groupedItems) ->
                item {
                    if (expanded) {
                        Text(
                            group.uppercase(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Navy200,
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                groupedItems.forEach { navItem ->
                    item {
                        SidebarItem(
                            item = navItem,
                            selected = activeArea == navItem.area,
                            expanded = expanded,
                            onClick = { onSelect(navItem.area) },
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = SidebarBorder, thickness = 1.dp)

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
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out", tint = Navy200, modifier = Modifier.size(16.dp))
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
                Icon(
                    areaIcon(item.area),
                    contentDescription = item.label,
                    tint = if (selected) Teal500 else Navy200,
                    modifier = Modifier.size(16.dp),
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
