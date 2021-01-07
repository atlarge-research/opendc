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

import org.opendc.experiments.sc20.experiment.model.CompositeWorkload
import org.opendc.experiments.sc20.experiment.model.OperationalPhenomena
import org.opendc.experiments.sc20.experiment.model.Topology
import org.opendc.experiments.sc20.experiment.model.Workload
import org.opendc.harness.dsl.anyOf

/**
 * A [Portfolio] that explores the effect of a composite workload.
 */
public class CompositeWorkloadPortfolio : Portfolio("composite-workload") {
    private val totalSampleLoad = 1.3301733005049648E12

    override val topology: Topology by anyOf(
        Topology("base"),
        Topology("exp-vol-hor-hom"),
        Topology("exp-vol-ver-hom"),
        Topology("exp-vel-ver-hom")
    )

    override val workload: Workload by anyOf(
        CompositeWorkload(
            "all-azure",
            listOf(Workload("solvinity-short", 0.0), Workload("azure", 1.0)),
            totalSampleLoad
        ),
        CompositeWorkload(
            "solvinity-25-azure-75",
            listOf(Workload("solvinity-short", 0.25), Workload("azure", 0.75)),
            totalSampleLoad
        ),
        CompositeWorkload(
            "solvinity-50-azure-50",
            listOf(Workload("solvinity-short", 0.5), Workload("azure", 0.5)),
            totalSampleLoad
        ),
        CompositeWorkload(
            "solvinity-75-azure-25",
            listOf(Workload("solvinity-short", 0.75), Workload("azure", 0.25)),
            totalSampleLoad
        ),
        CompositeWorkload(
            "all-solvinity",
            listOf(Workload("solvinity-short", 1.0), Workload("azure", 0.0)),
            totalSampleLoad
        )
    )

    override val operationalPhenomena: OperationalPhenomena by anyOf(
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = false)
    )

    override val allocationPolicy: String by anyOf(
        "active-servers"
    )
}
