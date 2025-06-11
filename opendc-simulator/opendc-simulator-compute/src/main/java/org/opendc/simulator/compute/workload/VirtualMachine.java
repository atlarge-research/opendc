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
import java.util.Objects;
import java.util.function.Consumer;

import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.ComputeResource;
import org.opendc.simulator.compute.gpu.SimGpu;
import org.opendc.simulator.compute.machine.CpuPerformanceCounters;
import org.opendc.simulator.compute.machine.GpuPerformanceCounters;
import org.opendc.simulator.compute.machine.ResourcePerformanceCounters;
import org.opendc.simulator.compute.machine.SimMachine;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link VirtualMachine} that composes multiple {@link SimWorkload}s.
 */
public final class VirtualMachine extends SimWorkload implements FlowSupplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualMachine.class);
    private final LinkedList<Workload> workloads;
    private int workloadIndex;

    private SimWorkload activeWorkload;

    private FlowEdge workloadEdge;

    private final Hashtable<ResourceType, ArrayList<Double>> resourceDemands = new Hashtable<>();
    private final Hashtable<ResourceType, ArrayList<Double>> resourceSupplies = new Hashtable<>();
    private final Hashtable<ResourceType, Double> resourceCapacities = new Hashtable<>();
    private final Hashtable<ResourceType, Double> resourceTimeScalingFactor = new Hashtable<>(); // formerly known as d
    private final Hashtable<ResourceType, FlowEdge> distributorEdges = new Hashtable<>();
    private final Hashtable<ResourceType, List<ResourcePerformanceCounters>> resourcePerformanceCounters = new Hashtable<>();

    private final long checkpointInterval;
    private final long checkpointDuration;
    private final double checkpointIntervalScaling;
    private CheckpointModel checkpointModel;

    private final ChainWorkload snapshot;

    private long lastUpdate;
    private Consumer<Exception> completion;

    private final List<ResourceType> availableResources = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TODO: How to handle GPU capacity?
    @Override
    public double getCapacity() {
        throw new UnsupportedOperationException("getCapacity() is not supported for VirtualMachine");
    }

    @Override
    public double getCapacity(ResourceType resourceType) {
        if (resourceType == ResourceType.AUXILIARY) {
            return 0.0;
        }
        return this.resourceCapacities.get(resourceType);
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
        return (CpuPerformanceCounters) this.resourcePerformanceCounters.get(ResourceType.CPU).getFirst();
    }

    public List<GpuPerformanceCounters> getGpuPerformanceCounters() {
        List<ResourcePerformanceCounters> gpuPerformanceCounters = this.resourcePerformanceCounters.get(ResourceType.GPU);
        return (gpuPerformanceCounters != null) ?
                gpuPerformanceCounters.stream()
                .filter(c -> c instanceof GpuPerformanceCounters)
                .map(c -> (GpuPerformanceCounters) c)
                .toList() : new ArrayList<>();
    }

    public GpuPerformanceCounters getSpecificGpuPerformanceCounters(int gpuId) {
        List<ResourcePerformanceCounters> gpuPerformanceCounters = this.resourcePerformanceCounters.get(ResourceType.GPU);
        if (gpuId < 0 || gpuId >= gpuPerformanceCounters.size()) {
            throw new IndexOutOfBoundsException("No such GPU id: " + gpuId);
        }
        return (GpuPerformanceCounters) gpuPerformanceCounters.get(gpuId);
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
            ResourceType resourceType = supplier.getResourceType();

            this.availableResources.add(resourceType);

            ArrayList<ComputeResource> resources = machine.getResources(resourceType);
            if (resources.isEmpty()) {
                throw new IllegalArgumentException("No resources of type " + resourceType + " found in machine ");
            }

            this.resourceCapacities.put(resourceType, resources.getFirst().getCapacity());

            ArrayList<ResourcePerformanceCounters> performanceCounters = new ArrayList<>();
            ArrayList<Double> resourceDemands = new ArrayList<>();
            ArrayList<Double> resourceSupplies = new ArrayList<>();
            for (ComputeResource resource : resources) {
                // TODO: not so cool, but works for now
                switch (resourceType) {
                    case CPU -> performanceCounters.add(new CpuPerformanceCounters());
                    case GPU -> performanceCounters.add(new GpuPerformanceCounters());
                    default -> throw new IllegalArgumentException("Unsupported resource type: " + resourceType);
                }
                this.resourceTimeScalingFactor.put(resourceType, 1.0 / resource.getCapacity());
                resourceDemands.add(0.0);
                resourceSupplies.add(0.0);
            }
            this.resourcePerformanceCounters.put(resourceType, performanceCounters);
            this.resourceDemands.put(resourceType, resourceDemands);
            this.resourceSupplies.put(resourceType, resourceSupplies);
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

        for (ResourceType resourceType : this.availableResources) {
            int i = 0;
            final double factor = this.resourceTimeScalingFactor.get(resourceType) * delta;
            for (ResourcePerformanceCounters performanceCounter : this.resourcePerformanceCounters.get(resourceType)) {
                if (delta > 0) {
                    performanceCounter.addActiveTime(Math.round(this.resourceSupplies.get(resourceType).get(i) * factor));
                    performanceCounter.setIdleTime(Math.round((this.resourceCapacities.get(resourceType) - this.resourceSupplies.get(resourceType).get(i)) * factor));
                    performanceCounter.addStealTime(Math.round((this.resourceDemands.get(resourceType).get(i) - this.resourceSupplies.get(resourceType).get(i)) * factor));
                }
                performanceCounter.setDemand(this.resourceDemands.get(resourceType).get(i));
                performanceCounter.setSupply(this.resourceSupplies.get(resourceType).get(i));
                performanceCounter.setCapacity(this.resourceCapacities.get(resourceType));
                i++;
            }
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
        this.resourceCapacities.put(supplierEdge.getSupplier().getResourceType(), supplierEdge.getCapacity());
        this.distributorEdges.put(supplierEdge.getSupplier().getResourceType(), supplierEdge);
    }

    /**
     * Push demand to the cpuMux
     *
     * @param supplierEdge The edge to the cpuMux
     * @param newDemand new demand to sent to the cpu
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {
        // FIXME: Needs to be assigned to specific resource if multiple exist -> add resource Id as parameter
        this.pushOutgoingDemand(supplierEdge, newDemand, supplierEdge.getSupplier().getResourceType());
    }

    /**
     * Push demand to the cpuMux
     *
     * @param supplierEdge The edge to the cpuMux
     * @param newDemand new demand to sent to the cpu
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand, ResourceType resourceType) {
        // FIXME: Needs to be assigned to specific resource if multiple exist -> add resource Id as parameter
        this.resourceDemands.put(resourceType, new ArrayList<>(List.of(newDemand)));
        this.distributorEdges.get(resourceType).pushDemand(newDemand, false, resourceType);
    }

    /**
     * Push supply to the workload
     *
     * @param consumerEdge The edge to the cpuMux
     * @param newSupply new supply to sent to the workload
     */
    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        this.resourceSupplies.put(consumerEdge.getConsumer().getResourceType(), new ArrayList<>(List.of(newSupply)));
        this.distributorEdges.get(consumerEdge.getConsumer().getResourceType()).pushSupply(newSupply, false, consumerEdge.getConsumer().getResourceType());
    }

    /**
     * Push supply to the workload
     *
     * @param consumerEdge The edge to the cpuMux
     * @param newSupply new supply to sent to the workload
     */
    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply, ResourceType resourceType) {
        this.resourceSupplies.put(resourceType, new ArrayList<>(List.of(newSupply)));
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
        this.pushOutgoingDemand(this.distributorEdges.get(consumerEdge.getConsumer().getResourceType()), newDemand);
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand, ResourceType resourceType) {
        updateCounters(this.clock.millis());
        this.pushOutgoingDemand(this.distributorEdges.get(resourceType), newDemand, resourceType);
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

        this.pushOutgoingSupply(this.distributorEdges.get(supplierEdge.getSupplier().getResourceType()), newSupply, supplierEdge.getSupplier().getResourceType());
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
        
        this.pushOutgoingSupply(this.distributorEdges.get(resourceType), newSupply, resourceType);
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
        if (!this.distributorEdges.contains(supplierEdge.getSupplier().getResourceType())) {
            return;
        }

        this.stopWorkload();
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        List<FlowEdge> consumerEdges = this.distributorEdges.values().stream()
                .filter(Objects::nonNull)
                .toList();
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
