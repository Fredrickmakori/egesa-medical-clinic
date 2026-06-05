package com.egesa.clinic.shared.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.WorkflowArea
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.ui.navigation.ClinicNavItem
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.screens.AreaScreen
import com.egesa.clinic.shared.ui.theme.Slate200
import com.egesa.clinic.shared.ui.theme.Slate50
import com.egesa.clinic.shared.ui.theme.Slate500
import com.egesa.clinic.shared.ui.theme.White

/**
 * Compact mobile shell with bottom navigation.
 * Used on phones and narrow layouts.
 */
@Composable
fun CompactMobileShell(
    session: SessionState,
    localRepository: LocalRepository,
    documentCaptureGateway: DocumentCaptureGateway,
    navItems: List<ClinicNavItem>,
    activeArea: WorkflowArea,
    onAreaSelected: (WorkflowArea) -> Unit,
    bottomNavVisible: Boolean = true,
    bottomNavLimit: Int? = null,
    overflowSheetOpen: Boolean = false,
    onOverflowOpen: (() -> Unit)? = null,
    onOverflowDismiss: (() -> Unit)? = null,
    onLogout: () -> Unit,
) {
    val primaryItems = bottomNavLimit?.let { navItems.take(it) } ?: navItems
    val overflowItems = bottomNavLimit?.let { navItems.drop(it) }.orEmpty()
    val hasMore = bottomNavLimit != null && navItems.size > bottomNavLimit

    Column(Modifier.fillMaxSize()) {
        MobileTopBar(session = session, activeArea = activeArea, onLogout = onLogout)
        MobileSearchBar(session = session)
        HorizontalDivider(color = Slate200, thickness = 1.dp)

        Box(
            Modifier.weight(1f).fillMaxWidth().background(Slate50),
            contentAlignment = Alignment.TopStart,
        ) {
            if (navItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No modules are available for your role. Contact an administrator.",
                        color = Slate500,
                    )
                }
            } else {
                AreaScreen(
                    area = activeArea,
                    session = session,
                    localRepository = localRepository,
                    documentCaptureGateway = documentCaptureGateway,
                )
            }
        }

        if (bottomNavVisible && navItems.isNotEmpty()) {
            HorizontalDivider(color = Slate200, thickness = 1.dp)
            MobileBottomNavigationBar(
                items = primaryItems,
                activeArea = activeArea,
                onAreaSelected = onAreaSelected,
                showMoreItem = hasMore,
                onMoreClick = onOverflowOpen,
            )
        }
    }

    if (overflowSheetOpen && hasMore && onOverflowDismiss != null) {
        MobileModuleOverflowSheet(
            navItems = navItems,
            overflowItems = overflowItems,
            activeArea = activeArea,
            session = session,
            onDismiss = onOverflowDismiss,
            onSelectArea = onAreaSelected,
            onLogout = onLogout,
        )
    }
}
