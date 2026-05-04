package com.egesa.clinic.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val supabase = createSupabaseClient(
    supabaseUrl = "https://vigeqwzqasblsnetbprm.supabase.co",
    supabaseKey = "sb_publishable_b8klbj4PwcFfW-eKpZ2G_A_OTXS5OVD"
) {
    install(Postgrest)
}

@Serializable
data class TodoItem(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodoList()
                }
            }
        }
    }
}

@Composable
fun TodoList() {
    var itemsState by remember { mutableStateOf<List<TodoItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        itemsState = supabase
            .from("todos")
            .select()
            .decodeList<TodoItem>()
    }

    LazyColumn {
        items(
            itemsState,
            key = { item -> item.id },
        ) { item ->
            Text(
                item.name,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
