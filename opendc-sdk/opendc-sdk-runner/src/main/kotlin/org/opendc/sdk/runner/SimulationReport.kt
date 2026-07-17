/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner

import org.opendc.sdk.model.experiment.ScenarioSpec
import org.opendc.sdk.runner.sink.CollectedMetrics
import org.opendc.sdk.runner.sink.ParquetOutput
import org.opendc.sdk.runner.sink.SinkResult
import java.nio.file.Path

/**
 * The outcome of a [OpenDC.simulate] call: one [ScenarioResult] per scenario that was run.
 */
public data class SimulationReport(public val scenarios: List<ScenarioResult>) {
    /** Every run across every scenario, flattened. */
    public val runs: List<RunResult> get() = scenarios.flatMap { it.runs }
}

/**
 * The outcome of a single scenario across all of its repetitions.
 *
 * @property scenario The scenario that was simulated.
 * @property runs One [RunResult] per repetition, ordered by seed.
 */
public data class ScenarioResult(
    public val scenario: ScenarioSpec,
    public val runs: List<RunResult>,
)

/**
 * The outcome of a single repetition of a scenario.
 *
 * @property seed The random seed of this repetition.
 * @property results The raw results produced by each configured output sink.
 */
public data class RunResult(
    public val seed: Long,
    public val results: List<SinkResult>,
) {
    /** The directory the parquet sink wrote to, if a [org.opendc.sdk.runner.sink.ParquetSink] was configured. */
    public val outputPath: Path? get() = results.filterIsInstance<ParquetOutput>().firstOrNull()?.path

    /** The metrics captured in memory, if an [org.opendc.sdk.runner.sink.InMemorySink] was configured. */
    public val metrics: CollectedMetrics? get() = results.filterIsInstance<CollectedMetrics>().firstOrNull()
}
