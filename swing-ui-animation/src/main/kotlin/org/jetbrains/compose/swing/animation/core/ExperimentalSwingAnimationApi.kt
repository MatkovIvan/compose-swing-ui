/*
 * Copyright 2024 The compose-swing-ui authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Vendored Compose animation engine for the Compose-over-Swing runtime.
 *
 * This package mirrors androidx `compose-animation:animation-core`. Its public surface tracks the
 * upstream library and may change between releases to stay aligned with it, so every declaration is
 * gated behind [ExperimentalSwingAnimationApi] and requires an explicit opt-in.
 */
package org.jetbrains.compose.swing.animation.core

/**
 * Marks the entire `swing-ui-animation` public surface as experimental.
 *
 * This module mirrors androidx `compose-animation:animation-core`: its types, easing curves,
 * spring/tween/keyframes specs, [Animatable], [Transition] and the rest of the engine track the
 * upstream library and may change to stay aligned with it. Signatures, defaults and behavior can
 * therefore shift between releases without going through the usual stable-API deprecation cycle.
 *
 * Consumers must opt in explicitly before using any declaration from this package, either by
 * annotating the usage site with `@OptIn(ExperimentalSwingAnimationApi::class)` or by enabling the
 * opt-in for the whole module via the `-opt-in` compiler argument.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message =
        "swing-ui-animation mirrors androidx animation-core and is experimental: it may change " +
            "to stay aligned with upstream. Opt in with @OptIn(ExperimentalSwingAnimationApi::class).",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalSwingAnimationApi
