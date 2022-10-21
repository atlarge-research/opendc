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

package org.opendc.simulator.network

import io.mockk.spyk
import org.opendc.simulator.flow2.FlowGraph
import org.opendc.simulator.flow2.FlowStage
import org.opendc.simulator.flow2.FlowStageLogic
import org.opendc.simulator.flow2.InPort
import org.opendc.simulator.flow2.Inlet
import org.opendc.simulator.flow2.OutPort
import org.opendc.simulator.flow2.Outlet

/**
 * A [SimNetworkPort] that acts as a test source.
 */
class TestSource(graph: FlowGraph) : SimNetworkPort(), FlowStageLogic {
    val logic = spyk(this)
    private val stage = graph.newStage(logic)

    val outlet: OutPort = stage.getOutlet("out")
    val inlet: InPort = stage.getInlet("in")

    init {
        outlet.push(80.0f)
    }

    override fun onUpdate(ctx: FlowStage, now: Long): Long = Long.MAX_VALUE

    override fun getOutlet(): Outlet = outlet

    override fun getInlet(): Inlet = inlet
}
