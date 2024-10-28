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

import java.util.LinkedList;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowNode;
import org.opendc.simulator.engine.FlowSupplier;

/**
 * A {@link SimChainWorkload} that composes multiple {@link SimWorkload}s.
 */
final class SimChainWorkload extends SimWorkload implements FlowSupplier {
    private final LinkedList<Workload> workloads;
    private int workloadIndex;

    private SimWorkload activeWorkload;
    private double demand = 0.0f;
    private double supply = 0.0f;

    private FlowEdge workloadEdge;
    private FlowEdge machineEdge;

    private double capacity = 0;

    private long checkpointInterval = 0;
    private long checkpointDuration = 0;
    private double checkpointIntervalScaling = 1.0;
    private CheckpointModel checkpointModel;

    private ChainWorkload snapshot;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public double getCapacity() {
        return this.capacity;
    }

    @Override
    public ChainWorkload getSnapshot() {
        return this.snapshot;
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    SimChainWorkload(FlowSupplier supplier, ChainWorkload workload, long now) {
        super(((FlowNode) supplier).getGraph());

        this.snapshot = workload;

        this.parentGraph = ((FlowNode) supplier).getGraph();
        this.parentGraph.addEdge(this, supplier);

        this.clock = this.parentGraph.getEngine().getClock();
        this.workloads = new LinkedList<>(workload.getWorkloads());
        this.checkpointInterval = workload.getCheckpointInterval();
        this.checkpointDuration = workload.getCheckpointDuration();
        this.checkpointIntervalScaling = workload.getCheckpointIntervalScaling();

        if (checkpointInterval > 0) {
            this.createCheckpointModel();
        }

        this.workloadIndex = -1;

        this.onStart();
    }

    public Workload getNextWorkload() {
        this.workloadIndex++;
        return workloads.pop();
    }

    // TODO: Combine with Constructor
    public void onStart() {
        if (this.workloads.isEmpty()) {
            return;
        }

        // Create and start a checkpoint model if initiated
        if (checkpointInterval > 0) {
            this.checkpointModel.start();
        }

        this.activeWorkload = this.getNextWorkload().startWorkload(this, this.clock.millis());
    }

    @Override
    public long onUpdate(long now) {
        return Long.MAX_VALUE;
    }

    @Override
    public void stopWorkload() {
        if (this.checkpointModel != null) {
            this.checkpointModel.close();
            this.checkpointModel = null;
        }

        if (this.activeWorkload != null) {
            this.activeWorkload.stopWorkload();
            this.activeWorkload = null;
        }

        this.closeNode();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Checkpoint related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void createCheckpointModel() {
        this.checkpointModel = new CheckpointModel(this);
    }

    @Override
    public void makeSnapshot(long now) {

        this.snapshot.removeWorkloads(this.workloadIndex);
        this.workloadIndex = 0;

        activeWorkload.makeSnapshot(now);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add connection to the active workload
     *
     * @param consumerEdge
     */
    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.workloadEdge = consumerEdge;
    }

    /**
     * Add Connection to the cpuMux
     * @param supplierEdge
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.machineEdge = supplierEdge;
        this.capacity = supplierEdge.getCapacity();
    }

    /**
     * Push demand to the cpuMux
     *
     * @param supplierEdge
     * @param newDemand
     */
    @Override
    public void pushDemand(FlowEdge supplierEdge, double newDemand) {
        this.machineEdge.pushDemand(newDemand);
    }

    /**
     * Push supply to the workload
     *
     * @param consumerEdge
     * @param newSupply
     */
    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {
        this.workloadEdge.pushSupply(newSupply);
    }

    /**
     * Handle new demand coming from the workload
     *
     * @param consumerEdge
     * @param newDemand
     */
    @Override
    public void handleDemand(FlowEdge consumerEdge, double newDemand) {
        if (newDemand == this.demand) {
            return;
        }

        this.demand = newDemand;
        this.pushDemand(this.machineEdge, newDemand);
    }

    /**
     * Handle new supply coming from the cpuMux
     *
     * @param supplierEdge
     * @param newSupply
     */
    @Override
    public void handleSupply(FlowEdge supplierEdge, double newSupply) {
        if (newSupply == this.supply) {
            return;
        }

        this.pushSupply(this.machineEdge, newSupply);
    }

    /**
     * Handle the removal of the workload.
     * If there is a next workload available, start this workload
     * Otherwise, close this SimChainWorkload
     *
     * @param consumerEdge
     */
    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        if (this.workloadEdge == null) {
            return;
        }

        // Remove the connection to the active workload
        this.activeWorkload = null;
        this.workloadEdge = null;

        // Start next workload
        if (!this.workloads.isEmpty()) {
            this.activeWorkload = getNextWorkload().startWorkload(this, this.clock.millis());
            return;
        }

        this.stopWorkload();
    }

    /**
     * Handle the removal of the connection to the cpuMux
     * When this happens, close the SimChainWorkload
     *
     * @param supplierEdge
     */
    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        this.stopWorkload();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void tryThrow(Throwable e) throws T {
        if (e == null) {
            return;
        }
        throw (T) e;
    }
}
