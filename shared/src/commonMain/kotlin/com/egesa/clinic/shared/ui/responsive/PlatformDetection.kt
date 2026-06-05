package com.egesa.clinic.shared.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Detects the device form factor and provides platform-specific configuration.
 * Helps structure UI to be automatically optimized for different input methods and screen sizes.
 */
@Composable
fun rememberDeviceType(): DeviceType {
    val screenWidthDp = getScreenWidthDp()
    val screenHeightDp = getScreenHeightDp()

    return when {
        // Tablet or desktop: wide screen > 600dp
        screenWidthDp > 600 -> {
            if (isLandscape(screenWidthDp, screenHeightDp)) {
                DeviceType.DESKTOP_LANDSCAPE
            } else {
                DeviceType.TABLET
            }
        }
        // Mobile portrait
        !isLandscape(screenWidthDp, screenHeightDp) -> DeviceType.MOBILE_PORTRAIT
        // Mobile landscape
        else -> DeviceType.MOBILE_LANDSCAPE
    }
}

private fun isLandscape(width: Int, height: Int): Boolean = width > height

enum class DeviceType {
    MOBILE_PORTRAIT,      // Phone in portrait mode
    MOBILE_LANDSCAPE,     // Phone in landscape mode
    TABLET,              // Tablet/iPad size
    DESKTOP_LANDSCAPE    // Desktop monitor or large screen
}

/**
 * Determines input method based on device type.
 * - MOBILE: Touch/Virtual keyboard (PIN pad/eye icon)
 * - DESKTOP: Keyboard/Mouse (numeric keyboard + Enter)
 */
fun DeviceType.getInputMethod(): InputMethod = when (this) {
    DeviceType.MOBILE_PORTRAIT, DeviceType.MOBILE_LANDSCAPE -> InputMethod.TOUCH_AND_KEYBOARD
    else -> InputMethod.KEYBOARD_PRIMARY
}

enum class InputMethod {
    KEYBOARD_PRIMARY,      // Desktop: keyboard + Enter submission
    TOUCH_AND_KEYBOARD     // Mobile: PIN pad + virtual keyboard
}

/**
 * Gets optimal layout configuration based on device type
 */
@Composable
fun DeviceType.getLayoutConfig(): LayoutConfig {
    return when (this) {
        DeviceType.MOBILE_PORTRAIT -> LayoutConfig(
            pinEntryWidth = 320.dp,
            pinPadHeight = 280.dp,
            showSideBranding = false,
            useFullWidth = true,
            pinDotSize = 14.dp,
            keyboardHeight = 200.dp
        )
        DeviceType.MOBILE_LANDSCAPE -> LayoutConfig(
            pinEntryWidth = 400.dp,
            pinPadHeight = 200.dp,
            showSideBranding = false,
            useFullWidth = true,
            pinDotSize = 12.dp,
            keyboardHeight = 150.dp
        )
        DeviceType.TABLET -> LayoutConfig(
            pinEntryWidth = 450.dp,
            pinPadHeight = 250.dp,
            showSideBranding = true,
            useFullWidth = false,
            pinDotSize = 16.dp,
            keyboardHeight = 0.dp
        )
        DeviceType.DESKTOP_LANDSCAPE -> LayoutConfig(
            pinEntryWidth = 480.dp,
            pinPadHeight = 0.dp,  // No PIN pad on desktop
            showSideBranding = true,
            useFullWidth = false,
            pinDotSize = 15.dp,
            keyboardHeight = 0.dp
        )
    }
}

/**
 * Layout configuration for responsive UI
 */
data class LayoutConfig(
    val pinEntryWidth: androidx.compose.ui.unit.Dp,
    val pinPadHeight: androidx.compose.ui.unit.Dp,
    val showSideBranding: Boolean,
    val useFullWidth: Boolean,
    val pinDotSize: androidx.compose.ui.unit.Dp,
    val keyboardHeight: androidx.compose.ui.unit.Dp
)

