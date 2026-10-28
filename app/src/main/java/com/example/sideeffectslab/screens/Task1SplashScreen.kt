package com.example.sideeffectslab.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val TAG = "Task1Splash"
private const val SplashWaitTimeMillis = 2000L

/**
 * Task 1. Splash screen.
 *
 * The parent makes a new lambda every 500 ms. After 2 seconds the two variants below
 * call their callback, and we see in Logcat which version they got.
 */
@Composable
fun Task1SplashScreen() {
    var version by remember { mutableIntStateOf(0) }

    // every 500 ms a new version, so the parent recomposes and passes a new lambda
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            version++
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Task 1: splash + rememberUpdatedState")
        Text("current version = v$version")

        Spacer(Modifier.height(16.dp))

        // a plain Int, so every recomposition really builds a different lambda
        // (if I wrote version straight inside the lambda, it would read the state later
        // and both variants would print the same thing)
        val v = version

        LandingScreenCaptured(onTimeout = { Log.d(TAG, "CAPTURED  -> v$v") })
        LandingScreen(onTimeout = { Log.d(TAG, "UPDATED   -> v$v") })
    }
}

/** Bad variant: the coroutine catches the first lambda and keeps it. */
@Composable
private fun LandingScreenCaptured(onTimeout: () -> Unit) {
    LaunchedEffect(true) {
        delay(SplashWaitTimeMillis)
        onTimeout()
    }
    Text("variant 1: captured lambda", modifier = Modifier.fillMaxWidth())
}

/** The real one: the lambda is read at the moment of the call. */
@Composable
private fun LandingScreen(onTimeout: () -> Unit) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(true) {
        delay(SplashWaitTimeMillis)
        currentOnTimeout()
    }
    Text("variant 2: rememberUpdatedState", modifier = Modifier.fillMaxWidth())
}

/*
My run:
    Task1Splash  CAPTURED  -> v0
    Task1Splash  UPDATED   -> v4

The first one is stuck on v0, its coroutine started on the first composition and kept that
lambda.

The experiment from the task: with LaunchedEffect(onTimeout) nothing is printed at all. A new
lambda every 500 ms means a new key every 500 ms, so the effect keeps restarting and
delay(2000) never finishes.
*/
