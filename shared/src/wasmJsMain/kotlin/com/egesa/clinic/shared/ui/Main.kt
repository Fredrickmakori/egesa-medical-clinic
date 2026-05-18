package com.egesa.clinic.shared.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import com.egesa.clinic.shared.db.DatabaseDriverFactory

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val databaseDriverFactory = DatabaseDriverFactory()

    val root = document.getElementById("root") ?: document.body!!
    root.className = "" // Clear loading state

    ComposeViewport(root) {
        ClinicApp(
            platform = ClientPlatform.Desktop,  // Web uses desktop-like UI layout
            databaseDriverFactory = databaseDriverFactory
        )
    }
}
