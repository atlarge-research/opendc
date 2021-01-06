/*
 * Copyright (c) 2021 AtLarge Research
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
import org.opendc.harness.dsl.anyOf

/**
 * A [Portfolio] that explores the effect of operational phenomena on metrics.
 */
public class OperationalPhenomenaPortfolio : Portfolio("operational_phenomena") {
    override val topology: Topology by anyOf(
        Topology("base")
    )

    override val workload: Workload by anyOf(
        Workload("solvinity", 0.1),
        Workload("solvinity", 0.25),
        Workload("solvinity", 0.5),
        Workload("solvinity", 1.0)
    )

    override val operationalPhenomena: OperationalPhenomena by anyOf(
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true),
        OperationalPhenomena(failureFrequency = 0.0, hasInterference = true),
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = false),
        OperationalPhenomena(failureFrequency = 0.0, hasInterference = false)
    )

    override val allocationPolicy: String by anyOf(
        "mem",
        "mem-inv",
        "core-mem",
        "core-mem-inv",
        "active-servers",
        "active-servers-inv",
        "random"
    )
}
