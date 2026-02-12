# Compose Swing UI

A Compose wrapper around Swing components using only **Compose Runtime** (no skiko or Compose Multiplatform dependencies).

This project provides a declarative, reactive way to build Swing UIs using Jetpack Compose's composition model, inspired by Compose HTML, Mosaic, and Compose Multiplatform.

## Features

- **Pure Compose Runtime**: Uses only `androidx.compose.runtime` without skiko or Compose Multiplatform
- **Swing Component Wrappers**: Composable wrappers for all major Swing components
- **Layout Support**: Composable panels with various layout managers (FlowLayout, BorderLayout, BoxLayout, GridLayout, etc.)
- **Menu Bar Support**: `JMenuBar.setContent` for composable menus
- **Window Support**: `JFrame.setContent` for composable window content
- **Reactive State Management**: Full support for Compose state and recomposition

## Architecture

The project consists of two modules:

- **swing-ui**: Core library with Compose wrappers for Swing components
- **sample-app**: Sample application demonstrating usage

### Key Components

- `SwingApplier`: Custom applier for managing Swing component hierarchy
- `SwingDispatcher`: Coroutine dispatcher for Swing EDT
- `GlobalSnapshotManager`: Manages Compose snapshot state updates
- `GlobalRecomposer`: Shared Recomposer instance used by all compositions
- `UpdateEffect`: Utility for reactive updates to Swing components

### Composition Model

All compositions in the application share a single global `Recomposer` instance, which provides:
- Efficient recomposition across all UI components
- Consistent state management throughout the application
- Proper parent-child composition relationships

The framework uses Swing's client properties to track composition hierarchy, allowing nested compositions to find their parent composition context automatically.

## Available Components

### Basic Components
- `Button` - JButton wrapper
- `Label` - JLabel wrapper
- `TextField` - JTextField wrapper
- `TextArea` - JTextArea wrapper
- `CheckBox` - JCheckBox wrapper
- `RadioButton` - JRadioButton wrapper
- `ComboBox` - JComboBox wrapper
- `Slider` - JSlider wrapper
- `ProgressBar` - JProgressBar wrapper
- `Separator` - JSeparator wrapper

### Layout Components
- `Panel` - JPanel with custom layout
- `FlowPanel` - JPanel with FlowLayout
- `BorderPanel` - JPanel with BorderLayout
- `BoxPanel` - JPanel with BoxLayout
- `GridPanel` - JPanel with GridLayout
- `GridBagPanel` - JPanel with GridBagLayout
- `CardPanel` - JPanel with CardLayout

### Menu Components
- `Menu` - JMenu wrapper
- `MenuItem` - JMenuItem wrapper
- `CheckBoxMenuItem` - JCheckBoxMenuItem wrapper
- `RadioButtonMenuItem` - JRadioButtonMenuItem wrapper
- `MenuSeparator` - Menu separator

### Window Components
- `Container.setContent` - Set composable content for any Container (JPanel, JFrame.contentPane, etc.)
- `JFrame.setContent` - Set composable content for a JFrame (delegates to Container.setContent)
- `JMenuBar.setContent` - Set composable content for a JMenuBar

## Usage

### Basic Example

```kotlin
import androidx.compose.runtime.*
import org.jetbrains.compose.swing.components.*
import org.jetbrains.compose.swing.window.setContent
import javax.swing.*

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("My App")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(600, 400)
        
        frame.setContent {
            var counter by remember { mutableStateOf(0) }
            
            FlowPanel {
                Label("Counter: $counter")
                Button("Increment", onClick = { counter++ })
                Button("Decrement", onClick = { counter-- })
            }
        }
        
        frame.isVisible = true
    }
}
```

### Menu Bar Example

```kotlin
val menuBar = JMenuBar()
menuBar.setContent {
    Menu("File") {
        MenuItem("New", onClick = { println("New clicked") })
        MenuItem("Open", onClick = { println("Open clicked") })
        MenuSeparator()
        MenuItem("Exit", onClick = { System.exit(0) })
    }
    Menu("Edit") {
        MenuItem("Cut", onClick = { println("Cut clicked") })
        MenuItem("Copy", onClick = { println("Copy clicked") })
        MenuItem("Paste", onClick = { println("Paste clicked") })
    }
}
frame.jMenuBar = menuBar
```

### Layout Example

```kotlin
frame.setContent {
    BorderPanel {
        // Top panel
        FlowPanel {
            Label("Header")
        }
        
        // Center panel with vertical layout
        BoxPanel(axis = BoxLayout.Y_AXIS) {
            Button("Button 1")
            Button("Button 2")
            Button("Button 3")
        }
        
        // Bottom panel
        FlowPanel {
            Label("Status: Ready")
        }
    }
}
```

### State Management

```kotlin
frame.setContent {
    var text by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(50) }
    
    BoxPanel(axis = BoxLayout.Y_AXIS) {
        TextField(
            value = text,
            onValueChange = { text = it }
        )
        
        CheckBox(
            text = "Enable feature",
            checked = checked,
            onCheckedChange = { checked = it }
        )
        
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            min = 0,
            max = 100
        )
        
        Label("Text: $text, Checked: $checked, Slider: $sliderValue")
    }
}
```

### Using Container.setContent

You can use `setContent` on any `Container`, not just `JFrame`. This is useful for creating nested compositions:

```kotlin
import org.jetbrains.compose.swing.setContent

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Container.setContent Example")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(600, 400)
        
        // Use setContent on the content pane directly
        frame.contentPane.setContent {
            var counter by remember { mutableStateOf(0) }
            
            BorderPanel {
                FlowPanel {
                    Label("Counter: $counter")
                    Button("Increment", onClick = { counter++ })
                }
            }
        }
        
        frame.isVisible = true
    }
}
```

All compositions share a global `Recomposer` and automatically find their parent composition via Swing's client properties, ensuring proper composition hierarchy and efficient recomposition.

## Building and Running

This project uses [Gradle](https://gradle.org/).

### Build the project
```bash
./gradlew build
```

### Run the sample application
```bash
./gradlew :sample-app:run
```

### Run tests
```bash
./gradlew check
```

### Clean build outputs
```bash
./gradlew clean
```

## Project Structure

```
compose-swing-ui/
├── swing-ui/                    # Core library
│   └── src/main/kotlin/
│       └── org/jetbrains/compose/swing/
│           ├── SwingApplier.kt           # Component tree applier
│           ├── SwingDispatcher.kt        # EDT coroutine dispatcher
│           ├── GlobalSnapshotManager.kt  # State snapshot manager
│           ├── components/
│           │   ├── Components.kt         # Basic component wrappers
│           │   ├── Layouts.kt            # Layout panel wrappers
│           │   └── MenuBar.kt            # Menu component wrappers
│           ├── util/
│           │   └── UpdateEffect.kt       # Reactive update utility
│           └── window/
│               ├── Application.kt        # Application lifecycle
│               ├── Window.kt             # Window composable
│               └── LayoutConfiguration.kt
└── sample-app/                  # Sample application
    └── src/main/kotlin/
        └── App.kt               # Comprehensive usage example
```

## References

This project is inspired by:
- [Compose HTML](https://github.com/JetBrains/compose-multiplatform) - Compose for web
- [Mosaic](https://github.com/JakeWharton/mosaic) - Compose for terminal UIs
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) - Window and setContent patterns

## License

This project is provided as-is for educational and development purposes.