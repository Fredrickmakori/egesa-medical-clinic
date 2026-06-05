package com.egesa.clinic.shared.ui

/**
 * Controls which shell chrome is used after login.
 *
 * - [Adaptive]: Pick layout from window width (desktop / web default).
 * - [MobileNative]: Android and iOS entry points; compact mobile chrome with width-aware fallbacks.
 */
enum class AppUiMode {
    Adaptive,
    MobileNative,
}
