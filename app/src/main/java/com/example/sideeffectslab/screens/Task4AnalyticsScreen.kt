package com.example.sideeffectslab.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val TAG = "Task4Analytics"

/** A fake external library. Compose knows nothing about this object. */
object Analytics {
    var userType: String = "unknown"
        set(value) {
            field = value
            Log.d(TAG, "Analytics.userType = $value")
        }
}

@Composable
fun Task4AnalyticsScreen() {
    var userType by remember { mutableStateOf("free") }
    var counter by remember { mutableIntStateOf(0) }

    // A plain array, NOT a mutableState. If I counted with state here, writing it inside
    // SideEffect would cause one more recomposition, and that would never stop.
    val sideEffectRuns = remember { intArrayOf(0) }

    SideEffect {
        Analytics.userType = userType
        sideEffectRuns[0]++
        Log.d(TAG, "SideEffect run #${sideEffectRuns[0]}, userType = $userType")
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Task 4: SideEffect")
        Text("userType = $userType")
        Text("counter = $counter")
        Text("SideEffect ran ${sideEffectRuns[0]} times")

        Button(onClick = { userType = if (userType == "free") "premium" else "free" }) {
            Text("Change userType")
        }

        // this button does not touch userType, but it still causes a recomposition
        Button(onClick = { counter++ }) {
            Text("Just recompose")
        }
    }
}

/*
Pressed "Just recompose" 3 times. userType stayed the same, but SideEffect still ran 3 more
times and wrote the same value again. That is normal, it has no keys. Its job is not "react to
a change", it is "keep the outside object equal to what is on the screen now".

Why not write Analytics.userType = userType straight in the body: that line would run during a
composition attempt too, and Compose can throw an attempt away. Then the analytics would have a
value that never appeared on the screen. SideEffect only runs after the commit.
*/
