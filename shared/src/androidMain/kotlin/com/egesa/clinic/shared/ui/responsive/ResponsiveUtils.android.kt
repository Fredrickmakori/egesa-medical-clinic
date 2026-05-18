package com.egesa.clinic.shared.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal actual fun getScreenWidthDp(): Int {
    return LocalConfiguration.current.screenWidthDp
}

@Composable
internal actual fun getScreenHeightDp(): Int {
    return LocalConfiguration.current.screenHeightDp
}

