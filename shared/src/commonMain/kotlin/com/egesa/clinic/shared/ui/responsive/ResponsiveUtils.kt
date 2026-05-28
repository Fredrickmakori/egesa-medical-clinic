package com.egesa.clinic.shared.ui.responsive

import androidx.compose.runtime.Composable

/**
 * Responsive breakpoints for different screen sizes
 * Follows Material Design 3 standards
 */
enum class WindowWidthSize {
    COMPACT,      // < 600 dp (phones)
    MEDIUM,       // 600 - 839 dp (small tablets, foldables)
    EXPANDED,     // >= 840 dp (large tablets, desktops)
}

enum class WindowHeightSize {
    COMPACT,      // < 480 dp
    MEDIUM,       // 480 - 899 dp
    EXPANDED,     // >= 900 dp
}

/**
 * Current window size class based on screen dimensions
 */
data class WindowSizeClass(
    val widthSize: WindowWidthSize,
    val heightSize: WindowHeightSize,
) {
    companion object {
        const val COMPACT_WIDTH = 600
        const val MEDIUM_WIDTH = 840
        const val COMPACT_HEIGHT = 480
        const val MEDIUM_HEIGHT = 900
    }
}

@Composable
internal expect fun getScreenWidthDp(): Int

@Composable
internal expect fun getScreenHeightDp(): Int

/**
 * Get current window size class based on screen configuration
 * Usage: val windowSize = currentWindowSizeClass()
 */
@Composable
fun currentWindowSizeClass(): WindowSizeClass {
    val widthDp = getScreenWidthDp()
    val heightDp = getScreenHeightDp()

    val widthSize = when {
        widthDp < WindowSizeClass.COMPACT_WIDTH -> WindowWidthSize.COMPACT
        widthDp < WindowSizeClass.MEDIUM_WIDTH -> WindowWidthSize.MEDIUM
        else -> WindowWidthSize.EXPANDED
    }

    val heightSize = when {
        heightDp < WindowSizeClass.COMPACT_HEIGHT -> WindowHeightSize.COMPACT
        heightDp < WindowSizeClass.MEDIUM_HEIGHT -> WindowHeightSize.MEDIUM
        else -> WindowHeightSize.EXPANDED
    }

    return WindowSizeClass(widthSize, heightSize)
}

/**
 * Responsive padding based on screen size
 * Examples:
 *   - COMPACT: 16.dp
 *   - MEDIUM: 20.dp
 *   - EXPANDED: 24.dp
 */
@Composable
fun responsiveHorizontalPadding(): Int = when (currentWindowSizeClass().widthSize) {
    WindowWidthSize.COMPACT -> 16
    WindowWidthSize.MEDIUM -> 20
    WindowWidthSize.EXPANDED -> 24
}

/**
 * Responsive padding for vertical spacing
 */
@Composable
fun responsiveVerticalPadding(): Int = when (currentWindowSizeClass().heightSize) {
    WindowHeightSize.COMPACT -> 8
    WindowHeightSize.MEDIUM -> 12
    WindowHeightSize.EXPANDED -> 16
}

/**
 * Responsive spacing between items
 */
@Composable
fun responsiveItemSpacing(): Int = when (currentWindowSizeClass().widthSize) {
    WindowWidthSize.COMPACT -> 8
    WindowWidthSize.MEDIUM -> 12
    WindowWidthSize.EXPANDED -> 16
}

/**
 * Responsive font size multiplier
 */
@Composable
fun responsiveFontScale(): Float = when (currentWindowSizeClass().widthSize) {
    WindowWidthSize.COMPACT -> 0.9f
    WindowWidthSize.MEDIUM -> 1.0f
    WindowWidthSize.EXPANDED -> 1.1f
}

/**
 * Sidebar width based on screen size
 * - COMPACT: Hidden (navigation drawer)
 * - MEDIUM: 240 dp (collapsible)
 * - EXPANDED: 240-280 dp
 */
@Composable
fun responsiveSidebarWidth(): Int = when (currentWindowSizeClass().widthSize) {
    WindowWidthSize.COMPACT -> 0    // Use bottom navigation
    WindowWidthSize.MEDIUM -> 240
    WindowWidthSize.EXPANDED -> 280
}

/**
 * Content max width for readability on large screens
 * - COMPACT: Full width
 * - MEDIUM: Full width
 * - EXPANDED: 1200 dp max
 */
@Composable
fun responsiveContentMaxWidth(): Int = when (currentWindowSizeClass().widthSize) {
    WindowWidthSize.COMPACT -> Int.MAX_VALUE
    WindowWidthSize.MEDIUM -> Int.MAX_VALUE
    WindowWidthSize.EXPANDED -> 1200
}

/**
 * Column count for grid layouts
 */
@Composable
fun responsiveGridColumns(): Int = when (currentWindowSizeClass().widthSize) {
    WindowWidthSize.COMPACT -> 1  // Single column
    WindowWidthSize.MEDIUM -> 2   // Two columns
    WindowWidthSize.EXPANDED -> 3 // Three columns
}

/**
 * Determine if layout should use horizontal (side-by-side) or vertical (stacked) organization
 */
@Composable
fun isWideLayout(): Boolean =
    currentWindowSizeClass().widthSize >= WindowWidthSize.MEDIUM

/**
 * Determine if we should show navigation drawer or bottom navigation
 */
@Composable
fun shouldUseDrawerNavigation(): Boolean =
    currentWindowSizeClass().widthSize >= WindowWidthSize.MEDIUM

/**
 * Determine if we should show compact UI (mobile-optimized)
 */
@Composable
fun shouldUseCompactUI(): Boolean =
    currentWindowSizeClass().widthSize <= WindowWidthSize.COMPACT

/**
 * Determine if layout is portrait or landscape
 */
@Composable
fun isPortraitOrientation(): Boolean {
    return getScreenHeightDp() > getScreenWidthDp()
}

/**
 * Sidebar visibility - visible on medium and large screens
 */
@Composable
fun shouldShowSidebar(): Boolean =
    shouldShowSidebarInternal()

@Composable
private fun shouldShowSidebarInternal(): Boolean =
    responsiveSidebarWidth() > 0

/**
 * Top bar configuration based on screen size
 */
@Composable
fun responsiveTopBarHeight(): Int = when (currentWindowSizeClass().widthSize) {
    WindowWidthSize.COMPACT -> 56   // Mobile height
    WindowWidthSize.MEDIUM -> 56
    WindowWidthSize.EXPANDED -> 64  // Slightly taller on desktop
}

/**
 * Responsive navigation bar height (bottom nav for mobile)
 */
@Composable
fun responsiveNavBarHeight(): Int = when (currentWindowSizeClass().widthSize) {
    WindowWidthSize.COMPACT -> 80  // Visible
    WindowWidthSize.MEDIUM -> 0    // Hidden, use drawer
    WindowWidthSize.EXPANDED -> 0  // Hidden, use drawer
}
