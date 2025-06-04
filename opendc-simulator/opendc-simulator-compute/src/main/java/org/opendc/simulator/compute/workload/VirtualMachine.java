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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.gpu.SimGpu;
import org.opendc.simulator.compute.machine.CpuPerformanceCounters;
import org.opendc.simulator.compute.machine.GpuPerformanceCounters;
import org.opendc.simulator.compute.machine.ResourcePerformanceCounters;
import org.opendc.simulator.compute.machine.SimMachine;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

/**
 * A {@link VirtualMachine} that composes multiple {@link SimWorkload}s.
 */
public final class VirtualMachine extends SimWorkload implements FlowSupplier {
    private final LinkedList<Workload> workloads;
    private int workloadIndex;

    private SimWorkload activeWorkload;
    private double cpuDemand = 0.0f; // TODO: Transform into list of resoureDemands
    private double cpuSupply = 0.0f;
    private double d = 0.0f;

    private double gpuDemand = 0.0f;
    private double gpuSupply = 0.0f;
    private double gpuD = 0.0f;
    private double gpuCapacity = 0.0f; // TODO: Transform into list of resource capacities

    private FlowEdge workloadEdge;
    private FlowEdge machineEdge; // TODO: Transform into list of resource edges, is not machine but distributors

    private double cpuCapacity = 0; // TODO: Transform into list of resource capacities

    private final long checkpointInterval;
    private final long checkpointDuration;
    private final double checkpointIntervalScaling;
    private CheckpointModel checkpointModel;

    private final ChainWorkload snapshot;

    private long lastUpdate;
    // TODO: Make it being used
    // TODO: is list a smart choice? or should list be handled in the counter itself?
    private final Hashtable<ResourceType, List<ResourcePerformanceCounters>> resourcePerformanceCounters = new Hashtable<>();
    private final CpuPerformanceCounters cpuPerformanceCounters = new CpuPerformanceCounters();
    private final List<GpuPerformanceCounters> gpuPerformanceCounters = new ArrayList<>();
    private Consumer<Exception> completion;

    private final List<ResourceType> availableResources = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TODO: How to handle GPU capacity?
    @Override
    public double getCapacity() {
        return this.cpuCapacity;
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

    public CpuPerformanceCounters getCpuPerformanceCounters() {
        return cpuPerformanceCounters;
    }

    public List<GpuPerformanceCounters> getGpuPerformanceCounters() {
        return gpuPerformanceCounters;
    }

    public GpuPerformanceCounters getSpecificGpuPerformanceCounters(int gpuId) {
        if (gpuId < 0 || gpuId >= gpuPerformanceCounters.size()) {
            throw new IndexOutOfBoundsException("No such GPU id: " + gpuId);
        }
        return gpuPerformanceCounters.get(gpuId);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    VirtualMachine(FlowSupplier supplier, ChainWorkload workload) {
        super(((FlowNode) supplier).getEngine());

        this.snapshot = workload;

        new FlowEdge(this, supplier);

        this.workloads = new LinkedList<>(workload.workloads());
        this.checkpointInterval = workload.checkpointInterval();
        this.checkpointDuration = workload.checkpointDuration();
        this.checkpointIntervalScaling = workload.checkpointIntervalScaling();

        this.lastUpdate = clock.millis();

        if (checkpointInterval > 0) {
            this.createCheckpointModel();
        }

        this.workloadIndex = -1;
        this.availableResources.add(supplier.getResourceType());
        this.onStart();
    }

    VirtualMachine(List<FlowSupplier> suppliers, ChainWorkload workload, SimMachine machine, Consumer<Exception> completion) {
        super(((FlowNode) suppliers.getFirst()).getEngine());

        this.snapshot = workload;

        for (FlowSupplier supplier : suppliers) {
            new FlowEdge(this, supplier);
            this.availableResources.add(supplier.getResourceType());
        }

        this.workloads = new LinkedList<>(workload.workloads());
        this.checkpointInterval = workload.checkpointInterval();
        this.checkpointDuration = workload.checkpointDuration();
        this.checkpointIntervalScaling = workload.checkpointIntervalScaling();

        this.lastUpdate = clock.millis();

        if (checkpointInterval > 0) {
            this.createCheckpointModel();
        }

        this.workloadIndex = -1;

        this.cpuCapacity = machine.getCpu().getFrequency();
        this.d = 1 / machine.getCpu().getFrequency();
        if (this.availableResources.contains(ResourceType.GPU)) {
            this.gpuD = 1 / machine.getGpu(0).getFrequency();
            this.gpuCapacity = machine.getGpu(0).getCapacity();
            for (SimGpu gpu : machine.getGpus()) {
                // if performance counter of GPUs are used, then values are twice as high as expected
                this.gpuPerformanceCounters.add(new GpuPerformanceCounters());
            }
        }
        this.completion = completion;


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

        this.activeWorkload = this.getNextWorkload().startWorkload(this);
    }

    public void updateCounters(long now) {
        // TODO: add other resources
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;
        long delta = now - lastUpdate;

        double cpuCapacity = 0.0f;
        if (delta > 0) {
            final double factor = this.d * delta;

            this.cpuPerformanceCounters.addCpuActiveTime(Math.round(this.cpuSupply * factor));
            this.cpuPerformanceCounters.setCpuIdleTime(Math.round((cpuCapacity - this.cpuSupply) * factor)); // Capacity is always 0.0f
            this.cpuPerformanceCounters.addCpuStealTime(Math.round((this.cpuDemand - this.cpuSupply) * factor));

            // TODO: Make loop
            if (this.availableResources.contains(ResourceType.GPU)) {
                final double gpuFactor = this.gpuD * delta;
                for (GpuPerformanceCounters gpuCounter : this.gpuPerformanceCounters) {
                    gpuCounter.addGpuActiveTime(Math.round(this.gpuSupply * gpuFactor));
                    gpuCounter.setGpuIdleTime(Math.round((this.gpuCapacity - this.gpuSupply) * gpuFactor));
                    gpuCounter.addGpuStealTime(Math.round((this.gpuDemand - this.gpuSupply) * gpuFactor));
                }
            }
        }

        this.cpuPerformanceCounters.setCpuDemand(this.cpuDemand);
        this.cpuPerformanceCounters.setCpuSupply(this.cpuSupply);
        this.cpuPerformanceCounters.setCpuCapacity(cpuCapacity);

        if (!this.availableResources.contains(ResourceType.GPU)) {
            return;
        }
        for (GpuPerformanceCounters gpuCounter : this.gpuPerformanceCounters) {
            gpuCounter.setGpuDemand(this.gpuDemand);
            gpuCounter.setGpuSupply(this.gpuSupply);
            gpuCounter.setGpuCapacity(this.gpuCapacity);
        }
    }

    @Override
    public long onUpdate(long now) {
        return Long.MAX_VALUE;
    }

    @Override
    public void stopWorkload() {
        this.stopWorkload(null);
    }

    private Exception stopWorkloadCause = null;

    public void stopWorkload(Exception cause) {
        if (cause != null) {
            this.stopWorkloadCause = cause;
        }

        if (this.checkpointModel != null) {
            this.checkpointModel.close();
            this.checkpointModel = null;
        }

        if (this.activeWorkload != null) {
            this.activeWorkload.stopWorkload();
            this.activeWorkload = null;
        }

        this.closeNode();
        if (this.completion != null) {
            this.completion.accept(stopWorkloadCause);
            this.completion = null;
        }
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
     * @param consumerEdge The edge to the workload
     */
    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.workloadEdge = consumerEdge;
    }

    /**
     * Add Connection to the cpuMux
     *
     * @param supplierEdge The edge to the cpuMux
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.machineEdge = supplierEdge;
        if (supplierEdge.getSupplier().getResourceType() == ResourceType.CPU) {
            this.cpuCapacity = supplierEdge.getCapacity();
        } else if (supplierEdge.getSupplier().getResourceType() == ResourceType.GPU) {
            this.gpuCapacity = supplierEdge.getCapacity();
        }
    }

    /**
     * Push demand to the cpuMux
     *
     * @param supplierEdge The edge to the cpuMux
     * @param newDemand new demand to sent to the cpu
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {

        if (supplierEdge.getSupplier().getResourceType() == ResourceType.CPU) {
            this.cpuDemand = newDemand;
        }
        if (supplierEdge.getSupplier().getResourceType() == ResourceType.GPU) {
            this.gpuDemand = newDemand;
        }
        this.machineEdge.pushDemand(newDemand);
    }

    /**
     * Push demand to the cpuMux
     *
     * @param supplierEdge The edge to the cpuMux
     * @param newDemand new demand to sent to the cpu
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand, ResourceType resourceType) {

        if (resourceType == ResourceType.CPU) {
            this.cpuDemand = newDemand;
        } else if (resourceType == ResourceType.GPU) {
            this.gpuDemand = newDemand;
        }
//        this.cpuDemand = newDemand;
        this.machineEdge.pushDemand(newDemand);
    }

    /**
     * Push supply to the workload
     *
     * @param consumerEdge The edge to the cpuMux
     * @param newSupply new supply to sent to the workload
     */
    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        this.cpuSupply = newSupply;
        this.machineEdge. pushDemand(newSupply, false);
    }

    /**
     * Push supply to the workload
     *
     * @param consumerEdge The edge to the cpuMux
     * @param newSupply new supply to sent to the workload
     */
    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply, ResourceType resourceType) {
        //TODO: Make it being used
        if (resourceType == ResourceType.CPU) {
            this.cpuSupply = newSupply;
        }else if (resourceType == ResourceType.GPU) {
            this.gpuSupply = newSupply;
        }
        this.workloadEdge.pushSupply(newSupply, false, resourceType);
    }

    /**
     * Handle new demand coming from the workload
     *
     * @param consumerEdge The edge to the workload
     * @param newDemand new demand coming from the workload
     */
    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand) {
        updateCounters(this.clock.millis());

        this.pushOutgoingDemand(this.machineEdge, newDemand);
    }

    /**
     * Handle new supply coming from the cpuMux
     *
     * @param supplierEdge The edge to the cpuMux
     * @param newSupply The new supply that is sent to the workload
     */
    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply) {
        updateCounters(this.clock.millis());

        this.pushOutgoingSupply(this.machineEdge, newSupply, supplierEdge.getSupplier().getResourceType());
    }

    /**
     * Handle new supply coming from the cpuMux
     *
     * @param supplierEdge The edge to the cpuMux
     * @param newSupply The new supply that is sent to the workload
     */
    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply, ResourceType resourceType) {
        updateCounters(this.clock.millis());

        this.pushOutgoingSupply(this.machineEdge, newSupply, resourceType);
    }

    /**
     * Handle the removal of the workload.
     * If there is a next workload available, start this workload
     * Otherwise, close this SimChainWorkload
     *
     * @param consumerEdge The edge to the active workload
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
            this.activeWorkload = getNextWorkload().startWorkload(this);
            return;
        }

        this.stopWorkload();
    }

    /**
     * Handle the removal of the connection to the cpuMux
     * When this happens, close the SimChainWorkload
     *
     * @param supplierEdge The edge to the cpuMux
     */
    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        if (this.machineEdge == null) {
            return;
        }

        this.stopWorkload();
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        List<FlowEdge> consumerEdges = (this.machineEdge != null) ? List.of(this.machineEdge) : List.of();
        List<FlowEdge> supplierEdges = (this.workloadEdge != null) ? List.of(this.workloadEdge) : List.of();

        return Map.of(
                FlowEdge.NodeType.CONSUMING, consumerEdges,
                FlowEdge.NodeType.SUPPLYING, supplierEdges);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.AUXILIARY;
    }

    public List<ResourceType> getAvailableResources() {
        return this.availableResources;
    }
}
