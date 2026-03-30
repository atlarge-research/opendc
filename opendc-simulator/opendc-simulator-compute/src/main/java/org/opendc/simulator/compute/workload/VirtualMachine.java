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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.ComputeResource;
import org.opendc.simulator.compute.machine.PerformanceCounters;
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

    private final List<ResourceType> usedResourceTypes = new ArrayList<>();

    private final double[] resourceDemands = new double[ResourceType.values().length];
    private final double[] resourceSupplies = new double[ResourceType.values().length];
    private final double[] resourceCapacities = new double[ResourceType.values().length];
    private final double[] resourceTimeScalingFactor = new double[ResourceType.values().length]; // formerly known as d
    private final FlowEdge[] distributorEdges = new FlowEdge[ResourceType.values().length];
    private final PerformanceCounters[] resourcePerformanceCounters =
            new PerformanceCounters[ResourceType.values().length];

    private final long checkpointInterval;
    private final long checkpointDuration;
    private final double checkpointIntervalScaling;
    private CheckpointModel checkpointModel;

    private final ChainWorkload snapshot;

    private long lastUpdate;
    private Consumer<Exception> completion;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public double getCapacity() {
        throw new UnsupportedOperationException("getCapacity() is not supported for VirtualMachine");
    }

    @Override
    public double getCapacity(ResourceType resourceType) {
        if (resourceType == ResourceType.AUXILIARY) {
            return 0.0;
        }
        return this.resourceCapacities[resourceType.ordinal()];
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

    public PerformanceCounters getCpuPerformanceCounters() {
        return this.resourcePerformanceCounters[ResourceType.CPU.ordinal()];
    }

    public PerformanceCounters getGpuPerformanceCounters() {
        return this.resourcePerformanceCounters[ResourceType.GPU.ordinal()];
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
        this.usedResourceTypes.add(supplier.getSupplierResourceType());
        this.onStart();
    }

    VirtualMachine(
            List<FlowSupplier> suppliers, ChainWorkload workload, SimMachine machine, Consumer<Exception> completion) {
        super(((FlowNode) suppliers.getFirst()).getEngine());

        this.snapshot = workload;

        for (FlowSupplier supplier : suppliers) {
            new FlowEdge(this, supplier);
            ResourceType resourceType = supplier.getSupplierResourceType();

            this.usedResourceTypes.add(resourceType);

            ArrayList<ComputeResource> resources = machine.getResources(resourceType);
            if (resources.isEmpty()) {
                throw new IllegalArgumentException("No resources of type " + resourceType + " found in machine ");
            }

            this.resourceCapacities[resourceType.ordinal()] =
                    resources.getFirst().getCapacity();

            this.resourceTimeScalingFactor[resourceType.ordinal()] =
                    1.0 / resources.getFirst().getCapacity();
            this.resourcePerformanceCounters[resourceType.ordinal()] = new PerformanceCounters();
            this.resourceDemands[resourceType.ordinal()] = 0.0;
            this.resourceSupplies[resourceType.ordinal()] = 0.0;
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
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;
        long delta = now - lastUpdate;

        for (ResourceType resourceType : this.usedResourceTypes) {
            final double factor = this.resourceTimeScalingFactor[resourceType.ordinal()] * delta;
            if (delta > 0) {
                this.resourcePerformanceCounters[resourceType.ordinal()].addActiveTime(
                        Math.round(this.resourceSupplies[resourceType.ordinal()] * factor));
                this.resourcePerformanceCounters[resourceType.ordinal()].setIdleTime(
                        Math.round((this.resourceCapacities[resourceType.ordinal()]
                                        - this.resourceSupplies[resourceType.ordinal()])
                                * factor));
                this.resourcePerformanceCounters[resourceType.ordinal()].addStealTime(Math.round(
                        (this.resourceDemands[resourceType.ordinal()] - this.resourceSupplies[resourceType.ordinal()])
                                * factor));
            }
            this.resourcePerformanceCounters[resourceType.ordinal()].setDemand(
                    this.resourceDemands[resourceType.ordinal()]);
            this.resourcePerformanceCounters[resourceType.ordinal()].setSupply(
                    this.resourceSupplies[resourceType.ordinal()]);
            this.resourcePerformanceCounters[resourceType.ordinal()].setCapacity(
                    this.resourceCapacities[resourceType.ordinal()]);
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
     * Add Connection to the resource flow distributor
     *
     * @param supplierEdge The edge to the resource flow distributor
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        ResourceType resourceType = supplierEdge.getSupplierResourceType();
        this.resourceCapacities[resourceType.ordinal()] = supplierEdge.getCapacity();
        this.distributorEdges[resourceType.ordinal()] = supplierEdge;
    }

    /**
     * Push demand to the resource flow distributor
     *
     * @param supplierEdge The edge to the resource flow distributor
     * @param newDemand new demand to sent to the resource flow distributor
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {
        this.pushOutgoingDemand(supplierEdge, newDemand, supplierEdge.getSupplierResourceType());
    }

    /**
     * Push demand to the resource flow distributor
     *
     * @param supplierEdge The edge to the resource flow distributor
     * @param newDemand new demand to sent to the resource flow distributor
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand, ResourceType resourceType) {
        this.resourceDemands[resourceType.ordinal()] = newDemand;
        this.distributorEdges[resourceType.ordinal()].pushDemand(newDemand, false, resourceType);
    }

    /**
     * Push supply to the workload
     *
     * @param consumerEdge The edge to the resource flow distributor
     * @param newSupply new supply to sent to the workload
     */
    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        this.resourceSupplies[consumerEdge.getConsumerResourceType().ordinal()] = newSupply;
        this.distributorEdges[consumerEdge.getConsumerResourceType().ordinal()].pushSupply(
                newSupply, false, consumerEdge.getConsumerResourceType());
    }

    /**
     * Push supply to the workload
     *
     * @param consumerEdge The edge to the resource flow distributor
     * @param newSupply new supply to sent to the workload
     */
    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply, ResourceType resourceType) {
        this.resourceSupplies[resourceType.ordinal()] = newSupply;
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
        this.pushOutgoingDemand(
                this.distributorEdges[consumerEdge.getConsumerResourceType().ordinal()], newDemand);
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand, ResourceType resourceType) {
        updateCounters(this.clock.millis());
        this.pushOutgoingDemand(this.distributorEdges[resourceType.ordinal()], newDemand, resourceType);
    }

    /**
     * Handle new supply coming from the resource flow distributor
     *
     * @param supplierEdge The edge to the resource flow distributor
     * @param newSupply The new supply that is sent to the workload
     */
    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply) {
        updateCounters(this.clock.millis());

        this.pushOutgoingSupply(
                this.distributorEdges[supplierEdge.getSupplierResourceType().ordinal()],
                newSupply,
                supplierEdge.getSupplierResourceType());
    }

    /**
     * Handle new supply coming from the resource flow distributor
     *
     * @param supplierEdge The edge to the resource flow distributor
     * @param newSupply The new supply that is sent to the workload
     */
    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply, ResourceType resourceType) {
        updateCounters(this.clock.millis());

        this.pushOutgoingSupply(this.distributorEdges[resourceType.ordinal()], newSupply, resourceType);
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
     * Handle the removal of the connection to the resource flow distributor
     * When this happens, close the SimChainWorkload
     *
     * @param supplierEdge The edge to the resource flow distributor
     */
    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        if (this.distributorEdges[supplierEdge.getSupplierResourceType().ordinal()] == null) {
            return;
        }

        this.stopWorkload();
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        List<FlowEdge> consumerEdges =
                Arrays.stream(this.distributorEdges).filter(Objects::nonNull).toList();
        List<FlowEdge> supplierEdges = (this.workloadEdge != null) ? List.of(this.workloadEdge) : List.of();

        return Map.of(
                FlowEdge.NodeType.CONSUMING, consumerEdges,
                FlowEdge.NodeType.SUPPLYING, supplierEdges);
    }

    public List<ResourceType> getUsedResourceTypes() {
        return this.usedResourceTypes;
    }
}
