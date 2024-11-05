/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.experiments.base.experiment

import org.opendc.compute.simulator.telemetry.parquet.ComputeExportConfig
import org.opendc.experiments.base.experiment.specs.AllocationPolicySpec
import org.opendc.experiments.base.experiment.specs.CheckpointModelSpec
import org.opendc.experiments.base.experiment.specs.ExportModelSpec
import org.opendc.experiments.base.experiment.specs.FailureModelSpec
import org.opendc.experiments.base.experiment.specs.ScenarioTopologySpec
import org.opendc.experiments.base.experiment.specs.WorkloadSpec

/**
 * A data class representing a scenario for a set of experiments.
 *
 * @property topology The list of HostSpec representing the topology of the scenario.
 * @property workload The WorkloadSpec representing the workload of the scenario.
 * @property allocationPolicy The AllocationPolicySpec representing the allocation policy of the scenario.
 * @property failureModel The FailureModel representing the failure model of the scenario. It can be null.
 * @property exportModel The ExportSpec representing the export model of the scenario. It defaults to an instance of ExportSpec.
 * @property outputFolder The String representing the output folder of the scenario. It defaults to "output".
 * @property name The String representing the name of the scenario. It defaults to an empty string.
 * @property runs The Int representing the number of runs of the scenario. It defaults to 1.
 * @property initialSeed The Int representing the initial seed of the scenario. It defaults to 0.
 * @property computeExportConfig configures which parquet columns are to be included in the output files.
 */
public data class Scenario(
    var id: Int = -1,
    val name: String = "",
    val outputFolder: String = "output",
    val runs: Int = 1,
    val initialSeed: Int = 0,
    val computeExportConfig: ComputeExportConfig,
    val topologySpec: ScenarioTopologySpec,
    val workloadSpec: WorkloadSpec,
    val allocationPolicySpec: AllocationPolicySpec,
    val exportModelSpec: ExportModelSpec = ExportModelSpec(),
    val failureModelSpec: FailureModelSpec?,
    val checkpointModelSpec: CheckpointModelSpec?,
    val maxNumFailures: Int = 10,
)
