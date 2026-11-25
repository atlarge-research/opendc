/*
 * Copyright (c) 2026 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.web.launcher

import kotlinx.serialization.Serializable
import org.opendc.sdk.model.scenario.ScenarioSpec

/** The one environment variable a launcher reads: where its manifest is. */
const val MANIFEST_URL_VARIABLE = "MANIFEST_URL"

/** The entry point of a launcher started from its files. */
const val LAUNCHER_MAIN = "org.opendc.web.launcher.MainKt"

/**
 * Everything one launcher process needs, written by the server and read at startup.
 *
 * The server has already rewritten every reference in [units] to a path relative to the working
 * directory; [inputs] only says what to stage at those paths and is never consulted while resolving.
 */
@Serializable
data class LaunchManifest(
    val inputs: List<StagedInput>,
    val units: List<LaunchUnit>,
    val parallelism: Int,
    // Defaulted so a manifest written by hand needs nothing to listen to it.
    val telemetry: TelemetryTarget = TelemetryTarget.None,
)

/**
 * Bytes to put at [path], relative to and inside the working directory, before the units start.
 *
 * @property source A `file:` or `http(s):` URL.
 * @property key The stored object, immutable under one key. A dispatcher that stages inputs itself
 *           caches by it, since [source] is signed afresh for every manifest.
 */
@Serializable
data class StagedInput(
    val path: String,
    val source: String,
    val key: String,
)

/**
 * One `(scenario, seed)` run: [scenario] carries its own `seed` and only staged paths.
 *
 * @property outcome Where the unit's [UnitOutcome] is published, once its outputs are.
 */
@Serializable
data class LaunchUnit(
    val scenario: ScenarioSpec,
    val outputs: List<OutputTarget>,
    val outcome: String,
)

/** Where the output file named [file] is published. */
@Serializable
data class OutputTarget(
    val file: String,
    val target: String,
)
