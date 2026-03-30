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

package org.opendc.simulator.compute.memory;

import org.opendc.simulator.compute.models.MemoryUnit;
import org.opendc.simulator.engine.engine.FlowEngine;

/**
 * The [SimMemory] implementation for a machine.
 */
public final class Memory {
    //    private final SimpleFlowSink sink;
    private final MemoryUnit memoryUnit;

    public Memory(FlowEngine engine, MemoryUnit memoryUnit) {

        this.memoryUnit = memoryUnit;
        // TODO: Fix this
        //        this.sink = new SimpleFlowSink(graph, (float) memoryUnit.getSize());
    }

    public double getCapacity() {
        //        return sink.getCapacity();
        return 0.0f;
    }

    public MemoryUnit getMemoryUnit() {
        return memoryUnit;
    }

    @Override
    public String toString() {
        return "SimAbstractMachine.Memory";
    }
}
