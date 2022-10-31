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

package org.opendc.simulator.compute.workload;

import org.opendc.simulator.compute.SimMachineContext;

/**
 * A model that characterizes the runtime behavior of some particular workload.
 *
 * <p>
 * Workloads are stateful objects that may be paused and resumed at a later moment. As such, be careful when using the
 * same {@link SimWorkload} from multiple contexts.
 */
public interface SimWorkload {
    /**
     * This method is invoked when the workload is started.
     *
     * @param ctx The execution context in which the machine runs.
     */
    void onStart(SimMachineContext ctx);

    /**
     * This method is invoked when the workload is stopped.
     *
     * @param ctx The execution context in which the machine runs.
     */
    void onStop(SimMachineContext ctx);

    /**
     * Create a snapshot of this workload.
     */
    SimWorkload snapshot();
}
