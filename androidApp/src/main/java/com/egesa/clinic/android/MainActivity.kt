package com.egesa.clinic.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egesa.clinic.shared.HospitalState
import com.egesa.clinic.shared.UserRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = HospitalState()
        setContent {
            MaterialTheme {
                AndroidAppShell(state)
            }
        }
    }
}

@Composable
private fun AndroidAppShell(state: HospitalState) {
    val role = UserRole.ADMIN
    val navItems = state.globalNavItemsFor(role)
    var activeIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Egesa Mobile") },
                actions = { Text("Search • Register • Alerts • Profile", Modifier.padding(end = 12.dp)) }
            )
        },
        bottomBar = {
            BottomAppBar {
                NavigationBar {
                    navItems.take(4).forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = activeIndex == index,
                            onClick = { activeIndex = index },
                            icon = { Text(item.label.take(1)) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val selected = navItems.getOrNull(activeIndex)
            Text("${state.breadcrumbFor(selected?.area ?: navItems.first().area).joinToString(" > ")}")
            Text(selected?.visibilityAnnotation.orEmpty())

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.allPatients()) { patient ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(patient.fullName)
                            Text("${patient.id} • ${patient.status}")
                        }
                    }
                }
            }
        }
    }
}
