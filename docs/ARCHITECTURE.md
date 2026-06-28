# Architecture

Compose Swing UI is a Compose-runtime binding over Swing. Layout, measurement, and painting stay
with Swing: your composition produces real `java.awt.Component`s, and Swing's own `LayoutManager`s
and look-and-feel size and paint them. What the library adds is Compose's composition model —
composition and recomposition, snapshot state, effects, and a frame clock — driving a live AWT
component tree on the Event Dispatch Thread (EDT).

This document describes the concepts that shape the binding. The KDoc on the public API is the
reference for individual functions; the source tree is the map of where things live.

---

## Mounting a composition

A composition is mounted onto an existing Swing container with `container.setContent { ... }`.
There are matching entry points for windows and for menu bars, and a high-level `application`
entry point that owns a whole app lifecycle.

Mounting always happens on the EDT, because composition and the AWT mutations it drives must run
on that thread. A mount discovers any composition already hosted by an ancestor container and, if
it finds one, joins it as a nested composition that shares its scope and `CompositionLocal`s;
otherwise it starts a fresh root composition. Each root composition is self-contained: it can be
inspected, driven, and disposed independently of every other, which is also what lets the test
harness drive a composition deterministically.

Mounting returns a handle. Disposing it tears the composition down and, for a root, releases the
recomposition and timing resources that root owned.

---

## Driving recomposition

A root composition recomposes on the EDT, so recomposition and the AWT changes that follow it
share the one thread Swing allows for component work. Inside effects and frame callbacks you can
therefore read and write both Compose state and Swing components directly, without hopping threads.

When snapshot state is written — from an effect, a coroutine, a background thread, or a Swing
listener callback — the change is observed at the level of the composition that owns the affected
nodes, and the scopes that read the changed state are scheduled to recompose. Reading and writing
snapshot state (`mutableStateOf`, `derivedStateOf`, `snapshotFlow`, `produceState`, and the rest)
behaves as it does on any Compose target.

---

## Pacing with a frame clock

Each top-level composition is paced by its own frame clock running at a display-like cadence on the
EDT. `withFrameNanos`, the animation APIs built on it, and recomposition are all driven by this
clock. The clock advances only while something is waiting for a frame, so an idle window does no
per-frame work while an animating one advances at a steady rate. Time-based work in separate
windows is independent.

---

## Applying changes to the AWT tree

As a composition changes, the runtime emits structural operations — insert, remove, move, clear —
that are applied to the backing container. Child order in the AWT tree is kept aligned with
composition order so index-based operations always address the intended component.

Swing does not lay out or repaint added, removed, or moved children on its own: a mutated container
needs an explicit layout-and-repaint pass to make the change visible. Every container touched
during a change pass therefore gets one such pass once the pass completes, so a change that touches
a container many times still costs a single layout. The menu tree follows the same model through
its own applier.

---

## Placing children with explicit constraints

Some layouts need to know *where* a child belongs — a `BorderLayout` needs a region (`NORTH`,
`CENTER`, …), and other constraint-based layouts need their own constraint objects. Composition
order does not express that intent: conditionals, movable content, and reordering all change a
child's index without changing where the author meant to place it. Placement is therefore explicit
and parent-driven rather than inferred from index.

A `CompositionLocal` carries the intended constraint from a slot-based parent down to its child;
the default is "add by position." `BorderPanel` is the canonical slot-based parent. Its regions are
declared through a receiver DSL, each region a single-child slot that provides its constraint to the
child it composes. Declaring a region adds its child in the right place, redeclaring it replaces the
child, and dropping a region removes the child. The same mechanism extends to other
constraint-based layouts, and to hosts whose children are installed through dedicated setters (such
as a scroll pane's viewport, headers, and corners) rather than a generic add.

A custom container that consumes an incoming constraint for its own placement starts its children
from the default baseline, so a nested constraint-based layout is free to provide its own
constraints to its own children.

---

## The node lifecycle and listeners

Each node in a composition wraps a Swing component and carries the per-node state the runtime needs
to place, update, and tear it down. A node is recyclable: when content is conditionally shown and
hidden, or replaced by structurally identical content in the same slot, the runtime can reuse the
existing backing component from a clean baseline instead of allocating a new one.

Reuse is why listener lifecycle matters. A listener that calls back into composition state must be
attached for exactly the node's current lifetime: it is detached when the node is released, reused,
or deactivated, so it never fires into a composition that has moved on. Listeners the host
application attaches directly to a component are never touched. A node installs a single stable
listener that always sees current composition state, rather than re-attaching one on every
recomposition.

Listeners are exposed through the modifier system. A typed lambda for a specific event and a raw
listener share one installation path, so the behavior is the same regardless of which form a caller
uses.

---

## Styling and configuration with modifiers

`SwingModifier` is the Compose-shaped way to configure a component: appearance, layout hints,
keyboard and interaction wiring, data transfer, accessibility, and listeners are expressed as
modifier elements chained onto a component. A passed `modifier` is applied first in the chain, so a
component's own defaults remain overridable.

Closed sets of Swing integer (and a few string) constants — scrollbar policies, orientations,
selection modes, and the like — are exposed as typed constant sets. A parameter that takes one of
these accepts exactly the values the wrapped Swing API expects, so an unintended value is flagged
in the IDE while the value passed at runtime is the plain Swing constant, with no translation layer.

---

## Effects and snapshot state

Because this is a Compose-runtime binding, the effect and state APIs behave to their usual Compose
contracts, with a few target-specific guarantees worth relying on:

- **Effects run on the EDT.** `LaunchedEffect`, `DisposableEffect`, and `SideEffect` execute on the
  Event Dispatch Thread, so inside them you can touch Swing components and Compose state directly. A
  `DisposableEffect`'s `onDispose` runs when its node leaves the composition or before the effect
  re-runs for changed keys, giving a precise place to set up and tear down resources tied to a piece
  of UI.

- **Snapshot state works out of the box.** State written from a listener callback, a coroutine, or a
  background thread is observed and recomposes the scopes that read it. Prefer the snapshot APIs over
  hand-wiring Swing listeners to state when you want derived or asynchronously produced values to
  stay in sync.

---

## A state change, end to end

Consider a button whose label reflects a counter:

```kotlin
var count by remember { mutableStateOf(0) }
Button(text = "Clicks: $count", onClick = { count++ })
```

1. The user clicks. Swing fires the button's listener on the EDT and the current `onClick` runs
   `count++`.
2. Writing the state is observed and the scope that read `count` is scheduled to recompose.
3. On the next frame the invalidated scope re-executes and recomputes the label; the stable listener
   is left in place.
4. The change is applied to the live button, and its container is marked for layout.
5. Once the change pass completes, that container is laid out and repainted once, and the new label
   is on screen.

If a slot had appeared or disappeared instead — a conditional inside `BorderPanel` — the applied
change would be an insert or remove in the correct region, and the layout-and-repaint pass is what
makes the structural change visible.

---

## Why Swing needs an explicit applier

The same Compose runtime drives very different targets, and the differences concentrate in how
changes reach the screen:

| Concern | Swing (this library) | DOM (Compose HTML) | Terminal (Mosaic) |
| --- | --- | --- | --- |
| Backing tree | `java.awt.Container` widgets | live DOM nodes | an in-memory node tree |
| Who lays out | Swing `LayoutManager`s | the browser's reflow engine | the target's own layout pass |
| Making changes visible | an explicit layout-and-repaint pass per touched container | mutating the DOM reflows and repaints automatically | the target re-runs layout and renders a frame |
| Placement | explicit constraints, because layout managers need region/constraint information | CSS and element order | the target's own modifier/layout system |
| Threading | the EDT | the single-threaded JS event loop | the target's render loop |

Swing reflows neither on mutation, the way the DOM does, nor on its own schedule, the way a target
that owns its render loop does. That is the fact the binding is built around: changes to the tree
are paired with an explicit layout-and-repaint pass, and placement is carried explicitly because the
layout managers that do the work need real constraint information.

---

For a step-by-step guide to building your own component on top of `SwingNode`, see
[`CUSTOM-COMPONENTS.md`](CUSTOM-COMPONENTS.md).
