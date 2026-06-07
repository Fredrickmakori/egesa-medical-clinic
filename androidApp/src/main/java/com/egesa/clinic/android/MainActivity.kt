package com.egesa.clinic.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import com.egesa.clinic.shared.ui.ClientPlatform
import com.egesa.clinic.shared.ui.ClinicApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClinicApp(
                platform = ClientPlatform.Tablet,
                databaseDriverFactory = DatabaseDriverFactory(this),
            )
        }
    }
}
