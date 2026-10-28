# SideEffectsLab

Practice 9, side-effects in Jetpack Compose (Lecture 13). Each task is one screen.

Part A answers: [docs/Part-A-answers.md](docs/Part-A-answers.md)

## Which API I used in each task and why

### Task 1

The waiting starts because the screen appeared, so `LaunchedEffect`. The key has to stay constant, otherwise the 2 seconds restart every time the parent builds a new lambda. But then the coroutine sits on the old lambda, so the callback goes through `rememberUpdatedState`.

This one took me the longest. At first I did not understand why a key would be wrong, it felt like the obvious thing to do.

### Task 2

`LaunchedEffect` simply cannot be called from `onClick`, it is a composable function. So `rememberCoroutineScope`. Its scope dies with the screen, which is what we want for a snackbar.

### Task 3

Add needs a matching remove, and `DisposableEffect` is the one API that makes me write `onDispose`. `lifecycleOwner` is the key. The callbacks are not keys, they go through `rememberUpdatedState`, otherwise the observer gets re-registered on every recomposition.

### Task 4

`Analytics` is a plain Kotlin object, Compose does not manage it. `SideEffect` runs after the commit, so a thrown away composition attempt never reaches the library.

### Task 5

Two separate problems here. The button only needs a boolean while the index changes on every item, so `derivedStateOf`. The analytics event needs a stream and not a state, so `snapshotFlow` with `map`, `distinctUntilChanged` and `filter`.

### Task 6

The repository is suspend and the UI needs Loading, Success and Error, which is what `produceState` is for. It starts when the screen appears, restarts on a new url, and is cancelled when the screen leaves.

## Logcat

Task 1, the version of the lambda that each variant got:

```
Task1Splash  CAPTURED  -> v0
Task1Splash  UPDATED   -> v4
```

Task 3, Switch toggled 5 times:

```
Task3Lifecycle  addObserver()
Task3Lifecycle  ON_START
Task3Lifecycle  removeObserver()
Task3Lifecycle  addObserver()
Task3Lifecycle  removeObserver()
Task3Lifecycle  addObserver()
Task3Lifecycle  removeObserver()
Task3Lifecycle  addObserver()
Task3Lifecycle  removeObserver()
Task3Lifecycle  addObserver()
Task3Lifecycle  removeObserver()
```

5 and 5, so nothing is left registered.

Task 4, the SideEffect counter. Run #3 is from the "Just recompose" button, where `userType` did not change at all:

```
Task4Analytics  Analytics.userType = free
Task4Analytics  SideEffect run #1, userType = free
Task4Analytics  Analytics.userType = premium
Task4Analytics  SideEffect run #2, userType = premium
Task4Analytics  Analytics.userType = premium
Task4Analytics  SideEffect run #3, userType = premium
```

Task 5, scrolled down to item 40 and back up. One event per crossing, not one per item:

```
Task5Scroll  ANALYTICS: scrolled past the first item
```

and the counters on the screen were:

```
derivedStateOf variant: 3 recompositions
direct read variant: 78 recompositions
```
