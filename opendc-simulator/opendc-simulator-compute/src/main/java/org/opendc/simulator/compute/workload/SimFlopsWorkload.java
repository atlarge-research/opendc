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
 * A {@link SimWorkload} that models applications as a static number of floating point operations executed on
 * multiple cores of a compute resource.
 */
public class SimFlopsWorkload implements SimWorkload, FlowStageLogic {
    private final long flops;
    private final double utilization;

    private SimMachineContext ctx;
    private FlowStage stage;
    private OutPort[] outputs;

    private float remainingAmount;
    private long lastUpdate;

    /**
     * Construct a new {@link SimFlopsWorkload}.
     *
     * @param flops The number of floating point operations to perform for this task in MFLOPs.
     * @param utilization The CPU utilization of the workload.
     */
    SimFlopsWorkload(long flops, double utilization) {
        if (flops < 0) {
            throw new IllegalArgumentException("Number of FLOPs must be positive");
        } else if (utilization <= 0.0 || utilization > 1.0) {
            throw new IllegalArgumentException("Utilization must be in (0, 1]");
        }

        this.flops = flops;
        this.utilization = utilization;
        this.remainingAmount = flops;
    }

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

        this.remainingAmount = flops;
        this.lastUpdate = graph.getEngine().getClock().millis();
    }

    @Override
    public void onStop(SimMachineContext ctx) {
        this.ctx = null;

        final FlowStage stage = this.stage;
        if (stage != null) {
            this.stage = null;
            stage.close();
        }
    }

    @Override
    public SimFlopsWorkload snapshot() {
        final FlowStage stage = this.stage;
        if (stage != null) {
            stage.sync();
        }

        return new SimFlopsWorkload((long) remainingAmount, utilization);
    }

    @Override
    public long onUpdate(FlowStage ctx, long now) {
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;

        long delta = Math.max(0, now - lastUpdate);

        float consumed = 0.f;
        float limit = 0.f;

        for (final OutPort output : outputs) {
            consumed += output.getRate() * delta;

            float outputLimit = (float) (output.getCapacity() * utilization);
            limit += outputLimit;

            output.push(outputLimit);
        }
        consumed = (float) (consumed * 0.001);

        float remainingAmount = this.remainingAmount - consumed;
        this.remainingAmount = remainingAmount;

        long duration = (long) Math.ceil(remainingAmount / limit * 1000);

        if (duration <= 0) {
            final SimMachineContext machineContext = this.ctx;
            if (machineContext != null) {
                machineContext.shutdown();
            }
            ctx.close();
            return Long.MAX_VALUE;
        }

        return now + duration;
    }

    @Override
    public String toString() {
        return "SimFlopsWorkload[FLOPs=" + flops + ",utilization=" + utilization + "]";
    }
}
