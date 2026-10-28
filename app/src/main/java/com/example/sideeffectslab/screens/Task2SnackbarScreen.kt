package com.example.sideeffectslab.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val TAG = "Task2Snackbar"

// Task 2. The snackbar is shown from onClick, so we need a scope and not LaunchedEffect.
@Composable
fun Task2SnackbarScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val taps = remember { mutableIntStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Task 2: rememberCoroutineScope")

            Button(onClick = {
                // LaunchedEffect does not work here: it is a @Composable function and
                // onClick is an ordinary lambda, there is no composition at that moment.
                // The compiler says "@Composable invocations can only happen from the
                // context of a @Composable function".
                val n = ++taps.intValue
                scope.launch {
                    Log.d(TAG, "show snackbar #$n")
                    val result = snackbarHostState.showSnackbar(
                        message = "Snackbar #$n",
                        actionLabel = "OK"
                    )
                    Log.d(TAG, "snackbar #$n finished, result = $result")
                    if (result == SnackbarResult.ActionPerformed) {
                        Log.d(TAG, "user pressed OK")
                    }
                }
            }) {
                Text("Show snackbar")
            }
        }
    }
}

/*
Left the screen while the snackbar was still visible. No crash, and it did not follow me to the
next screen, because the scope dies with the composable.

Tapping twice quickly closes the first snackbar and shows the second one. SnackbarHostState has
a Mutex inside, so only one can be shown at a time:
    show snackbar #1
    show snackbar #2
    snackbar #1 finished, result = Dismissed
*/
