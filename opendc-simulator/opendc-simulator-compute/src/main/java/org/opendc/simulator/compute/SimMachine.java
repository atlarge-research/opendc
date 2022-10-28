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

package org.opendc.simulator.compute;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opendc.simulator.compute.device.SimPeripheral;
import org.opendc.simulator.compute.model.MachineModel;
import org.opendc.simulator.compute.workload.SimWorkload;

/**
 * A generic machine that is able to execute {@link SimWorkload} objects.
 */
public interface SimMachine {
    /**
     * Return the model of the machine containing its specifications.
     */
    MachineModel getModel();

    /**
     * Return the peripherals attached to the machine.
     */
    List<? extends SimPeripheral> getPeripherals();

    /**
     * Start the specified {@link SimWorkload} on this machine.
     *
     * @param workload The workload to start on the machine.
     * @param meta The metadata to pass to the workload.
     * @param completion A block that is invoked when the workload completes carrying an exception if thrown by the workload.
     * @return A {@link SimMachineContext} that represents the execution context for the workload.
     * @throws IllegalStateException if a workload is already active on the machine or if the machine is closed.
     */
    SimMachineContext startWorkload(SimWorkload workload, Map<String, Object> meta, Consumer<Exception> completion);

    /**
     * Cancel the active workload on this machine (if any).
     */
    void cancel();
}
