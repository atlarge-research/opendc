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

package org.opendc.simulator.compute.workload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.opendc.simulator.compute.machine.SimMachine;
import org.opendc.simulator.engine.graph.FlowSupplier;

public record ChainWorkload(
        ArrayList<Workload> workloads,
        long checkpointInterval,
        long checkpointDuration,
        double checkpointIntervalScaling)
        implements Workload {

    public void removeWorkloads(int numberOfWorkloads) {
        if (numberOfWorkloads <= 0) {
            return;
        }
        this.workloads.subList(0, numberOfWorkloads).clear();
    }

    @Override
    public SimWorkload startWorkload(FlowSupplier supplier) {
        return new VirtualMachine(supplier, this);
    }

    @Override
    public SimWorkload startWorkload(List<FlowSupplier> supplier, SimMachine machine, Consumer<Exception> completion) {
        return new VirtualMachine(supplier, this, machine, completion);
    }
}
