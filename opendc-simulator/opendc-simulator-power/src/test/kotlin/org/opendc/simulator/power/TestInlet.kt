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

package org.opendc.simulator.power

import org.opendc.simulator.flow2.FlowGraph
import org.opendc.simulator.flow2.FlowStage
import org.opendc.simulator.flow2.FlowStageLogic
import org.opendc.simulator.flow2.Outlet

/**
 * A test inlet.
 */
class TestInlet(graph: FlowGraph) : SimPowerInlet(), FlowStageLogic {
    private val stage = graph.newStage(this)
    val flowOutlet = stage.getOutlet("out")

    init {
        flowOutlet.push(100.0f)
    }

    override fun onUpdate(ctx: FlowStage, now: Long): Long = Long.MAX_VALUE

    override fun getFlowOutlet(): Outlet {
        return flowOutlet
    }
}
