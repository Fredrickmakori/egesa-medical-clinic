package com.egesa.clinic.shared.ui.shell

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.WorkflowArea
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.ui.components.Avatar
import com.egesa.clinic.shared.ui.components.RoleBadge
import com.egesa.clinic.shared.ui.navigation.ClinicNavItem
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.navigation.navItemsFor
import com.egesa.clinic.shared.ui.screens.AreaScreen
import com.egesa.clinic.shared.ui.theme.*

private const val BOTTOM_NAV_LIMIT = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletShell(session: SessionState, localRepository: LocalRepository, onLogout: () -> Unit) {
    val navItems     = remember(session.role) { navItemsFor(session.role) }
    val primaryItems = navItems.take(BOTTOM_NAV_LIMIT)
    var activeArea   by remember { mutableStateOf(navItems.first().area) }
    var drawerOpen   by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TabletTopBar(
                session    = session,
                onMenuClick = { drawerOpen = true },
                onLogout   = onLogout,
            )
        },
        bottomBar = {
            TabletBottomNav(
                items      = primaryItems,
                allItems   = navItems,
                activeArea = activeArea,
                onSelect   = { activeArea = it },
                hasMore    = navItems.size > BOTTOM_NAV_LIMIT,
                onMoreClick = { drawerOpen = true },
            )
        },
        containerColor = Slate50,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AreaScreen(area = activeArea, session = session, localRepository = localRepository)
        }
    }

    // ── Overflow drawer ──────────────────────────────────────────────────────
    if (drawerOpen) {
        ModalBottomSheet(
            onDismissRequest = { drawerOpen = false },
            containerColor   = White,
            shape            = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                Text(
                    "All Modules",
                    style   = MaterialTheme.typography.titleMedium,
                    color   = Slate900,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                navItems.drop(BOTTOM_NAV_LIMIT).forEach { item ->
                    OverflowItem(
                        item     = item,
                        selected = activeArea == item.area,
                        onClick  = { activeArea = it; drawerOpen = false },
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Slate200)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Slate100).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Avatar(initials = session.initials, size = 36)
                        Column {
                            Text(session.fullName, style = MaterialTheme.typography.titleSmall)
                            Text(session.shiftLabel, style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                    }
                    TextButton(onClick = { drawerOpen = false; onLogout() }) {
                        Text("Sign out", color = StatusCritical)
                    }
                }
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletTopBar(
    session: SessionState,
    onMenuClick: () -> Unit,
    onLogout: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(Navy800),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("EC", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("Egesa Clinic", style = MaterialTheme.typography.titleMedium, color = Slate900)
            }
        },
        actions = {
            // Search stub
            Box(
                Modifier.width(240.dp).height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Slate100),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⌕", color = Slate400, fontSize = 14.sp)
                    Text("Search…", style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
            }
            Spacer(Modifier.width(8.dp))
            RoleBadge(session.role.name)
            Spacer(Modifier.width(8.dp))
            Avatar(initials = session.initials, size = 34, modifier = Modifier.padding(end = 12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = White,
            titleContentColor = Slate900,
        ),
    )
}

// ── Bottom navigation ──────────────────────────────────────────────────────────

@Composable
private fun TabletBottomNav(
    items: List<ClinicNavItem>,
    allItems: List<ClinicNavItem>,
    activeArea: WorkflowArea,
    onSelect: (WorkflowArea) -> Unit,
    hasMore: Boolean,
    onMoreClick: () -> Unit,
) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 0.dp,
        modifier       = Modifier.height(66.dp),
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = activeArea == item.area,
                onClick  = { onSelect(item.area) },
                icon     = {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(7.dp))
                            .background(if (activeArea == item.area) Teal100 else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.shortLabel.take(2),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (activeArea == item.area) Teal700 else Slate400,
                            letterSpacing = 0.3.sp,
                        )
                    }
                },
                label = {
                    Text(
                        item.label,
                        fontSize   = 11.sp,
                        fontWeight = if (activeArea == item.area) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Teal700,
                    selectedTextColor   = Teal700,
                    unselectedIconColor = Slate400,
                    unselectedTextColor = Slate400,
                    indicatorColor      = Color.Transparent,
                ),
            )
        }
        if (hasMore) {
            NavigationBarItem(
                selected = false,
                onClick  = onMoreClick,
                icon     = {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⋯", fontSize = 14.sp, color = Slate400)
                    }
                },
                label    = { Text("More", fontSize = 11.sp, color = Slate400) },
                colors   = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
            )
        }
    }
}

@Composable
private fun OverflowItem(
    item: ClinicNavItem,
    selected: Boolean,
    onClick: (WorkflowArea) -> Unit,
) {
    val bg = if (selected) Navy50 else Color.Transparent
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(if (selected) Teal100 else Slate100),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.shortLabel.take(2),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = if (selected) Teal700 else Slate500, letterSpacing = 0.5.sp,
            )
        }
        Text(
            item.label,
            modifier   = Modifier.weight(1f).clickable { onClick(item.area) },
            style      = MaterialTheme.typography.bodyMedium,
            color      = if (selected) Navy800 else Slate700,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (selected) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Teal600))
        }
    }
}
