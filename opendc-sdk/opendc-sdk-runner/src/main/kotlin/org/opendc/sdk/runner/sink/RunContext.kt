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

package org.opendc.sdk.runner.sink

import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.runner.internal.ExportSettings

/**
 * Identifies the single run an [OutputSink] session is opened for.
 *
 * @property scenario The fully-resolved scenario being simulated.
 * @property experimentName The name of the owning experiment, used to lay out output on disk.
 * @property scenarioId The scenario's index within its experiment's cartesian expansion.
 * @property seed The random seed of this specific run.
 * @property gpuCount The maximum number of GPUs on any host in the topology.
 */
public class RunContext internal constructor(
    public val scenario: Scenario,
    public val experimentName: String,
    public val scenarioId: Int,
    public val seed: Long,
    public val gpuCount: Int,
    internal val export: ExportSettings,
)
