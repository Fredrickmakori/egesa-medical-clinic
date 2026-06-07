package com.egesa.clinic.shared.ui

import com.egesa.clinic.shared.db.DatabaseDriverFactory
import com.egesa.clinic.shared.ui.ClientPlatform
import com.egesa.clinic.shared.ui.mobile.MobileClinicApp
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

import androidx.compose.runtime.remember

fun MainViewController(databaseDriverFactory: DatabaseDriverFactory = DatabaseDriverFactory()): UIViewController = ComposeUIViewController {
    val factory = remember { databaseDriverFactory }
    MobileClinicApp(
        platform = ClientPlatform.Mobile,
        databaseDriverFactory = factory,
    )
}
