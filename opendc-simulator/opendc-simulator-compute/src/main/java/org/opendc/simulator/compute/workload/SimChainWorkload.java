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

import java.util.List;
import java.util.Map;
import org.opendc.simulator.compute.SimMachineContext;
import org.opendc.simulator.compute.SimMemory;
import org.opendc.simulator.compute.SimNetworkInterface;
import org.opendc.simulator.compute.SimProcessingUnit;
import org.opendc.simulator.compute.SimStorageInterface;
import org.opendc.simulator.flow2.FlowGraph;

/**
 * A {@link SimWorkload} that composes two {@link SimWorkload}s.
 */
final class SimChainWorkload implements SimWorkload {
    private final SimWorkload[] workloads;
    private int activeWorkloadIndex;

    private Context activeContext;

    /**
     * Construct a {@link SimChainWorkload} instance.
     *
     * @param workloads The workloads to chain.
     * @param activeWorkloadIndex The index of the active workload.
     */
    SimChainWorkload(SimWorkload[] workloads, int activeWorkloadIndex) {
        this.workloads = workloads;
        this.activeWorkloadIndex = activeWorkloadIndex;
    }

    /**
     * Construct a {@link SimChainWorkload} instance.
     *
     * @param workloads The workloads to chain.
     */
    SimChainWorkload(SimWorkload... workloads) {
        this(workloads, 0);
    }

    @Override
    public void onStart(SimMachineContext ctx) {
        final SimWorkload[] workloads = this.workloads;
        final int activeWorkloadIndex = this.activeWorkloadIndex;

        if (activeWorkloadIndex >= workloads.length) {
            return;
        }

        final Context context = new Context(ctx);
        activeContext = context;

        tryThrow(context.doStart(workloads[activeWorkloadIndex]));
    }

    @Override
    public void onStop(SimMachineContext ctx) {
        final SimWorkload[] workloads = this.workloads;
        final int activeWorkloadIndex = this.activeWorkloadIndex;

        if (activeWorkloadIndex >= workloads.length) {
            return;
        }

        final Context context = activeContext;
        activeContext = null;

        tryThrow(context.doStop(workloads[activeWorkloadIndex]));
    }

    @Override
    public SimChainWorkload snapshot() {
        final int activeWorkloadIndex = this.activeWorkloadIndex;
        final SimWorkload[] workloads = this.workloads;
        final SimWorkload[] newWorkloads = new SimWorkload[workloads.length - activeWorkloadIndex];

        for (int i = 0; i < newWorkloads.length; i++) {
            newWorkloads[i] = workloads[activeWorkloadIndex + i].snapshot();
        }

        return new SimChainWorkload(newWorkloads, 0);
    }

    /**
     * A {@link SimMachineContext} that intercepts the shutdown calls.
     */
    private class Context implements SimMachineContext {
        private final SimMachineContext ctx;

        private Context(SimMachineContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public FlowGraph getGraph() {
            return ctx.getGraph();
        }

        @Override
        public Map<String, Object> getMeta() {
            return ctx.getMeta();
        }

        @Override
        public List<? extends SimProcessingUnit> getCpus() {
            return ctx.getCpus();
        }

        @Override
        public SimMemory getMemory() {
            return ctx.getMemory();
        }

        @Override
        public List<? extends SimNetworkInterface> getNetworkInterfaces() {
            return ctx.getNetworkInterfaces();
        }

        @Override
        public List<? extends SimStorageInterface> getStorageInterfaces() {
            return ctx.getStorageInterfaces();
        }

        @Override
        public SimWorkload snapshot() {
            final SimWorkload workload = workloads[activeWorkloadIndex];
            return workload.snapshot();
        }

        @Override
        public void reset() {
            ctx.reset();
        }

        @Override
        public void shutdown() {
            shutdown(null);
        }

        @Override
        public void shutdown(Exception cause) {
            final SimWorkload[] workloads = SimChainWorkload.this.workloads;
            final int activeWorkloadIndex = ++SimChainWorkload.this.activeWorkloadIndex;

            final Exception stopException = doStop(workloads[activeWorkloadIndex - 1]);
            if (cause == null) {
                cause = stopException;
            } else if (stopException != null) {
                cause.addSuppressed(stopException);
            }

            if (stopException == null && activeWorkloadIndex < workloads.length) {
                ctx.reset();

                final Exception startException = doStart(workloads[activeWorkloadIndex]);

                if (startException == null) {
                    return;
                } else if (cause == null) {
                    cause = startException;
                } else {
                    cause.addSuppressed(startException);
                }
            }

            ctx.shutdown(cause);
        }

        /**
         * Start the specified workload.
         *
         * @return The {@link Exception} that occurred while starting the workload or <code>null</code> if the workload
         *         started successfully.
         */
        private Exception doStart(SimWorkload workload) {
            try {
                workload.onStart(this);
            } catch (Exception cause) {
                final Exception stopException = doStop(workload);
                if (stopException != null) {
                    cause.addSuppressed(stopException);
                }
                return cause;
            }

            return null;
        }

        /**
         * Stop the specified workload.
         *
         * @return The {@link Exception} that occurred while stopping the workload or <code>null</code> if the workload
         *         stopped successfully.
         */
        private Exception doStop(SimWorkload workload) {
            try {
                workload.onStop(this);
            } catch (Exception cause) {
                return cause;
            }

            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void tryThrow(Throwable e) throws T {
        if (e == null) {
            return;
        }
        throw (T) e;
    }
}
