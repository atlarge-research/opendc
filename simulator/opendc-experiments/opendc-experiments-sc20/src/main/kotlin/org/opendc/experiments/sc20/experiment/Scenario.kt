/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.experiments.sc20.experiment

import org.opendc.experiments.sc20.experiment.model.OperationalPhenomena
import org.opendc.experiments.sc20.experiment.model.Topology
import org.opendc.experiments.sc20.experiment.model.Workload
import org.opendc.experiments.sc20.runner.ContainerExperimentDescriptor
import org.opendc.experiments.sc20.runner.ExperimentDescriptor

/**
 * A scenario represents a single point in the design space (a unique combination of parameters).
 */
public class Scenario(
    override val parent: Portfolio,
    val id: Int,
    val repetitions: Int,
    val topology: Topology,
    val workload: Workload,
    val allocationPolicy: String,
    val operationalPhenomena: OperationalPhenomena
) : ContainerExperimentDescriptor() {
    override val children: Sequence<ExperimentDescriptor> = sequence {
        repeat(repetitions) { i -> yield(Run(this@Scenario, i, i)) }
    }
}
