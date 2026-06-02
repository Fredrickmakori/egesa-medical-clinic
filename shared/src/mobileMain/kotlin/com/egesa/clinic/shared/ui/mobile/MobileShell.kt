package com.egesa.clinic.shared.ui.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.egesa.clinic.shared.data.DocumentCaptureGateway
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.navigation.navItemsFor
import com.egesa.clinic.shared.ui.navigation.roleCanAccessArea
import com.egesa.clinic.shared.ui.navigation.safeDefaultLandingArea
import com.egesa.clinic.shared.ui.responsive.shouldUseCompactUI
import com.egesa.clinic.shared.ui.shell.ResponsiveShell

private const val BOTTOM_NAV_LIMIT = 4

/**
 * Shared Android + iOS shell entry point.
 * Phones use compact bottom navigation with overflow; wider mobile layouts keep the adaptive shell.
 */
@Composable
fun MobileShell(
    session: SessionState,
    localRepository: LocalRepository,
    documentCaptureGateway: DocumentCaptureGateway,
    onLogout: () -> Unit,
) {
    if (!shouldUseCompactUI()) {
        ResponsiveShell(
            session = session,
            localRepository = localRepository,
            documentCaptureGateway = documentCaptureGateway,
            onLogout = onLogout,
        )
        return
    }

    val navItems = remember(session.role) { navItemsFor(session.role) }
    val landingArea = remember(session.role) { safeDefaultLandingArea(session.role) }
    var activeArea by remember(session.role) { mutableStateOf(landingArea) }
    var overflowOpen by remember { mutableStateOf(false) }

    LaunchedEffect(session.role, navItems) {
        if (navItems.isEmpty()) return@LaunchedEffect
        if (!roleCanAccessArea(session.role, activeArea)) {
            activeArea = landingArea
        }
    }

    CompactMobileShell(
        session = session,
        localRepository = localRepository,
        documentCaptureGateway = documentCaptureGateway,
        navItems = navItems,
        activeArea = activeArea,
        onAreaSelected = { activeArea = it },
        bottomNavLimit = BOTTOM_NAV_LIMIT,
        overflowSheetOpen = overflowOpen,
        onOverflowOpen = { overflowOpen = true },
        onOverflowDismiss = { overflowOpen = false },
        onLogout = onLogout,
    )
}
