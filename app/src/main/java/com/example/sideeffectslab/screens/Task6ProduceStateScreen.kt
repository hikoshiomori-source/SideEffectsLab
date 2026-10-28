package com.example.sideeffectslab.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val TAG = "Task6Produce"

sealed interface ImageState {
    data object Loading : ImageState
    data class Success(val url: String) : ImageState
    data class Error(val message: String) : ImageState
}

/** Fake repository. */
object FakeImageRepository {
    suspend fun load(url: String): ImageState {
        delay(500)
        return if (url.endsWith(".png")) {
            ImageState.Success(url)
        } else {
            ImageState.Error("only .png is supported")
        }
    }
}

/**
 * Task 6. produceState.
 *
 * The name starts with a lowercase letter, because this composable returns a value
 * and does not emit any UI.
 */
@Composable
fun loadNetworkImage(url: String): State<ImageState> {
    return produceState<ImageState>(initialValue = ImageState.Loading, key1 = url) {
        Log.d(TAG, "producer started for $url")
        try {
            value = ImageState.Loading

            val loaded = FakeImageRepository.load(url)
            // written twice on purpose: the second one is the same value, so it should
            // not cause a second recomposition
            value = loaded
            value = loaded
        } finally {
            Log.d(TAG, "producer finished / cancelled for $url")
        }
    }
}

@Composable
fun Task6ProduceStateScreen() {
    var url by remember { mutableStateOf("cat.png") }
    val imageState by loadNetworkImage(url)

    val recompositions = remember { intArrayOf(0) }
    recompositions[0]++

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Task 6: produceState")
        Text("url = $url")
        Text("body ran ${recompositions[0]} times")

        when (val state = imageState) {
            is ImageState.Loading -> CircularProgressIndicator()
            is ImageState.Success -> Text("loaded: ${state.url}")
            is ImageState.Error -> Text("error: ${state.message}")
        }

        Button(onClick = { url = if (url == "cat.png") "dog.jpg" else "cat.png" }) {
            Text("Change url")
        }
    }
}

/** My own produceState, from Question 11 of Part A. */
@Composable
fun <T> myProduceState(
    initialValue: T,
    key: Any?,
    producer: suspend MutableState<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }

    LaunchedEffect(key) {
        result.producer()
    }

    return result
}

/*
Changing the url gives:
    producer finished / cancelled for cat.png
    producer started for dog.jpg
and the spinner comes back, because the state goes to Loading again. Leaving the screen in the
middle of loading prints only the first line, so the coroutine really is cancelled.

The result is written twice in a row and the counter on the screen goes up by one, not by two.
MutableState compares the new value with the old one using equals(), and Success is a data
class, so the second write is thrown away.

(My first attempt at this experiment was wrong. I wrote Loading twice, but the state was
already Loading, so neither write did anything and there was nothing to see.)
*/
