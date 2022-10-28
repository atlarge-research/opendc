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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SimWorkload} that composes two {@link SimWorkload}s.
 */
final class SimChainWorkload implements SimWorkload {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimChainWorkload.class);

    private final SimWorkload[] workloads;
    private int activeWorkloadIndex;

    private Context activeContext;

    /**
     * Construct a {@link SimChainWorkload} instance.
     *
     * @param workloads The workloads to chain.
     */
    SimChainWorkload(SimWorkload... workloads) {
        this.workloads = workloads;
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

        if (!context.doStart(workloads[activeWorkloadIndex])) {
            ctx.shutdown();
        }
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

        context.doStop(workloads[activeWorkloadIndex]);
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
        public void reset() {
            ctx.reset();
        }

        @Override
        public void shutdown() {
            final SimWorkload[] workloads = SimChainWorkload.this.workloads;
            final int activeWorkloadIndex = ++SimChainWorkload.this.activeWorkloadIndex;

            if (doStop(workloads[activeWorkloadIndex - 1]) && activeWorkloadIndex < workloads.length) {
                ctx.reset();

                if (doStart(workloads[activeWorkloadIndex])) {
                    return;
                }
            }

            ctx.shutdown();
        }

        /**
         * Start the specified workload.
         *
         * @return <code>true</code> if the workload started successfully, <code>false</code> otherwise.
         */
        private boolean doStart(SimWorkload workload) {
            try {
                workload.onStart(this);
            } catch (Exception cause) {
                LOGGER.warn("Workload failed during onStart callback", cause);
                doStop(workload);
                return false;
            }

            return true;
        }

        /**
         * Stop the specified workload.
         *
         * @return <code>true</code> if the workload stopped successfully, <code>false</code> otherwise.
         */
        private boolean doStop(SimWorkload workload) {
            try {
                workload.onStop(this);
            } catch (Exception cause) {
                LOGGER.warn("Workload failed during onStop callback", cause);
                return false;
            }

            return true;
        }
    }
}
