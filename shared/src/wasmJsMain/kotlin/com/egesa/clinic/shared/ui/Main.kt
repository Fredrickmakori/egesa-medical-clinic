package com.egesa.clinic.shared.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import com.egesa.clinic.shared.db.DatabaseDriverFactory

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val databaseDriverFactory = DatabaseDriverFactory()

    ComposeViewport(document.body!!) {
        ClinicApp(
            platform = ClientPlatform.Desktop,  // Web uses desktop-like UI layout
            databaseDriverFactory = databaseDriverFactory
        )
    }
}
