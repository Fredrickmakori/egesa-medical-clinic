@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.egesa.clinic.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.egesa.clinic.shared.ui.ClinicApp
import com.egesa.clinic.shared.ui.ClientPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClinicApp(ClientPlatform.Tablet)
        }
    }
}
