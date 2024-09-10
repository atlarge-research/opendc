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

import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import org.opendc.simulator.compute.SimMachineContext;
import org.opendc.simulator.compute.SimMemory;
import org.opendc.simulator.compute.SimNetworkInterface;
import org.opendc.simulator.compute.SimProcessingUnit;
import org.opendc.simulator.compute.SimStorageInterface;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.FlowStage;
import org.opendc.simulator.flow2.FlowStageLogic;

/**
 * A {@link SimWorkload} that composes two {@link SimWorkload}s.
 */
final class SimChainWorkload implements SimWorkload {
    private final SimWorkload[] workloads;
    private int activeWorkloadIndex;

    private Context activeContext;

    private long checkpointInterval = 0;
    private long checkpointDuration = 0;



    private double checkpointIntervalScaling = 1.0;
    private CheckPointModel checkpointModel;
    private SimChainWorkload snapshot;

    /**
     * Construct a {@link SimChainWorkload} instance.
     *
     * @param workloads The workloads to chain.
     * @param activeWorkloadIndex The index of the active workload.
     */
    SimChainWorkload(SimWorkload[] workloads, int activeWorkloadIndex) {
        this.workloads = workloads;

        if (this.workloads.length > 1) {
            checkpointInterval = this.workloads[1].getCheckpointInterval();
            checkpointDuration = this.workloads[1].getCheckpointDuration();
            checkpointIntervalScaling = this.workloads[1].getCheckpointIntervalScaling();
        }

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
    public long getCheckpointInterval() {
        return checkpointInterval;
    }

    @Override
    public long getCheckpointDuration() {
        return checkpointDuration;
    }

    @Override
    public double getCheckpointIntervalScaling() {
        return checkpointIntervalScaling;
    }

    @Override
    public void setOffset(long now) {
        for (SimWorkload workload : this.workloads) {
            workload.setOffset(now);
        }
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

        if (checkpointInterval > 0) {
            this.createCheckpointModel();
            this.checkpointModel.start();
        }

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

        if (this.checkpointModel != null) {
            this.checkpointModel.stop();
        }

        tryThrow(context.doStop(workloads[activeWorkloadIndex]));
    }

    @Override
    public void makeSnapshot(long now) {
        final int activeWorkloadIndex = this.activeWorkloadIndex;
        final SimWorkload[] workloads = this.workloads;
        final SimWorkload[] newWorkloads = new SimWorkload[workloads.length - activeWorkloadIndex];

        for (int i = 0; i < newWorkloads.length; i++) {
            workloads[activeWorkloadIndex + i].makeSnapshot(now);
            newWorkloads[i] = workloads[activeWorkloadIndex + i].getSnapshot();
        }

        this.snapshot = new SimChainWorkload(newWorkloads, 0);
    }

    @Override
    public SimChainWorkload getSnapshot() {
        return this.snapshot;
    }

    @Override
    public void createCheckpointModel() {
        this.checkpointModel = new CheckPointModel(activeContext, this, this.checkpointInterval, this.checkpointDuration, this.checkpointIntervalScaling);
    }

    private class CheckPointModel implements FlowStageLogic {
        private SimChainWorkload workload;
        private long checkpointInterval;
        private long checkpointDuration;
        private double checkpointIntervalScaling = 1.0;
        private FlowStage stage;

        private long startOfInterval;
        private Boolean firstCheckPoint = true;

        CheckPointModel(Context context,
                        SimChainWorkload workload,
                        long checkpointInterval,
                        long checkpointDuration,
                        double checkpointIntervalScaling) {
            this.checkpointInterval = checkpointInterval;
            this.checkpointDuration = checkpointDuration;
            this.checkpointIntervalScaling = checkpointIntervalScaling;
            this.workload = workload;



            this.stage = context.getGraph().newStage(this);

            InstantSource clock = this.stage.getGraph().getEngine().getClock();

            this.startOfInterval = clock.millis();
        }

        @Override
        public long onUpdate(FlowStage ctx, long now) {
            long passedTime = now - startOfInterval;
            long remainingTime = this.checkpointInterval - passedTime;

            if (!this.firstCheckPoint) {
                remainingTime += this.checkpointDuration;
            }

            // Interval not completed
            if (remainingTime > 0) {
                return now + remainingTime;
            }

            workload.makeSnapshot(now);
            if (firstCheckPoint) {this.firstCheckPoint = false;}

            // Scale the interval time between checkpoints based on the provided scaling
            this.checkpointInterval = (long) (this.checkpointInterval * this.checkpointIntervalScaling);

            return now + this.checkpointInterval + this.checkpointDuration;
        }

        public void start() {
            this.stage.sync();
        }

        public void stop() {
            this.stage.close();
        }
    }

    /**
     * A {@link SimMachineContext} that intercepts the shutdown calls.
     */
    private class Context implements SimMachineContext {
        private final SimMachineContext ctx;
        private SimWorkload snapshot;

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
        public void makeSnapshot(long now) {
            final SimWorkload workload = workloads[activeWorkloadIndex];
            this.snapshot = workload.getSnapshot();
        }

        @Override
        public SimWorkload getSnapshot(long now) {
            this.makeSnapshot(now);

            return this.snapshot;
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

            if (SimChainWorkload.this.checkpointModel != null) {
                SimChainWorkload.this.checkpointModel.stop();
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
