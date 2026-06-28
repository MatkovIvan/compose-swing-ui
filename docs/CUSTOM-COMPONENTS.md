# Defining a custom Swing component in Compose

Compose Swing UI ships wrappers for the common widgets (`Button`, `TextField`, `Slider`, …), but
real applications host many bespoke Swing components. Wrapping your own component is a **first-class,
supported use case** — every built-in wrapper is built exactly the same way, on top of the public
`SwingNode` API. This guide shows how.

## The mental model

A composable component is a function that wraps a single Swing `Component`. You describe the
component once and let Compose keep it in sync with state:

```kotlin
@Composable
@SwingComposable
public fun MyWidget(/* state + callbacks */) {
    SwingNode(
        factory = { /* create the Swing component */ },
        update = { /* reactively push state onto it */ },
        onRelease = { /* optional cleanup */ },
    )
}
```

Every Swing composable carries `@SwingComposable` alongside `@Composable` (see the note below), so
your custom component must too.

- `factory` runs **once**, when the node enters the composition. Build (and do one-time
  configuration of) your Swing component here.
- `update` runs on the first composition and on **every recomposition**. Inside it you declare which
  pieces of state map onto which component properties; the framework only re-applies the ones that
  actually changed.
- `onRelease` runs **once**, when the node leaves the composition for good.

`SwingNode` is `inline` and `reified` on the component type, so inside `update` the component is
available as the strongly-typed `this`.

## The `SwingNode` signatures

There are two overloads. The leaf overload wraps a component that has no composable children:

```kotlin
@Composable
@SwingComposable
public inline fun <reified T : Component> SwingNode(
    noinline factory: () -> T,
    crossinline update: SwingNodeUpdater<T>.() -> Unit = {},
    noinline onRelease: (T.() -> Unit)? = null,
    hostsSubcompositions: Boolean = false,
)
```

The container overload additionally hosts composable `content` as children — use it when your
component is a `java.awt.Container` (e.g. a custom `JPanel`) that should contain further
composables:

```kotlin
@Composable
@SwingComposable
public inline fun <reified T : Component> SwingNode(
    noinline factory: () -> T,
    crossinline update: SwingNodeUpdater<T>.() -> Unit = {},
    noinline onRelease: (T.() -> Unit)? = null,
    hostsSubcompositions: Boolean = false,
    crossinline content: @Composable @SwingComposable () -> Unit,
)
```

`hostsSubcompositions` defaults to `false`; you only set it for a custom container whose internal
children run their own `setContent` — see *Hosting nested compositions* below.

### `@SwingComposable`: keeping Swing and `compose.ui` apart

Every Swing composable in this library is annotated `@SwingComposable`, marking it as Swing-target
content. Calling a foreign-applier composable (e.g. `androidx.compose.material.Text`) inside a Swing
composition — or a Swing composable inside a `compose.ui` composition — is then a compile-time error
with a "Swing Composable vs UI Composable" message, instead of compiling silently and failing at
runtime. Your custom component must carry `@SwingComposable` (and annotate any `content` lambda as
`@Composable @SwingComposable () -> Unit`) to stay inside this guarantee.

## Inside `update`: `set` for properties, `SwingModifier` for styling and listeners

The `update` block runs with a `SwingNodeUpdater<T>` receiver. Its core tool is `set`; for styling
and lifecycle-safe listeners you apply a `SwingModifier` chain.

### `set(value) { … }` — reactive property updates

```kotlin
set(value) { /* this: T */ this.someProperty = it }
```

`set` records `value` and runs the block (with the component as `this` and `value` as `it`) on the
first composition, then again **only when `value` changes** between recompositions. This is the
idiomatic way to push one piece of state onto one Swing property. Call `set` once per property you
want kept in sync.

## Styling with a `modifier: SwingModifier` parameter

Visual and interaction concerns that are common across components — colors, fonts, borders, tooltips,
focus, hover — are expressed as a `SwingModifier` chain rather than ad-hoc `set` calls. Give your
component a `modifier: SwingModifier = SwingModifier` parameter and apply it **last** in `update` via
`applyModifier`, so caller-supplied modifiers compose on top of your own defaults:

```kotlin
@Composable
public fun MyWidget(
    /* state + callbacks */
    modifier: SwingModifier = SwingModifier,
) {
    SwingNode(
        factory = { /* … */ },
        update = {
            set(/* … */) { /* … */ }
            applyModifier(modifier) // last
        },
    )
}
```

Built-in modifier builders are extension functions on `SwingModifier`, grouped by concern:

- **Appearance** — `foreground`, `background`, `font`, `border`, `opaque`, `cursor`.
- **Layout** — `preferredSize`, `minimumSize`, `maximumSize`, `visible`, `enabled`,
  `componentOrientation`.
- **Metadata** — `name`, `toolTip`, `clientProperty`.
- **Interaction** — `focusable`, `onHover`, `onFocus`, plus the typed instance listener builders
  (`mouseListener`, `keyListener`, …; see *Attaching a listener* below).

Chain them: `SwingModifier.foreground(Color.RED).border(lineBorder).onHover { … }`. The framework
diffs the chain across recompositions, applies new/changed elements, and **restores the original
value** of any element that is removed from the chain.

## Writing a custom property element

When you need a styling property the built-ins do not cover, implement the public
`SwingModifier.Element` and `SwingModifier.Node` pair. They split immutable description from mutable
per-component state:

- `Element<T : Component, N : Node<T>>` is the **immutable description** of one chain entry. It carries
  the value to write and the component type it targets; the framework holds it as data and replaces it
  with a fresh instance on each chain change.
- `Node<T : Component>` is the **stateful counterpart**, created once per chain slot and kept across
  recompositions. It holds the captured original value and exposes the live, already-typed `component`.

The element declares its target type two ways that must agree:

- `targetType: Class<T>` names the most general component the property needs —
  `Component::class.java` for a property every `java.awt.Component` has, `JComponent::class.java` for a
  `JComponent`-only property like a tooltip or border, a concrete widget class for a widget-specific
  one;
- the node's `component` arrives **already** typed `T`, so your `Node` body reads `component` directly
  without casting.

A node whose component is not a `T` is rejected at apply with a clear message naming the element and
the required vs. actual type.

The lifecycle, across the `Element`/`Node` pair:

- `Element.create()` builds the `Node` once, when the element first enters the chain;
- `Node.onAttach()` runs once, right after the component is injected — **capture the component's
  existing value here** so it can be restored;
- `Element.update(node)` runs on attach and on every chain change — **write the new value here**, so a
  fresh element instance (a new value on recomposition) reaches the live node without re-creating it;
- `Node.onDetach()` runs once, when the element is dropped from the chain or the node is recycled —
  **restore the captured original here**.

A property element is **keyed and last-wins**: its `key` defaults to the element's class, so two
elements of different types never collide. Leave `additive` at its default `false` for a property
(one value wins); a listener instead sets `additive = true` so two of the same builder both install
(see *Attaching a listener* below). Override `key` only when several instances of the *same* type must
coexist as independent slots (e.g. keyed by a property name).

A tooltip lives on `JComponent`, so the element targets `JComponent` via `targetType`; the node reads
its already-typed `component` without casting:

```kotlin
private class ToolTipNode : SwingModifier.Node<JComponent>() {
    private var original: String? = null
    var text: String? = null

    override fun onAttach() {
        original = component.toolTipText // capture the pre-modifier value
    }

    fun apply() {
        component.toolTipText = text // write the latest value
    }

    override fun onDetach() {
        component.toolTipText = original // restore on removal/reuse
    }
}

private class ToolTipElement(private val text: String?) :
    SwingModifier.Element<JComponent, ToolTipNode> {
    override val targetType: Class<JComponent> get() = JComponent::class.java

    override fun create(): ToolTipNode = ToolTipNode()

    override fun update(node: ToolTipNode) {
        node.text = text
        node.apply()
    }
}

public fun SwingModifier.toolTip(text: String?): SwingModifier =
    then(ToolTipElement(text))
```

The built-in `toolTip` builder is the same property shape — capture in attach, write in update,
restore in detach — reusing a shared property node internally; the code above is the public
`Element`/`Node` path you write for a property the library does not ship.

For a property every component has (no `JComponent`-only access), target `Component` instead —
`Element<Component, …>` with `targetType = Component::class.java` — and the node's `component` arrives
typed as `java.awt.Component`.

## Attaching a listener

### Typed instance builders — attach an existing listener object

To attach an existing Swing/AWT listener **object** as-is, use the typed instance builders. Each takes
the listener instance, expresses the target component type, and owns the lifecycle: the same instance
is added on install and removed on detach/reset/reuse (AWT removes by identity), so the runtime never
touches listeners the host app added and you never write a manual `removeXxxListener`.

```kotlin
val onMove = remember { object : MouseAdapter() { override fun mouseMoved(e: MouseEvent) { … } } }
SwingModifier.name("canvas").mouseMotionListener(onMove)
```

The builders, by listener type: `mouseListener`, `mouseMotionListener`, `mouseWheelListener`,
`keyListener`, `focusListener`, `componentListener`, `hierarchyListener`, `containerListener`
(requires a `Container` target), `propertyChangeListener` (unbound, or bound to a property name),
`actionListener` (requires an `AbstractButton` target), and `documentListener` (requires a
`JTextComponent` target; it observes the component's `Document`).

The same instance-builder contract also covers widget- and model-specific listeners:
`changeListener` (for a component that fires change events, such as `JSlider`, `JSpinner`,
`JTabbedPane`, `JProgressBar`, `AbstractButton`, or `JViewport`), `listSelectionListener` (requires a
`JList` target), `treeSelectionListener` (requires a `JTree` target), and `internalFrameListener`
(requires a `JInternalFrame` target).

Each builder is **additive** (no key): two of the same builder both install and both fire, mirroring
Swing's `addXxxListener`. Pass a **stable** instance — `remember { … }` it. A fresh lambda or object
on each recomposition is a new instance, which detaches the old one and attaches the new (a correct
but wasteful `remove`/`add` round-trip).

### `SwingModifier.listener` — the low-level escape hatch

When no typed builder fits — a listener kind the library does not ship a builder for, or one whose
add/remove pair lives on a *model* (not the component) — drop to `SwingModifier.listener`, the single
listener seam every typed and model builder is built on. It takes the listener `instance` plus the
matching `attach`/`detach` pair:

```kotlin
val listener = remember { SomeListener { /* read state off the event, call the live callback */ } }
SwingModifier.listener<MyType, SomeListener>(
    instance = listener,
    attach = { component, l -> component.addSomeListener(l) },   // component is already typed MyType
    detach = { component, l -> component.removeSomeListener(l) },
)
```

`listener<T, L>` is reified on the target component type `T`, so `attach`/`detach` receive the
component already typed, and a node whose component is not a `T` is rejected at apply with a clear
error. There is no `key` parameter — like the typed builders, it is **additive**.

The same `instance` is added once via `attach` when the element enters the chain and removed via
`detach` when it leaves or the node is released/reused — the runtime never touches listeners the host
app added, and you never write a manual `removeXxxListener`. Supplying a *different* instance
(reference inequality) on a later recomposition detaches the old one and attaches the new, so pass a
**stable** instance — `remember { … }` it. To keep the latest callbacks visible without re-attaching,
wrap the callback in `rememberUpdatedState` and read it from inside the remembered listener.

## Domain callbacks stay component parameters

Per-component semantic callbacks — `onClick` on a button, `onValueChange` on a slider — are **not**
modifiers. They remain ordinary parameters of your composable function; callers just pass
`onClick = { … }`.

## A worked example: wrapping `JSpinner`

Here is a complete, compilable wrapper for `JSpinner`, mirroring how `TextField`/`Slider` are built
— a `value` in, an `onValueChange` out:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.annotations.SwingComposable
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.changeListener
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.event.ChangeListener

@Composable
@SwingComposable
fun Spinner(
    value: Int,
    onValueChange: (Int) -> Unit = {},
    min: Int = 0,
    max: Int = 100,
    step: Int = 1,
    enabled: Boolean = true,
    modifier: SwingModifier = SwingModifier,
) {
    // rememberUpdatedState keeps the latest callback without re-attaching the listener every recomposition.
    val callback = rememberUpdatedState(onValueChange)
    // One stable ChangeListener for the node's lifetime — remember it so the same instance is re-used.
    val listener = remember { ChangeListener { event -> callback.value((event.source as JSpinner).value as Int) } }
    SwingNode(
        factory = { JSpinner(SpinnerNumberModel(value, min, max, step)) },
        update = {
            // Reactive property updates: each block re-runs only when its value changes.
            set(enabled) { this.isEnabled = it }
            set(value) { if (this.value != it) this.value = it }
            // The changeListener builder owns the listener's lifecycle; the caller's modifier last.
            applyModifier(SwingModifier.changeListener(listener) then modifier)
        },
    )
}
```

Notice the `if (this.value != it)` guard in the `value` setter: it prevents a feedback loop where
applying the incoming state would itself fire the change listener. The listener reads the current
`onValueChange` through `rememberUpdatedState`, so the stable instance always sees the latest callback
without re-attaching.

### A container example

For a custom container, use the `content` overload and create a `Container` in the factory; children
emitted by `content` are added by the framework's applier:

```kotlin
import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.annotations.SwingComposable
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.border.TitledBorder

@Composable
@SwingComposable
fun TitledGroup(
    title: String,
    content: @Composable @SwingComposable () -> Unit,
) {
    SwingNode(
        factory = { JPanel(FlowLayout()).apply { border = TitledBorder("") } },
        update = {
            set(title) { (this.border as TitledBorder).title = it }
        },
        content = content,
    )
}
```

## Hosting nested compositions: `hostsSubcompositions`

Both `SwingNode` overloads take an opt-in `hostsSubcompositions: Boolean = false`. Leave it `false`
(the default) unless your custom component, internally, drives its **own** `setContent` against one of
its children — for example a Swing container that manages tabs, popups, or split panes by calling
`setContent` on sub-panels it creates itself.

Set it `true` so those nested `setContent` calls **join the surrounding composition** instead of
spinning up a detached, independent one — sharing its recomposer, scope, and `CompositionLocal`s.

```kotlin
SwingNode(
    factory = { TabbedPanel() }, // a JComponent that runs setContent on its own tab panels
    hostsSubcompositions = true,
)
```

When `hostsSubcompositions = true`, the factory component **must** be a `javax.swing.JComponent`; a
bare `java.awt.Component` host throws `IllegalStateException` at apply. Keep it `false` for every
ordinary leaf or container component.

## `onRelease` for cleanup

If your component holds a resource that must be released — a timer, a native handle, a registration
on a shared bus — release it in `onRelease`. It runs once, when the node leaves the composition for
good (the typed component is `this`):

```kotlin
SwingNode(
    factory = { ExpensiveComponent() },
    update = { /* … */ },
    onRelease = { dispose() }, // this: ExpensiveComponent
)
```

`onRelease` is for your own resources. Listeners installed via `SwingModifier.listener` are detached
automatically — you do not need to remove them in `onRelease`.
