# Part A. Theory answers

Student: ____________________  Group: ____________  Date: ____________

---

## Question 1

A side-effect is when a composable changes something that lives outside of it. For example it calls the network, writes to a database, writes a log, or changes some global variable. The composable itself should only describe the UI, and nothing else.

The body of a composable is a bad place for this, because we do not control how many times it runs. Recomposition happens every time the state it reads changes. During an animation that can be every frame. On top of that the order is not guaranteed, and Compose can throw a recomposition away and run it again. So `viewModel.loadProfile()` in the body gets called again and again, and `analytics.log("opened")` sends tens of events instead of one. That is what happened in the situation above.

Effect (Key Term): an effect is a special composable that does not draw anything on the screen. It only exists so that a side-effect can run at a safe moment of the composition lifecycle, and so that Compose can cancel it or clean it up later.

Data loading belongs in the ViewModel because of unidirectional data flow: state goes down from the ViewModel to the UI, and events go up from the UI to the ViewModel. The ViewModel is not recreated on every recomposition and it survives screen rotation, so the request is sent once. The composable just reads the state and draws it.

---

## Question 2

| I want to | API |
|---|---|
| call a suspend function while the composable is in the Composition | `LaunchedEffect` |
| launch a coroutine from a callback such as `onClick` | `rememberCoroutineScope` |
| read the latest value inside an effect without restarting it | `rememberUpdatedState` |
| register a listener and remove it afterwards | `DisposableEffect` |
| publish Compose state to code Compose does not manage | `SideEffect` |
| turn Flow, LiveData or RxJava into Compose State | `produceState` |
| recompose less often than the input changes | `derivedStateOf` |
| turn Compose State into a Flow | `snapshotFlow` |

---

## Question 3

(1) The effect starts when the composable enters the Composition for the first time. `LaunchedEffect` runs its block in a coroutine at that moment.

(2) When `pulseRateMs` changes from 3000 to 1000, the key changes. So Compose cancels the coroutine that is running now (the `delay(3000)` is interrupted) and starts the block again from the beginning with the new value. Now the pulse waits 1 second instead of 3.

(3) When the composable leaves the Composition, the coroutine is cancelled. `isActive` is a property of the coroutine scope, it becomes `false` after cancellation, so the `while` loop stops at the next check and the animation does not run forever. Even without `isActive` the loop would stop, because `delay()` throws `CancellationException`, but writing `isActive` makes it obvious.

In the MiniComposition model from the lecture, the effect with key 100 printed that it was cancelled when we left the composition. That is the same thing: leaving the Composition cancels everything that `LaunchedEffect` started.

---

## Question 4

`LaunchedEffect(true)` is suspicious because `true` never changes. So the effect starts once and never restarts, no matter what happens around it. Here `userId` can change, but the effect does not care, it already caught the first `userId` and will keep sending hello for the wrong user. Like `while(true)`, it is not always a bug, but when you see it you should stop and check if it is really correct.

A constant key is fine when the work must really happen only once while the screen is alive and it does not depend on any value. The splash screen from the lecture is like that: wait 2 seconds and go to the next screen. There is no parameter there that could change the waiting.

Honestly, this is the part I keep mixing up. My rule now is to look at what the block reads, and then check that all of it is in the keys.

Fixed code:

```kotlin
LaunchedEffect(userId) {
    api.sendHello(userId)
}
```

Now when `userId` changes, the old coroutine is cancelled and hello is sent for the new user.

---

## Question 5

`LaunchedEffect` is itself a `@Composable` function. Composable functions can only be called from the composition, that is from the body of another composable. `onClick` is a normal lambda, it runs later, when the user taps, and at that moment there is no composition going on. The compiler says:

```
@Composable invocations can only happen from the context of a @Composable function
```

`rememberCoroutineScope()` returns a `CoroutineScope` which is remembered in the place of the composition where it was called. Its lifetime is tied to that place: when the composable leaves the Composition, the scope is cancelled, and all coroutines started from it are cancelled too. So we cannot leak work after the screen is gone.

Correct version:

```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch {
        snackbarHostState.showSnackbar("Hi")
    }
}) {
    Text("Show")
}
```

`showSnackbar` is a suspend function because it does not return immediately. It waits until the snackbar is dismissed or the time runs out, and it can even tell us if the action button was pressed. That waiting must happen in a coroutine, and because it is suspend it can also be cancelled when we leave the screen.

The rule I use: the work starts because the composable appeared -> `LaunchedEffect`. The work starts because the user did something -> `rememberCoroutineScope`.

---

## Question 6

The effect might call a stale lambda because `LaunchedEffect(true)` starts only once and the coroutine captures the `onTimeout` that existed at that moment. Later the parent recomposes and passes a new lambda, but the coroutine is still holding the old one, so after 2 seconds the old lambda is called.

`rememberUpdatedState(onTimeout)` makes a `State` that is rewritten on every recomposition with the newest value. The coroutine does not capture the lambda itself, it captures the state holder and reads `currentOnTimeout` only at the moment it calls it. So it always gets the latest version, and the effect itself is not restarted.

Making `onTimeout` the effect key is wrong because the parent creates a new lambda instance on every recomposition, so the key is always "different". The effect would be cancelled and started again and again, the `delay(2000)` would never finish, and `onTimeout` would never be called at all.

The model output `captured -> onTimeout v1` and `updatedState -> onTimeout v3` proves it: the captured variable froze on the first version, and the one from `rememberUpdatedState` followed the changes up to the last version.

---

## Question 7

Cleanup is needed because the observer is registered in an object that lives longer than our composable. The lifecycle belongs to the Activity, and our screen can disappear much earlier. If we do not remove it, the observer just stays in the lifecycle list and holds a reference to our lambda, which holds everything around it. That is the leak. The second problem is that opening the screen again adds one more observer, so the same event is handled twice, then three times.

The `onDispose` rule: `DisposableEffect` must end with an `onDispose { ... }` block, and it must be the last statement of the effect. If it is not there, the code does not even compile, because the block has to return a `DisposableEffectResult`.

An empty `onDispose` is bad practice, because it means there is nothing to clean up, so `DisposableEffect` was the wrong choice. In that case `LaunchedEffect` (if the work is suspend) or `SideEffect` (if we only need to push a value out after recomposition) fits better.

Fixed code:

```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> /* ... */ }
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

---

## Question 8

Console output (4 lines):

```
A.addObserver(observer)
A.removeObserver(observer)
B.addObserver(observer)
B.removeObserver(observer)
```

The second `screen("A")` prints nothing because the key is the same as before. `DisposableEffect` compares the new key with the old one, they are equal, so it does not dispose anything and does not run the block again. The effect that is already working just continues.

When `B` arrives, the old observer is removed first and only after that the new one is added. The order is: `onDispose` of the old effect, then the block of the new effect. It has to be like this, otherwise for a moment there would be two observers at the same time.

The last line comes from `comp.leave()`: leaving the composition disposes the effect that is active, and at that point it is `B`.

I ran the model and got exactly these four lines in this order.

---

## Question 9

`SideEffect` runs after every successful recomposition, that is after the composition is committed. So it does not run for a composition attempt that Compose threw away.

A direct call in the body is dangerous because the body runs during the attempt, before Compose has decided anything. If that attempt is discarded, the UI will never show those values, but the analytics library already got them. So the library has data from a composition that never existed on the screen.

The attempt model from the lecture:

1. Direct call, `commits = false`: analytics is called anyway, during the attempt. Wrong, because the composition was thrown away.
2. `SideEffect`, `commits = false`: analytics is not called. Compose saw that the composition did not commit, so it did not run the effect. This is what we want.
3. `SideEffect`, `commits = true`: analytics is called once, after the commit, with the value that is really on the screen.

`SideEffect` does not take keys. It just runs after every successful recomposition. That looks wasteful at first, but it is what keeps the outside object equal to what is on the screen.

---

## Question 10

`produceState` starts a coroutine and gives us back a `State` that the coroutine can write into. Inside the producer we can call suspend functions and do `value = ...` as many times as we need.

The producer starts when the composable enters the Composition, and it is cancelled when the composable leaves the Composition or when the key changes (on a key change it is cancelled and started again with the new key).

The returned `State` conflates: if we set the same value that is already there, no recomposition happens. That is because `mutableStateOf` uses structural equality by default, so writing an equal value is simply ignored. In the lecture model we wrote `Loading` twice in a row and the recomposition counter went up only once.

`loadNetworkImage` starts with a lowercase letter because of the naming rule: a composable that emits UI is written in PascalCase like a class, but a composable that returns a value is named like a normal function, in camelCase. `rememberCoroutineScope` and `produceState` themselves follow the same rule.

---

## Question 11

```kotlin
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
```

It behaves like `LaunchedEffect` because inside it is literally a `LaunchedEffect`. No magic. A remembered `MutableState` and a coroutine tied to the composition, that is all.

When the key changes, `LaunchedEffect` cancels the running producer and starts it again with the new key. The state itself is not reset, because `remember` has no key, so until the new producer writes something the UI keeps showing the last value.

---

## Question 12

When we read `listState.firstVisibleItemIndex` directly in the composable, the composable subscribes to that state. The index changes all the time while scrolling, so the whole composable is recomposed on every change, even though the button only needs to know "is the index bigger than 0 or not".

`derivedStateOf` solves it because it calculates the boolean inside, and notifies the readers only when the result of the calculation changed. The index may change ten times, but if the answer stays `true`, nobody is recomposed.

It is wrapped in `remember` so that the derived state object survives recompositions. Without `remember` a new object would be created on every recomposition and it would not remember the previous result, so there would be no benefit at all.

Count for `[0, 0, 1, 2, 3, 4, 5, 3, 1, 0]` (I counted it twice, the first time I forgot the very first composition and got 8):

Direct read. The first composition is 1. After that I count how many times the value really changed: 0 -> 0 no change, 0 -> 1, 1 -> 2, 2 -> 3, 3 -> 4, 4 -> 5, 5 -> 3, 3 -> 1, 1 -> 0. That is 8 changes. 1 + 8 = **9 recompositions**.

With `derivedStateOf`. The values become `index > 0`: false, false, true, true, true, true, true, true, true, false. The first composition is 1 (false). Then false -> true is one change, and true -> false at the end is one more. 1 + 2 = **3 recompositions**.

---

## Question 13

Here `derivedStateOf` is pure overhead, because `fullName` changes exactly as often as `firstName` and `lastName` change. Every letter the user types changes the input and also changes the result, so nothing is filtered out. We only pay for the extra object and the extra snapshot tracking.

Correct version:

```kotlin
var firstName by remember { mutableStateOf("") }
var lastName by remember { mutableStateOf("") }

val fullName = "$firstName $lastName"
```

Just calculate it. It is a cheap string concatenation and it is recalculated only when the composable recomposes anyway.

Criterion: use `derivedStateOf` only when the input changes more often than the result. If the input and the result change together, do not use it.

The Caution in my own words: `derivedStateOf` is not free. People treat it as an optimization for everything. If the result changes as often as the input, it only makes the code slower and harder to read. I did the same mistake in my own project before this lecture.

---

## Question 14

I would use `derivedStateOf` when the amplitudes come from a fast changing state (for example the microphone writes a new amplitude every few milliseconds) and the UI needs something from it that changes much slower. For example "is the voice loud enough to show the red indicator", or a short downsampled list of bars. Then hundreds of input changes give only a few recompositions.

I would not use it in these three cases:

1. The amplitudes are precomputed. Then they are a normal list, not state at all, and there is nothing to track or filter.
2. The waveform is static. Nothing changes, so there is no recomposition to avoid in the first place.
3. The waveform is updated manually with `LaunchedEffect` or `animate*`. Here we already decide ourselves how often a new value arrives, and we want to draw every one of them. `derivedStateOf` would let all of them through anyway, so we pay for it and get nothing back.

---

## Question 15

`snapshotFlow` takes a block that reads Compose state and turns it into a `Flow`. It runs the block, remembers which snapshot states were read inside, and emits a new value when one of them changes. It also compares with the previous value, so if the result is the same it does not emit.

It is called a cold Flow because the block does not run until somebody collects it. Creating it does nothing at all. Every collector starts its own run.

`map { index -> index > 0 }` turns the index into a boolean, are we past the first item or not. `distinctUntilChanged()` drops the repeats, so only the moment when the boolean flips goes through. And `filter { it == true }` throws away the flip back to false, because we do not want an event when the user scrolls back to the top.

`collect` is inside `LaunchedEffect` because `collect` is a suspend function and it never finishes by itself. It has to live in a coroutine that is cancelled when the composable leaves the Composition, otherwise the collection would keep running after the screen is gone.

Stream `0, 0, 1, 2, 5, 3, 0, 0, 1, 4`, step by step:

1. `snapshotFlow` itself drops equal values that come one after another: `0, 1, 2, 5, 3, 0, 1, 4`
2. `map { it > 0 }`: `false, true, true, true, true, false, true, true`
3. `distinctUntilChanged()`: `false, true, false, true`
4. `filter { it == true }`: `true, true`

So **2 events** are sent.

---

## Question 16

`tick` goes 1, 2, 3, 4. `owner` is "A" on ticks 1 and 2, and becomes "B" on tick 3.

| Keys | Restarts (prediction) | Why |
|---|---|---|
| `keys = []` | 1 | There is no key, so the effect starts only once on tick 1 and never restarts. It keeps working with owner "A" even after the owner became "B". |
| `keys = [owner]` | 2 | Starts on tick 1 with "A", and restarts on tick 3 when the owner becomes "B". Exactly when it is needed. |
| `keys = [owner, tick]` | 4 | `tick` changes every time, so the effect is cancelled and started on every tick, even when nothing important changed. |

The wrong one is `keys = []`, because it works with a stale value (owner "A" that does not exist any more). The right one is `keys = [owner]`, because the effect only uses `owner` and restarts only when `owner` changes. The inefficient one is `keys = [owner, tick]`: the result is correct, but it does 4 restarts instead of 2 and throws away work for nothing.

I ran the lecture model and the numbers came out 1, 2 and 4, like I predicted.

---

## Question 17

`lifecycleOwner` must be a key. The effect registers the observer on this exact object, so if the object changes, the registration has to be moved to the new one.

`currentOnStart` and `currentOnStop` must not be keys. The parent creates new lambdas on almost every recomposition, so if they were keys the observer would be removed and added again all the time, for no reason. And we do not need a restart at all: `rememberUpdatedState` already makes sure that when the observer fires, it reads the newest lambda.

If `lifecycleOwner` is not a key and it changes, the observer stays registered on the old owner. The old owner keeps a reference to us (leak), we keep receiving events from a lifecycle that is not ours any more, and the new owner sends us nothing. So we get a leak and lost events at the same time.

Key Point rule: put in the keys every value that the effect depends on and whose change must restart the effect. If a value must only be read as "the latest one" and must not restart anything, keep it out of the keys and wrap it in `rememberUpdatedState`.

---

## Question 18

Bug 1. `viewModel.markOpened(userId)` is a side-effect written straight into the composable body. It runs on every recomposition, so the server gets many "opened" events, and it can even run in a composition that is discarded.

Bug 2. `LaunchedEffect(true) { delay(5_000); onClose() }` has a constant key and captures `onClose`. After 5 seconds it calls the lambda from the very first composition, not the current one.

Bug 3. `DisposableEffect(owner)` has no `onDispose`, so it does not compile, and logically the observer is never removed, which leaks it.

Bug 4. `LaunchedEffect(Unit) { send() }` is inside `onClick`. A composable cannot be called from a normal callback. This one is the easiest to find, because it does not compile.

Corrected version:

```kotlin
@Composable
fun ChatScreen(userId: String, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val observer = remember { LifecycleEventObserver { _, _ -> } }
    val currentOnClose by rememberUpdatedState(onClose)

    // (1) the side-effect is in an effect now, and it restarts only when the user changes
    LaunchedEffect(userId) {
        viewModel.markOpened(userId)
    }

    // (2) the 5 seconds must not restart, so the key stays constant,
    //     but the lambda is read through rememberUpdatedState
    LaunchedEffect(Unit) {
        delay(5_000)
        currentOnClose()
    }

    // (3) now the observer is removed
    DisposableEffect(owner) {
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
        }
    }

    // (4) a coroutine from a callback is started with the remembered scope
    Button(onClick = {
        scope.launch { send() }
    }) {
        Text("Send")
    }
}
```

---

## Question 19

| API | Runs | Cleanup | Restarts on |
|---|---|---|---|
| `LaunchedEffect` | when the composable enters the Composition, the block runs in a coroutine | the coroutine is cancelled when it leaves | when a key changes |
| `rememberCoroutineScope` | nothing runs by itself, it only gives a scope, and we launch when we want (usually from a callback) | the scope is cancelled when the composable leaves | never, it is remembered |
| `rememberUpdatedState` | on every recomposition it writes the newest value into the state | no cleanup | it does not restart anything, it only refreshes the value |
| `DisposableEffect` | when the composable enters the Composition | `onDispose`, and it is required | when a key changes, and on leaving |
| `SideEffect` | after every successful recomposition (after the commit) | no cleanup | it has no keys, it runs after each commit |
| `produceState` | when it enters the Composition, the producer runs as a coroutine | the producer is cancelled | when a key changes, and on leaving |
| `derivedStateOf` | lazily, when the derived value is read and some input has changed | no cleanup | when an input state changes, but readers are notified only if the result changed |
| `snapshotFlow` | when somebody collects it, inside a coroutine | the collection stops together with the coroutine | when a state read inside the block changes |

Examples from my own projects:

* `LaunchedEffect` - loading the list of films when the screen opens, the category id is the key.
* `rememberCoroutineScope` - the "Add to favourites" button.
* `rememberUpdatedState` - a timer screen that calls `onFinish()` at the end. The parent keeps making new lambdas and the countdown must not start over.
* `DisposableEffect` - a `BroadcastReceiver` for internet connection changes, registered and unregistered.
* `SideEffect` - telling Crashlytics which screen the user is on, so the crash report is useful.
* `produceState` - loading an avatar from the network with Loading, Success and Error.
* `derivedStateOf` - the "back to top" button in a long chat.
* `snapshotFlow` - one analytics event when the user scrolls past the first item of the feed.
