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
import org.opendc.simulator.compute.SimMachineContext;
import org.opendc.simulator.compute.SimProcessingUnit;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.FlowStage;
import org.opendc.simulator.flow2.FlowStageLogic;
import org.opendc.simulator.flow2.OutPort;

/**
 * A [SimWorkload] that models application execution as a single duration.
 */
public class SimRuntimeWorkload implements SimWorkload, FlowStageLogic {
    private long duration;
    private final double utilization;

    private SimMachineContext ctx;
    private FlowStage stage;
    private OutPort[] outputs;

    private long remainingDuration;
    private long lastUpdate;

    private long checkpointTime; // How long does it take to make a checkpoint?
    private long checkpointWait; // How long to wait until a new checkpoint is made?
    private long totalChecks;

    public SimRuntimeWorkload(long duration, double utilization) {
        this(duration, utilization, 0, 0);
        //        if (duration < 0) {
        //            throw new IllegalArgumentException("Duration must be positive");
        //        } else if (utilization <= 0.0 || utilization > 1.0) {
        //            throw new IllegalArgumentException("Utilization must be in (0, 1]");
        //        }
        //
        //        this.checkpointTime = 0L;
        //        this.checkpointWait = 0L;
        //        this.duration = duration;
        //
        //        this.utilization = utilization;
        //        this.remainingDuration = duration;
    }

    /**
     * Construct a new {@link SimRuntimeWorkload}.
     *
     * @param duration The duration of the workload in milliseconds.
     * @param utilization The CPU utilization of the workload.
     */
    public SimRuntimeWorkload(long duration, double utilization, long checkpointTime, long checkpointWait) {
        if (duration < 0) {
            throw new IllegalArgumentException("Duration must be positive");
        } else if (utilization <= 0.0 || utilization > 1.0) {
            throw new IllegalArgumentException("Utilization must be in (0, 1]");
        }

        this.checkpointTime = checkpointTime;
        this.checkpointWait = checkpointWait;
        this.duration = duration;

        if (this.checkpointWait > 0) {
            // Determine the number of checkpoints that need to be made during the workload
            // If the total duration is divisible by the wait time between checkpoints, we can remove the last
            // checkpoint
            int to_remove = ((this.duration % this.checkpointWait == 0) ? 1 : 0);
            this.totalChecks = this.duration / this.checkpointWait - to_remove;
            this.duration += (this.checkpointTime * totalChecks);
        }

        this.utilization = utilization;
        this.remainingDuration = duration;
    }

    @Override
    public void setOffset(long now) {}

    @Override
    public void onStart(SimMachineContext ctx) {
        this.ctx = ctx;

        final FlowGraph graph = ctx.getGraph();
        final FlowStage stage = graph.newStage(this);
        this.stage = stage;

        final List<? extends SimProcessingUnit> cpus = ctx.getCpus();
        final OutPort[] outputs = new OutPort[cpus.size()];
        this.outputs = outputs;

        for (int i = 0; i < cpus.size(); i++) {
            final SimProcessingUnit cpu = cpus.get(i);
            final OutPort output = stage.getOutlet("cpu" + i);

            graph.connect(output, cpu.getInput());
            outputs[i] = output;
        }

        this.remainingDuration = duration;
        this.lastUpdate = graph.getEngine().getClock().millis();
    }

    @Override
    public void onStop(SimMachineContext ctx) {
        this.ctx = null;

        final FlowStage stage = this.stage;
        if (stage != null) {
            this.stage = null;
            this.outputs = null;
            stage.close();
        }
    }

    @Override
    public SimRuntimeWorkload snapshot() {
        final FlowStage stage = this.stage;
        if (stage != null) {
            stage.sync();
        }

        var remaining_time = this.remainingDuration;

        if (this.checkpointWait > 0) {
            // Calculate last checkpoint
            var total_check_time = this.checkpointWait + this.checkpointTime;
            var processed_time = this.duration - this.remainingDuration;
            var processed_checks = (int) (processed_time / total_check_time);
            var processed_time_last_check =
                    (processed_checks * total_check_time); // The processed time after the last checkpoint

            remaining_time = this.duration
                    - processed_time_last_check; // The remaining duration to process after last checkpoint
            var remaining_checks = (int) (remaining_time / total_check_time);
            remaining_time -= (remaining_checks * checkpointTime);
        } else {
            remaining_time = duration;
        }

        return new SimRuntimeWorkload(remaining_time, utilization, this.checkpointTime, this.checkpointWait);
    }

    @Override
    public long onUpdate(FlowStage ctx, long now) {
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;

        long delta = now - lastUpdate;
        long duration = this.remainingDuration - delta;

        if (delta == 0 && this.ctx == null) {
            // This means the workload has been terminated
            // But, has not executed to completion
            return Long.MAX_VALUE;
        }

        if (duration <= 0) {
            final SimMachineContext machineContext = this.ctx;
            if (machineContext != null) {
                machineContext.shutdown();
            }
            ctx.close();
            return Long.MAX_VALUE;
        }

        this.remainingDuration = duration;

        for (final OutPort output : outputs) {
            float limit = (float) (output.getCapacity() * utilization);
            output.push(limit);
        }

        return now + duration;
    }

    @Override
    public String toString() {
        return "SimDurationWorkload[duration=" + duration + "ms,utilization=" + utilization + "]";
    }
}
