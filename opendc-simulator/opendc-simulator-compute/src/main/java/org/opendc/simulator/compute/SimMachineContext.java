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
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.flow2.FlowGraph;

/**
 * A simulated execution context in which a bootable image runs.
 *
 * <p>
 * This interface represents the interface between the running image (e.g. operating system) and the physical
 * or virtual firmware on which the image runs.
 */
public interface SimMachineContext {
    /**
     * Return the {@link FlowGraph} in which the workload executes.
     */
    FlowGraph getGraph();

    /**
     * Return the metadata associated with the context.
     * <p>
     * Users can pass this metadata to the workload via {@link SimMachine#startWorkload(SimWorkload, Map, Consumer)}.
     */
    Map<String, Object> getMeta();

    /**
     * Return the CPUs available on the machine.
     */
    List<? extends SimProcessingUnit> getCpus();

    /**
     * Return the memory interface of the machine.
     */
    SimMemory getMemory();

    /**
     * Return the network interfaces available to the workload.
     */
    List<? extends SimNetworkInterface> getNetworkInterfaces();

    /**
     * Return the storage devices available to the workload.
     */
    List<? extends SimStorageInterface> getStorageInterfaces();

    /**
     * Create a snapshot of the {@link SimWorkload} running on this machine.
     *
     * @throws UnsupportedOperationException if the workload does not support snapshotting.
     */
    SimWorkload snapshot();

    /**
     * Reset all resources of the machine.
     */
    void reset();

    /**
     * Shutdown the workload.
     */
    void shutdown();

    /**
     * Shutdown the workload due to failure.
     *
     * @param cause The cause for shutting down the workload.
     */
    void shutdown(Exception cause);
}
