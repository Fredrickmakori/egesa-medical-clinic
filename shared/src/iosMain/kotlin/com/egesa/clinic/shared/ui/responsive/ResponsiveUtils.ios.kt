package com.egesa.clinic.shared.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun getScreenWidthDp(): Int {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return with(density) { size.width.toDp().value.toInt() }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun getScreenHeightDp(): Int {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return with(density) { size.height.toDp().value.toInt() }
}
