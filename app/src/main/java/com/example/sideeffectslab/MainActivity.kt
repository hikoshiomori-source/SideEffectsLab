package com.example.sideeffectslab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sideeffectslab.screens.Task1SplashScreen
import com.example.sideeffectslab.screens.Task2SnackbarScreen
import com.example.sideeffectslab.screens.Task3LifecycleScreen
import com.example.sideeffectslab.screens.Task4AnalyticsScreen
import com.example.sideeffectslab.screens.Task5ScrollScreen
import com.example.sideeffectslab.screens.Task6ProduceStateScreen
import com.example.sideeffectslab.ui.theme.SideEffectsLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SideEffectsLabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}

/** Every task screen is added to this list. */
private val demos: List<Pair<String, @Composable () -> Unit>> = listOf(
    "Task 1. Splash screen" to { Task1SplashScreen() },
    "Task 2. Snackbar" to { Task2SnackbarScreen() },
    "Task 3. Lifecycle log" to { Task3LifecycleScreen() },
    "Task 4. Analytics" to { Task4AnalyticsScreen() },
    "Task 5. Scroll" to { Task5ScrollScreen() },
    "Task 6. produceState" to { Task6ProduceStateScreen() },
    // task screens are added here, one per task
)

@Composable
private fun App() {
    var openedIndex by remember { mutableStateOf<Int?>(null) }

    val index = openedIndex
    if (index == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("SideEffectsLab")
            demos.forEachIndexed { i, (title, _) ->
                Button(
                    onClick = { openedIndex = i },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(title)
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedButton(
                onClick = { openedIndex = null },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("< back")
            }
            // when we go back, the screen leaves the Composition,
            // so all its effects are cleaned up. Good for testing.
            demos[index].second()
        }
    }
}
