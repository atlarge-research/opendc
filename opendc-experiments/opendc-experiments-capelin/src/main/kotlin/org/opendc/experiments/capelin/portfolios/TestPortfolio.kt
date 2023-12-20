/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.experiments.capelin.portfolios

import org.opendc.compute.workload.sampleByLoad
import org.opendc.compute.workload.trace
import org.opendc.experiments.base.portfolio.Portfolio
import org.opendc.experiments.base.portfolio.model.OperationalPhenomena
import org.opendc.experiments.base.portfolio.model.Scenario
import org.opendc.experiments.base.portfolio.model.Topology
import org.opendc.experiments.base.portfolio.model.Workload

/**
 * A [Portfolio] to perform a simple test run.
 */
public class TestPortfolio : Portfolio {
    override val scenarios: Iterable<Scenario> = listOf(
        Scenario(
            Topology("single"),
            Workload("bitbrains-small", trace("trace").sampleByLoad(1.0)),
            OperationalPhenomena(failureFrequency = 0.0, hasInterference = true),
            "active-servers"
        )
    )
}
