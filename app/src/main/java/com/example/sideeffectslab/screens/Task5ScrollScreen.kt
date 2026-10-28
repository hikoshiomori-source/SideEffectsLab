package com.example.sideeffectslab.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "Task5Scroll"

// Task 5. Two buttons that do the same thing, but one of them recomposes much less.
@Composable
fun Task5ScrollScreen() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val items = remember { List(100) { "Item $it" } }

    // one analytics event when the user goes past the first item
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> index > 0 }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                Log.d(TAG, "ANALYTICS: scrolled past the first item")
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState) {
            items(items) { item ->
                Text(item, modifier = Modifier.padding(16.dp))
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            GoodButton(listState) { scope.launch { listState.animateScrollToItem(0) } }
            BadButton(listState) { scope.launch { listState.animateScrollToItem(0) } }
        }
    }
}

/** Variant 1: derivedStateOf, recomposes only when the answer changes. */
@Composable
private fun GoodButton(listState: LazyListState, onClick: () -> Unit) {
    val recompositions = remember { intArrayOf(0) }
    SideEffect { recompositions[0]++ }

    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Text("derivedStateOf variant: ${recompositions[0]} recompositions")
    if (showButton) {
        Button(onClick = onClick) { Text("Scroll to top (good)") }
    }
}

/** Variant 2: the index is read directly, so it recomposes on every item. */
@Composable
private fun BadButton(listState: LazyListState, onClick: () -> Unit) {
    val recompositions = remember { intArrayOf(0) }
    SideEffect { recompositions[0]++ }

    val showButton = listState.firstVisibleItemIndex > 0

    Text("direct read variant: ${recompositions[0]} recompositions")
    if (showButton) {
        Button(onClick = onClick) { Text("Scroll to top (bad)") }
    }
}

/*
Scrolled down to item 40 and back. The counters on the screen said 3 for derivedStateOf and 78
for the direct read. Logcat printed the analytics line once per crossing.

If I take distinctUntilChanged() out, the line is printed for every new index while scrolling
down, which is exactly the thing we are trying to avoid.
*/
