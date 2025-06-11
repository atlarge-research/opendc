/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.simulator.compute.workload.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.compute.workload.VirtualMachine;
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimTraceWorkload extends SimWorkload implements FlowConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimTraceWorkload.class);
    private LinkedList<TraceFragment> remainingFragments;
    private int fragmentIndex;

    private TraceFragment currentFragment;
    private long startOfFragment;

    private final Map<ResourceType,FlowEdge> machineResourceEdges = new HashMap<>();

    // TODO: Currently GPU memory is not considered and can not be used
    private final ArrayList<ResourceType> usedResourceTypes = new ArrayList<>();
    private final Map<ResourceType, Double> resourcesSupplied = new HashMap<>(); // the currently supplied resources
    private final Map<ResourceType, Double> newResourcesSupply = new HashMap<>(); // The supplied resources with next update
    private final Map<ResourceType, Double> resourcesDemand = new HashMap<>(); // The demands per resource type
    private final Map<ResourceType, Double> remainingWork = new HashMap<>(); // The duration of the fragment at the demanded speeds
    private double totalRemainingWork = 0.0; // The total remaining work of the fragment across all resources, used to determine the end of the fragment
    private final Map<ResourceType, Boolean> workloadFinished = new HashMap<>(); // The workload finished for each resource type

    private final long checkpointDuration;
    private final TraceWorkload snapshot;

    private final ScalingPolicy scalingPolicy;
    private final String taskName;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public long getPassedTime(long now) {
        return now - this.startOfFragment;
    }

    public TraceWorkload getSnapshot() {
        return snapshot;
    }

    @Override
    public long getCheckpointInterval() {
        return 0;
    }

    @Override
    public long getCheckpointDuration() {
        return 0;
    }

    @Override
    public double getCheckpointIntervalScaling() {
        return 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimTraceWorkload(FlowSupplier supplier, TraceWorkload workload) {
        super(((FlowNode) supplier).getEngine());

        this.snapshot = workload;
        this.checkpointDuration = workload.checkpointDuration();
        this.scalingPolicy = workload.getScalingPolicy();
        this.remainingFragments = new LinkedList<>(workload.getFragments());
        this.fragmentIndex = 0;
        this.taskName = workload.getTaskName();

        this.startOfFragment = this.clock.millis();

        new FlowEdge(this, supplier);
        if (supplier instanceof VirtualMachine) {
            // instead iterate over the resources in the fragment as required resources not provided by the VM
            for (ResourceType resourceType : workload.getResourceTypes()) {
                this.usedResourceTypes.add(resourceType);
                this.resourcesSupplied.put(resourceType, 0.0);
                this.newResourcesSupply.put(resourceType, 0.0);
                this.resourcesDemand.put(resourceType, 0.0);
                this.remainingWork.put(resourceType, 0.0);
                this.workloadFinished.put(resourceType, false);
            }
        }
    }

    // Needed if workload not started by VM
    public SimTraceWorkload(List<FlowSupplier> resourceSuppliers, TraceWorkload workload) {
        // same engine for all suppliers
        super(((FlowNode) resourceSuppliers.getFirst()).getEngine());

        this.snapshot = workload;
        this.checkpointDuration = workload.checkpointDuration();
        this.scalingPolicy = workload.getScalingPolicy();
        this.remainingFragments = new LinkedList<>(workload.getFragments());
        this.fragmentIndex = 0;
        this.taskName = workload.getTaskName();

        this.startOfFragment = this.clock.millis();

        for (FlowSupplier supplier : resourceSuppliers) {
            if (supplier.getResourceType() != ResourceType.AUXILIARY){
                new FlowEdge(this, supplier);
                this.usedResourceTypes.add(supplier.getResourceType());
                this.resourcesSupplied.put(supplier.getResourceType(), 0.0);
                this.newResourcesSupply.put(supplier.getResourceType(), 0.0);
                this.resourcesDemand.put(supplier.getResourceType(), 0.0);
                this.remainingWork.put(supplier.getResourceType(), 0.0);
                this.workloadFinished.put(supplier.getResourceType(), false);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Fragment related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {
        long passedTime = getPassedTime(now);
        this.startOfFragment = now;

        for (ResourceType resourceType : this.usedResourceTypes) {
            // The amount of work done since last update
            double finishedWork = this.scalingPolicy.getFinishedWork(this.resourcesDemand.get(resourceType), this.resourcesSupplied.get(resourceType), passedTime);
            this.remainingWork.put(resourceType, this.remainingWork.get(resourceType) - finishedWork);
            this.totalRemainingWork -= finishedWork;
            if (this.remainingWork.get(resourceType) <= 0) {
                this.workloadFinished.put(resourceType, true);
            }
        }

        // If this.totalRemainingWork <= 0, the fragment has been completed across all resources
        if (this.totalRemainingWork <= 0 && !this.workloadFinished.containsValue(false)) {
            this.startNextFragment();

            this.invalidate();
            return Long.MAX_VALUE;
        }

        for (ResourceType resourceType : this.usedResourceTypes) {
            if (this.machineResourceEdges.get(resourceType) != null) {
                this.pushOutgoingDemand(this.machineResourceEdges.get(resourceType), this.resourcesDemand.get(resourceType), resourceType);
            }
        }

        // Update the supplied resources
        for (ResourceType resourceType : this.usedResourceTypes) {
            this.resourcesSupplied.put(resourceType, this.newResourcesSupply.get(resourceType));
        }


        long timeUntilNextUpdate = Long.MIN_VALUE;

        for (ResourceType resourceType : this.usedResourceTypes) {
            // The amount of time required to finish the fragment at this speed
            long remainingDuration = this.scalingPolicy.getRemainingDuration(
                this.resourcesDemand.get(resourceType), this.resourcesSupplied.get(resourceType), this.remainingWork.get(resourceType));

            if (remainingDuration == 0.0) {
                // if resource not initialized, then nothing happens
                this.totalRemainingWork -= this.remainingWork.get(resourceType);
                this.remainingWork.put(resourceType, 0.0);
                this.workloadFinished.put(resourceType, true);
            }

            // The next update should happen when the fastest resource is done, so that it is no longer tracked when unused
            if (remainingDuration > 0 && (timeUntilNextUpdate == Long.MIN_VALUE || remainingDuration < timeUntilNextUpdate)) {
                timeUntilNextUpdate = remainingDuration;
            }
        }

        return timeUntilNextUpdate == Long.MIN_VALUE ? now : now + timeUntilNextUpdate;
    }

    public TraceFragment getNextFragment() {
        if (this.remainingFragments.isEmpty()) {
            return null;
        }
        this.currentFragment = this.remainingFragments.pop();
        this.fragmentIndex++;

        return this.currentFragment;
    }

    private void startNextFragment() {

        TraceFragment nextFragment = this.getNextFragment();
        if (nextFragment == null) {
            this.stopWorkload();
            return;
        }


        // Reset the remaining work for all resources
        this.totalRemainingWork = 0.0;

        // TODO: only acceleration is considered, not memory
        for (ResourceType resourceType : usedResourceTypes) {
            double demand = nextFragment.getResourceUsage(resourceType);

            this.remainingWork.put(resourceType, this.scalingPolicy.getRemainingWork(demand, nextFragment.duration()));
            this.totalRemainingWork += this.remainingWork.get(resourceType);
            this.workloadFinished.put(resourceType, false);

            if (this.machineResourceEdges.get(resourceType) != null){
                this.pushOutgoingDemand(this.machineResourceEdges.get(resourceType), demand, resourceType);
            }
        }

    }

    @Override
    public void stopWorkload() {
        if (areAllEdgesNull()) {
            return;
        }

        // TODO: Maybe move this to the end
        // Currently stopWorkload is called twice
        this.closeNode();

        for (ResourceType resourceType : this.usedResourceTypes) {
            this.machineResourceEdges.put(resourceType, null);
            this.workloadFinished.put(resourceType, true);
        }
        this.remainingFragments = null;
        this.currentFragment = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Checkpoint related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * SimTraceWorkload does not make a checkpoint, checkpointing is handled by SimChainWorkload
     * TODO: Maybe add checkpoint models for SimTraceWorkload
     */
    @Override
    public void createCheckpointModel() {}

    /**
     * Create a new snapshot based on the current status of the workload.
     * @param now Moment on which the snapshot is made in milliseconds
     */

    public void makeSnapshot(long now) {

        // Check if fragments is empty

        // Get remaining time of current fragment
        long passedTime = getPassedTime(now);

        // The amount of work done since last update
        for (ResourceType resourceType : this.usedResourceTypes) {
            double finishedWork = this.scalingPolicy.getFinishedWork(this.resourcesDemand.get(resourceType), this.resourcesSupplied.get(resourceType), passedTime);
            this.remainingWork.put(resourceType, this.remainingWork.get(resourceType) - finishedWork);
            this.totalRemainingWork -= finishedWork;
        }

        long remainingDuration = 0;
        for (ResourceType resourceType : this.usedResourceTypes) {

            // The amount of time required to finish the fragment at this speed
            remainingDuration = Math.max(remainingDuration, this.scalingPolicy.getRemainingDuration(
                this.resourcesDemand.get(resourceType), this.resourcesSupplied.get(resourceType), this.remainingWork.get(resourceType)));
        }

        // If this is the end of the Task, don't make a snapshot
        if (this.currentFragment == null || (remainingDuration <= 0 && remainingFragments.isEmpty())) {
            return;
        }

        // Create a new fragment based on the current fragment and remaining duration
        TraceFragment newFragment =
                new TraceFragment(remainingDuration, currentFragment.cpuUsage(), currentFragment.cpuCoreCount(),
                    currentFragment.gpuUsage(), currentFragment.gpuCoreCount(), currentFragment.gpuMemoryUsage());

        // Alter the snapshot by removing finished fragments
        this.snapshot.removeFragments(this.fragmentIndex);
        this.snapshot.addFirst(newFragment);

        this.remainingFragments.addFirst(newFragment);

        // Create and add a fragment for processing the snapshot process
        TraceFragment snapshotFragment = new TraceFragment(
                this.checkpointDuration, this.snapshot.getMaxCpuDemand(), this.snapshot.getMaxCoreCount(),
                this.snapshot.getMaxGpuDemand(), this.snapshot.getMaxGpuCoreCount(), this.snapshot.getMaxGpuMemoryDemand());
        this.remainingFragments.addFirst(snapshotFragment);

        this.fragmentIndex = -1;
        startNextFragment();

        this.startOfFragment = now;

        this.invalidate();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Handle updates in supply from the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     * @param newSupply The new demand that needs to be sent to the VM
     */
    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply) {
        ResourceType suppliedResourceType = ResourceType.CPU;
        if (this.resourcesSupplied.get(suppliedResourceType) == newSupply){
            return;
        }
        this.resourcesSupplied.put(suppliedResourceType, this.newResourcesSupply.get(suppliedResourceType));
        this.newResourcesSupply.put(suppliedResourceType, newSupply);

        this.invalidate();
    }

    /**
     * Handle updates in supply from the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     * @param newSupply The new demand that needs to be sent to the VM
     */
    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply, ResourceType resourceType) {
        if (this.resourcesSupplied.get(resourceType) == newSupply){
            return;
        }
        this.resourcesSupplied.put(resourceType, this.newResourcesSupply.get(resourceType));
        this.newResourcesSupply.put(resourceType, newSupply);

        this.invalidate();
    }

    /**
     * Push a new demand to the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     * @param newDemand The new demand that needs to be sent to the VM
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {
        ResourceType demandedResourceType = ResourceType.CPU;
        if (this.resourcesDemand.get(demandedResourceType) == newDemand){
            return;
        }

        this.resourcesDemand.put(demandedResourceType, newDemand);
        this.machineResourceEdges.get(demandedResourceType).pushDemand(newDemand);
    }
    /**
     * Push a new demand to the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     * @param newDemand The new demand that needs to be sent to the VM
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand, ResourceType resourceType) {
        if (this.resourcesDemand.get(resourceType) == newDemand){
            return;
        }

        this.resourcesDemand.put(resourceType, newDemand);
        this.machineResourceEdges.get(resourceType).pushDemand(newDemand, false, resourceType);
    }

    /**
     * Add the connection to the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        ResourceType incommingResourceType = supplierEdge.getResourceType();

        if (machineResourceEdges.containsValue(supplierEdge)) {
            return; // Skip if this exact edge is already registered
        }

        this.machineResourceEdges.put(incommingResourceType, supplierEdge);
        if (supplierEdge.getSupplier() instanceof VirtualMachine vm ) {
            for (ResourceType resourceType : vm.getAvailableResources()) {
                if (resourceType == incommingResourceType || resourceType == ResourceType.AUXILIARY) {
                    continue;
                }

                if (!this.machineResourceEdges.containsKey(resourceType)) {
                    new FlowEdge(this, vm, resourceType);
                }
            }
        }
    }

    /**
     * Handle the removal of the connection to the Virtual Machine
     * When the connection to the Virtual Machine is removed, the SimTraceWorkload is removed
     *
     * @param supplierEdge edge to the VM on which this is running
     */
    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        if (areAllEdgesNull()) {
            return;
        }

        this.stopWorkload();
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        Map<FlowEdge.NodeType, List<FlowEdge>> connectedEdges = new HashMap<>();
        for (ResourceType resourceType : ResourceType.values()) {
            if (this.machineResourceEdges.get(resourceType) != null) {
                connectedEdges.put(FlowEdge.NodeType.CONSUMING, List.of(this.machineResourceEdges.get(resourceType)));
            }
        }
        return connectedEdges;
    }

    @Override
    public ResourceType getResourceType() { return ResourceType.AUXILIARY; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Util Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean areAllEdgesNull() {
        for (FlowEdge edge : this.machineResourceEdges.values()) {
            if (edge != null) {
                return false;
            }
        }
        return true;
    }

}
