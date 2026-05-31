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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.egesa.clinic.shared.WorkflowArea
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.data.NoopDocumentCaptureGateway
import com.egesa.clinic.shared.ui.components.Avatar
import com.egesa.clinic.shared.ui.components.RoleBadge
import com.egesa.clinic.shared.ui.components.SyncStatusIndicator
import com.egesa.clinic.shared.ui.navigation.ClinicNavItem
import com.egesa.clinic.shared.ui.navigation.SessionState
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
fun ResponsiveShell(
    session: SessionState,
    localRepository: LocalRepository,
    documentCaptureGateway: DocumentCaptureGateway = NoopDocumentCaptureGateway,
    onLogout: () -> Unit
) {
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
            documentCaptureGateway = documentCaptureGateway,
            navItems = navItems,
            activeArea = activeArea,
            onAreaSelected = { activeArea = it },
            bottomNavVisible = bottomNavVisible,
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
    documentCaptureGateway: DocumentCaptureGateway,
    navItems: List<ClinicNavItem>,
    activeArea: WorkflowArea,
    onAreaSelected: (WorkflowArea) -> Unit,
    bottomNavVisible: Boolean,
    onLogout: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        MobileTopBar(session = session, activeArea = activeArea, onLogout = onLogout)
        MobileSearchBar()
        HorizontalDivider(color = Slate200, thickness = 1.dp)

        // â”€â”€ Content â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Bottom Navigation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
        // â”€â”€ Sidebar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Main content â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

// â”€â”€ Mobile Top Bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(areaIcon(activeArea), contentDescription = null, tint = Indigo700, modifier = Modifier.size(22.dp))
            Column {
                Text(activeArea.displayName(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                Text("Egesa Clinic", fontSize = 10.sp, color = Slate500)
            }
        }
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                .background(Indigo100).clickable(onClick = onLogout),
            contentAlignment = Alignment.Center,
        ) {
            Avatar(initials = session.initials, size = 28, bg = Indigo700)
        }
    }
}

@Composable
private fun MobileSearchBar() {
    var search by remember { mutableStateOf("") }
    GlobalSearchBox(
        value = search,
        onValueChange = { search = it },
        modifier = Modifier.fillMaxWidth().background(White).padding(horizontal = 14.dp, vertical = 8.dp),
        placeholder = "Search patient, visit, invoice",
    )
}

// â”€â”€ Desktop Top Bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun DesktopTopBar(activeArea: WorkflowArea, session: SessionState) {
    var search by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().height(64.dp).background(White).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Breadcrumb
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

        GlobalSearchBox(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.widthIn(min = 280.dp, max = 420.dp),
            placeholder = "Search patient, queue, invoice",
        )

        // Right actions
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
private fun GlobalSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
) {
    val results = remember(value) { globalSearchResults(value) }
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
            ClinicSearchResults(
                results = results,
                modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
            )
        }
    }
}

@Composable
private fun ClinicSearchResults(results: List<SearchResult>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
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

private data class SearchResult(
    val title: String,
    val detail: String,
    val type: String,
    val icon: ImageVector,
    val color: Color,
)

private fun globalSearchResults(query: String): List<SearchResult> {
    if (query.isBlank()) return emptyList()
    val pool = listOf(
        SearchResult("Achieng Mary", "PT-1042 - waiting in reception queue", "Patient", Icons.Filled.People, Indigo700),
        SearchResult("Otieno Brian", "PT-1178 - lab result pending review", "Patient", Icons.Filled.Science, Rose700),
        SearchResult("INV-1008", "M-Pesa STK sent - KES 2,500", "Invoice", Icons.Filled.Payments, Amber700),
        SearchResult("Room 2 schedule", "Clinical officer follow-up appointments", "Calendar", Icons.Filled.CalendarMonth, Sky700),
        SearchResult("Amoxicillin 500mg", "Low stock - reorder required", "Stock", Icons.Filled.Inventory2, StatusWarning),
    )
    return pool.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.detail.contains(query, ignoreCase = true) ||
            it.type.contains(query, ignoreCase = true)
    }.ifEmpty { pool.take(3) }.take(4)
}

// â”€â”€ Adaptive Sidebar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

        // Nav items
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

// â”€â”€ Bottom Navigation Bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                        Icon(
                            areaIcon(item.area),
                            contentDescription = item.label,
                            tint = if (activeArea == item.area) Teal700 else Navy400,
                            modifier = Modifier.size(16.dp),
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

private fun WorkflowArea.displayName(): String = when (this) {
    WorkflowArea.DASHBOARD -> "Command Center"
    WorkflowArea.RECEPTION -> "Reception"
    WorkflowArea.APPOINTMENTS -> "Appointments"
    WorkflowArea.CONSULTATION -> "Consultation"
    WorkflowArea.DIAGNOSIS -> "Diagnosis"
    WorkflowArea.LAB_IMAGING -> "Lab / Imaging"
    WorkflowArea.PHARMACY -> "Pharmacy"
    WorkflowArea.WARDS -> "Wards"
    WorkflowArea.BILLING -> "Billing / M-Pesa"
    WorkflowArea.INVENTORY -> "Inventory"
    WorkflowArea.NOTIFICATIONS -> "SMS / Alerts"
    WorkflowArea.ADMIN -> "Admin"
    WorkflowArea.REPORTS -> "Reports"
    WorkflowArea.SETTINGS -> "Settings"
    WorkflowArea.MOH_REPORTS -> "MOH Reports"
}

private fun areaIcon(area: WorkflowArea): ImageVector = when (area) {
    WorkflowArea.DASHBOARD -> Icons.Filled.Dashboard
    WorkflowArea.RECEPTION -> Icons.Filled.People
    WorkflowArea.APPOINTMENTS -> Icons.Filled.CalendarMonth
    WorkflowArea.CONSULTATION -> Icons.Filled.MedicalServices
    WorkflowArea.DIAGNOSIS -> Icons.Filled.LocalHospital
    WorkflowArea.LAB_IMAGING -> Icons.Filled.Science
    WorkflowArea.PHARMACY -> Icons.Filled.Medication
    WorkflowArea.WARDS -> Icons.Filled.LocalHospital
    WorkflowArea.BILLING -> Icons.Filled.Payments
    WorkflowArea.INVENTORY -> Icons.Filled.Inventory2
    WorkflowArea.REPORTS, WorkflowArea.MOH_REPORTS -> Icons.Filled.Assessment
    WorkflowArea.NOTIFICATIONS -> Icons.Filled.Sms
    WorkflowArea.ADMIN -> Icons.Filled.AdminPanelSettings
    WorkflowArea.SETTINGS -> Icons.Filled.Settings
}

