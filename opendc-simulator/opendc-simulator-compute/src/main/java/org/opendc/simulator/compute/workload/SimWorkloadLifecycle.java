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

import java.util.HashSet;
import org.opendc.simulator.compute.SimMachineContext;

/**
 * A helper class to manage the lifecycle of a {@link SimWorkload}.
 */
public final class SimWorkloadLifecycle {
    private final SimMachineContext ctx;
    private final HashSet<Runnable> waiting = new HashSet<>();

    /**
     * Construct a {@link SimWorkloadLifecycle} instance.
     *
     * @param ctx The {@link SimMachineContext} of the workload.
     */
    public SimWorkloadLifecycle(SimMachineContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Register a "completer" callback that must be invoked before ending the lifecycle of the workload.
     */
    public Runnable newCompleter() {
        Runnable completer = new Runnable() {
            @Override
            public void run() {
                final HashSet<Runnable> waiting = SimWorkloadLifecycle.this.waiting;
                if (waiting.remove(this) && waiting.isEmpty()) {
                    ctx.shutdown();
                }
            }
        };
        waiting.add(completer);
        return completer;
    }
}
