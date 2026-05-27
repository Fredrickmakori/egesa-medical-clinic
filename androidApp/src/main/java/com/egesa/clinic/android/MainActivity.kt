@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.egesa.clinic.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.egesa.clinic.shared.ui.ClinicApp
import com.egesa.clinic.shared.ui.ClientPlatform
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val driverFactory = remember { DatabaseDriverFactory(this) }
            ClinicApp(ClientPlatform.Tablet, driverFactory)
        }
    }
}
