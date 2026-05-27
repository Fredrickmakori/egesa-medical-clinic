package com.egesa.clinic.shared.ui

import com.egesa.clinic.shared.db.DatabaseDriverFactory
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

import androidx.compose.runtime.remember

fun MainViewController(databaseDriverFactory: DatabaseDriverFactory = DatabaseDriverFactory()): UIViewController = ComposeUIViewController {
    val factory = remember { databaseDriverFactory }
    ClinicApp(ClientPlatform.Tablet, factory)
}
