package com.example.sideeffectslab.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val TAG = "Task3Lifecycle"

/**
 * Task 3. Lifecycle logger.
 *
 * The Switch shows and hides the logger, so we can see add/remove pairs in Logcat.
 */
@Composable
fun Task3LifecycleScreen() {
    var visible by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Task 3: DisposableEffect")

        Row {
            Text("show logger")
            Switch(checked = visible, onCheckedChange = { visible = it })
        }

        if (visible) {
            LifecycleLogger(
                onStart = { Log.d(TAG, "ON_START") },
                onStop = { Log.d(TAG, "ON_STOP") }
            )
        }
    }
}

@Composable
private fun LifecycleLogger(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    // the lambdas must not be keys, otherwise the observer is removed and added
    // again on every recomposition
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnStop by rememberUpdatedState(onStop)

    DisposableEffect(lifecycleOwner) {
        Log.d(TAG, "addObserver()")

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> currentOnStart()
                Lifecycle.Event.ON_STOP -> currentOnStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            Log.d(TAG, "removeObserver()")
        }
    }

    Text("lifecycle logger is working")
}

/*
Switched it 5 times, got 5 addObserver() and 5 removeObserver() lines, always in pairs.

When I deleted onDispose it did not even build:
    Type mismatch: inferred type is Unit but DisposableEffectResult was expected
So the compiler does not let you forget the cleanup here, which is nice.
*/
