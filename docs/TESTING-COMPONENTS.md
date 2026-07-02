# Testing components with the swing-ui-test harness

The `:swing-ui-test` harness runs a composition off-screen and deterministically, then lets you find
components — and the top-level windows the composition realizes — assert their state, and drive
interactions through them. This guide shows how to write behavioral tests — tests that exercise
state → recomposition → visible change through the public API.

## Setup

Add the harness as a test dependency:

```kotlin
dependencies {
    testImplementation(project(":swing-ui-test"))
}
```

Then write a plain `@Test` method whose body is a `runSwingUiTest { … }` block. Inside the block you
call `setContent { … }` to mount your composable, and the harness, finders, assertions, and actions
are all in scope.

```kotlin
import org.jetbrains.compose.swing.test.runSwingUiTest
import org.jetbrains.compose.swing.components.button.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.Test

class CounterTest {
    @Test
    fun clickingIncrements() = runSwingUiTest {
        var clicks by mutableStateOf(0)
        setContent {
            Button(text = "Clicks: $clicks", onClick = { clicks++ })
        }
        onNodeWithText("Clicks: 0").performClick()
        onNodeWithText("Clicks: 1").assertExists()
    }
}
```

`setContent` waits for the composition to settle before returning, so by the next line the tree
reflects the initial state. After an action that writes Compose state, the harness settles again
before the following assertion runs — no sleeps, no manual pumping.

## Finding components

Single-node finders return a `SwingNodeInteraction`:

- `onNodeWithText(text)` — match a component by its displayed text.
- `onNodeWithTag(tag)` — match by test tag (see *Test tags* below).
- `onNodeWithName(name)` — match by component name.
- `onNodeOfType<T>()` — match the single component of a given Swing type.
- `onRoot()` — the composition root.
- `onNode(matcher)` — match with a `SwingMatcher` (e.g. `hasText`, `hasTestTag`, `hasName`,
  `isEnabled`, combined with `and` / `or`).

Multi-node finders return a `SwingNodeInteractionCollection`:

- `onAllNodesWithText(text)`, `onAllNodesWithTag(tag)`, `onAllNodesOfType<T>()`, `onAllNodes(matcher)`.

Narrow a collection to a subtree with `within(ancestor)`, assert its size with
`assertCountEquals(n)`, and target one match with `[index]`, `onFirst()`, or `onLast()` — each
returns a `SwingNodeInteraction` that re-resolves against the live tree on every use:

```kotlin
onAllNodesWithText("row")[1].assertIsEnabled()
onAllNodesWithTag("item").onLast().assertTextEquals("newest")
```

### Test tags

A test tag is a stable handle that survives label and layout changes — prefer it over matching on
displayed text when the text is dynamic. Attach one with the `testTag` modifier:

```kotlin
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.testTag

TextField(value = name, onValueChange = { name = it }, modifier = SwingModifier.testTag("name-field"))
```

```kotlin
onNodeWithTag("name-field").performTextInput("Ada")
```

## Asserting state

Assertions are available on a `SwingNodeInteraction`; each returns the interaction so they chain:

- `assertExists()` / `assertDoesNotExist()`
- `assertIsDisplayed()`
- `assertTextEquals(text)`
- `assertIsEnabled()` / `assertIsNotEnabled()`
- `assertLayoutConstraint(expected)` — assert the parent-assigned layout constraint.

```kotlin
onNodeWithTag("submit")
    .assertIsDisplayed()
    .assertIsEnabled()
    .assertTextEquals("Submit")
```

When you need to read a property the assertions do not cover, reach the live component with the typed
`fetch<T>()` and assert on it directly:

```kotlin
import javax.swing.JList

val list = onNodeOfType<JList<*>>().fetch<JList<*>>()
assertEquals(2, list.selectedIndex)
```

## Driving interactions

Actions are available on a `SwingNodeInteraction`:

- `performClick()` — click the component.
- `performTextInput(text)` — append text to a text component.
- `performTextReplacement(text)` — replace a text component's contents.

```kotlin
onNodeWithTag("amount").performTextReplacement("42")
onNodeWithText("Save").performClick()
```

## Testing windows and dialogs

Content that composes `Window { }` or `Dialog { }` realizes a real top-level peer, which needs a
display: start such a test with a JUnit assumption,
`Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), …)`, so it reports SKIPPED on headless CI
and runs everywhere else.

Window finders resolve the windows realized by the composition under test — other windows in the
process are never matched:

- `onWindow()` — the single realized window.
- `onWindowWithTitle(title)` — match by window title.
- `onWindow(matcher)` / `onAllWindows(matcher)` — match with a `SwingMatcher` (e.g. `hasTitle`).

A `SwingWindowInteraction` offers `assertExists()` / `assertDoesNotExist()`, `assertIsVisible()` /
`assertIsNotVisible()`, the typed `fetch<T>()` for the realized `JFrame`/`JDialog`, and the node
finders scoped to that window's content pane:

```kotlin
import org.jetbrains.compose.swing.test.onWindowWithTitle
import org.jetbrains.compose.swing.window.Window
import org.junit.jupiter.api.Assumptions.assumeFalse
import java.awt.GraphicsEnvironment

@Test
fun settingsWindowShowsItsContent() = runSwingUiTest {
    assumeFalse(GraphicsEnvironment.isHeadless(), "requires a display")
    setContent {
        Window(onCloseRequest = {}, title = "Settings") {
            Button(text = "Apply", onClick = { })
        }
    }
    val window = onWindowWithTitle("Settings")
    window.assertIsVisible()
    window.onNodeWithText("Apply").assertIsEnabled()
}
```

A dialog show is applied on its own event-dispatch turn; the idle gate drains it, so after a state
change plus `awaitIdle()` the realized dialog already reflects the declared visibility.

## Waiting on external timing

Composition state changes settle automatically, so most tests need no waiting. When a condition
genuinely depends on timing outside the composition (a coroutine driven by wall-clock, an external
callback), use `waitUntil { … }`; use `awaitIdle()` to settle the composition explicitly when you
have written state outside of an action.

## Screenshot comparison

The harness can capture a component (or the whole root) to an image and compare it against a stored
golden by structural similarity.

```kotlin
import org.jetbrains.compose.swing.test.screenshot.assertImageAgainstGolden

onNodeWithTag("chart").assertImageAgainstGolden("chart-default")
```

`assertImageAgainstGolden(goldenIdentifier)` is available on both a `SwingNodeInteraction` (captures
the matched component) and on the test itself (captures the root). A `threshold` parameter controls
how strict the structural-similarity match is. To compare two captured images without a golden file,
capture with `captureToImage()` and use `assertImageMatches(expected)`.

## Related

- [`../swing-ui-test/README.md`](../swing-ui-test/README.md) — the harness module.
- [`CUSTOM-COMPONENTS.md`](CUSTOM-COMPONENTS.md) — building the components you are testing.
