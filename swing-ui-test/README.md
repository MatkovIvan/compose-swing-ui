# swing-ui-test

The test harness for Compose Swing UI. It runs compositions headlessly and deterministically — never
realizing an on-screen window, never sleeping — so component behavior can be asserted in plain
`@Test` methods.

## Usage

Add the module as `testImplementation`, then write tests with `runSwingUiTest`:

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

The harness offers finders (`onNodeWithText`, `onNodeWithTag`, `onNodeOfType<T>()`, …), assertions
(`assertExists`, `assertIsDisplayed`, `assertTextEquals`, …), actions (`performClick`,
`performTextInput`, …), and golden-image screenshot comparison. See
[`../docs/TESTING-COMPONENTS.md`](../docs/TESTING-COMPONENTS.md) for the full guide; the API itself
is documented in KDoc.

## Related

- [`../docs/TESTING-COMPONENTS.md`](../docs/TESTING-COMPONENTS.md) — how to write component tests.
- [`../swing-ui/README.md`](../swing-ui/README.md) — the core library.
