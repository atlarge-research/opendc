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

    private final FlowEdge[] machineResourceEdges = new FlowEdge[ResourceType.values().length];

    // TODO: Currently GPU memory is not considered and can not be used
    private final ArrayList<ResourceType> usedResourceTypes = new ArrayList<>();

    private final double[] resourcesSupplied =
            new double[ResourceType.values().length]; // the currently supplied resources
    private final double[] newResourcesSupply =
            new double[ResourceType.values().length]; // The supplied resources with next update
    private final double[] resourcesDemand = new double[ResourceType.values().length]; // The demands per resource type
    private final double[] remainingWork =
            new double[ResourceType.values().length]; // The duration of the fragment at the demanded speeds
    private double totalRemainingWork =
            0.0; // The total remaining work of the fragment across all resources, used to determine the end of the
    // fragment
    private final boolean[] workloadFinished =
            new boolean[ResourceType.values().length]; // The workload finished for each resource type

    private final long checkpointDuration;
    private final TraceWorkload snapshot;

    private final ScalingPolicy scalingPolicy;
    private final int taskId;

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
        this.taskId = workload.getTaskId();

        this.startOfFragment = this.clock.millis();

        new FlowEdge(this, supplier);
        if (supplier instanceof VirtualMachine) {
            // instead iterate over the resources in the fragment as required resources not provided by the VM
            for (ResourceType resourceType : workload.getResourceTypes()) {
                this.usedResourceTypes.add(resourceType);

                //                this.resourcesSupplied.put(resourceType, 0.0);
                //                this.newResourcesSupply.put(resourceType, 0.0);
                //                this.resourcesDemand.put(resourceType, 0.0);
                //                this.remainingWork.put(resourceType, 0.0);
                //                this.workloadFinished.put(resourceType, false);
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
        this.taskId = workload.getTaskId();

        this.startOfFragment = this.clock.millis();

        for (FlowSupplier supplier : resourceSuppliers) {
            if (supplier.getSupplierResourceType() != ResourceType.AUXILIARY) {
                new FlowEdge(this, supplier);
                this.usedResourceTypes.add(supplier.getSupplierResourceType());
                //                this.resourcesSupplied.put(supplier.getSupplierResourceType(), 0.0);
                //                this.newResourcesSupply.put(supplier.getSupplierResourceType(), 0.0);
                //                this.resourcesDemand.put(supplier.getSupplierResourceType(), 0.0);
                //                this.remainingWork.put(supplier.getSupplierResourceType(), 0.0);
                //                this.workloadFinished.put(supplier.getSupplierResourceType(), false);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Fragment related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isWorkloadFinished() {
        for (ResourceType resourceType : this.usedResourceTypes) {
            if (!this.workloadFinished[resourceType.ordinal()]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public long onUpdate(long now) {
        long passedTime = getPassedTime(now);
        this.startOfFragment = now;

        for (ResourceType resourceType : this.usedResourceTypes) {
            // The amount of work done since last update
            double finishedWork = this.scalingPolicy.getFinishedWork(
                    this.resourcesDemand[resourceType.ordinal()],
                    this.resourcesSupplied[resourceType.ordinal()],
                    passedTime);

            // TODO: maybe remove Math.max, as as we are already checking for <= 0
            this.remainingWork[resourceType.ordinal()] =
                    Math.max(0, this.remainingWork[resourceType.ordinal()] - finishedWork);
            this.totalRemainingWork -= finishedWork;
            if (this.remainingWork[resourceType.ordinal()] <= 0) {
                this.workloadFinished[resourceType.ordinal()] = true;
            }
        }

        // If this.totalRemainingWork <= 0, the fragment has been completed across all resources
        if ((int) this.totalRemainingWork <= 0 && this.isWorkloadFinished()) {
            this.startNextFragment();

            this.invalidate();
            return Long.MAX_VALUE;
        }

        for (ResourceType resourceType : this.usedResourceTypes) {
            if (this.machineResourceEdges[resourceType.ordinal()] != null) {
                this.pushOutgoingDemand(
                        this.machineResourceEdges[resourceType.ordinal()],
                        this.resourcesDemand[resourceType.ordinal()],
                        resourceType);
            }
        }

        // Update the supplied resources
        for (ResourceType resourceType : this.usedResourceTypes) {
            this.resourcesSupplied[resourceType.ordinal()] = this.newResourcesSupply[resourceType.ordinal()];
        }

        long timeUntilNextUpdate = Long.MIN_VALUE;

        for (ResourceType resourceType : this.usedResourceTypes) {
            // The amount of time required to finish the fragment at this speed
            long remainingDuration = this.scalingPolicy.getRemainingDuration(
                    this.resourcesDemand[resourceType.ordinal()],
                    this.resourcesSupplied[resourceType.ordinal()],
                    this.remainingWork[resourceType.ordinal()]);

            if ((int) remainingDuration == 0) {
                // if resource not initialized, then nothing happens
                if (this.remainingWork[resourceType.ordinal()] >= 0.0) {
                    this.totalRemainingWork -= this.remainingWork[resourceType.ordinal()];
                }
                this.remainingWork[resourceType.ordinal()] = 0.0;
                this.workloadFinished[resourceType.ordinal()] = true;
            }

            // The next update should happen when the fastest resource is done, so that it is no longer tracked when
            // unused
            if (remainingDuration > 0
                    && (timeUntilNextUpdate == Long.MIN_VALUE || remainingDuration < timeUntilNextUpdate)) {
                timeUntilNextUpdate = remainingDuration;
            }
        }

        long nextUpdate = timeUntilNextUpdate == Long.MAX_VALUE ? Long.MAX_VALUE : now + timeUntilNextUpdate;

        // if for all resources the remaining work is 0, then invalidate the workload, to reschedule the next fragment
        if (nextUpdate == now + Long.MIN_VALUE) {
            this.invalidate();
            return Long.MAX_VALUE;
        }
        return nextUpdate;
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

            this.remainingWork[resourceType.ordinal()] =
                    this.scalingPolicy.getRemainingWork(demand, nextFragment.duration());
            this.totalRemainingWork += this.remainingWork[resourceType.ordinal()];
            this.workloadFinished[resourceType.ordinal()] = false;

            if (this.machineResourceEdges[resourceType.ordinal()] != null) {
                this.pushOutgoingDemand(this.machineResourceEdges[resourceType.ordinal()], demand, resourceType);
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
            this.machineResourceEdges[resourceType.ordinal()] = null;
            this.workloadFinished[resourceType.ordinal()] = true;
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
            double finishedWork = this.scalingPolicy.getFinishedWork(
                    this.resourcesDemand[resourceType.ordinal()],
                    this.resourcesSupplied[resourceType.ordinal()],
                    passedTime);
            this.remainingWork[resourceType.ordinal()] = this.remainingWork[resourceType.ordinal()] - finishedWork;
            this.totalRemainingWork -= finishedWork;
        }

        long remainingDuration = 0;
        for (ResourceType resourceType : this.usedResourceTypes) {

            // The amount of time required to finish the fragment at this speed
            remainingDuration = Math.max(
                    remainingDuration,
                    this.scalingPolicy.getRemainingDuration(
                            this.resourcesDemand[resourceType.ordinal()],
                            this.resourcesSupplied[resourceType.ordinal()],
                            this.remainingWork[resourceType.ordinal()]));
        }

        // If this is the end of the Task, don't make a snapshot
        if (this.currentFragment == null || (remainingDuration <= 0 && remainingFragments.isEmpty())) {
            return;
        }

        // Create a new fragment based on the current fragment and remaining duration
        TraceFragment newFragment = new TraceFragment(
                remainingDuration,
                currentFragment.cpuUsage(),
                currentFragment.gpuUsage(),
                currentFragment.gpuMemoryUsage());

        // Alter the snapshot by removing finished fragments
        this.snapshot.removeFragments(this.fragmentIndex);
        this.snapshot.addFirst(newFragment);

        this.remainingFragments.addFirst(newFragment);

        // Create and add a fragment for processing the snapshot process
        TraceFragment snapshotFragment = new TraceFragment(
                this.checkpointDuration,
                this.snapshot.getMaxCpuDemand(),
                this.snapshot.getMaxGpuDemand(),
                this.snapshot.getMaxGpuMemoryDemand());
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
        if (this.resourcesSupplied[suppliedResourceType.ordinal()] == newSupply) {
            return;
        }
        this.resourcesSupplied[suppliedResourceType.ordinal()] =
                this.newResourcesSupply[suppliedResourceType.ordinal()];
        this.newResourcesSupply[suppliedResourceType.ordinal()] = newSupply;

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
        // for cases where equal share or fixed share is used and the resource is provided despite not being used
        if (!this.usedResourceTypes.contains(resourceType)) {
            return;
        }
        if (this.resourcesSupplied[resourceType.ordinal()] == newSupply) {
            return;
        }
        this.resourcesSupplied[resourceType.ordinal()] = this.newResourcesSupply[resourceType.ordinal()];
        this.newResourcesSupply[resourceType.ordinal()] = newSupply;

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
        if (this.resourcesDemand[demandedResourceType.ordinal()] == newDemand) {
            return;
        }

        this.resourcesDemand[demandedResourceType.ordinal()] = newDemand;
        this.machineResourceEdges[demandedResourceType.ordinal()].pushDemand(newDemand);
    }
    /**
     * Push a new demand to the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     * @param newDemand The new demand that needs to be sent to the VM
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand, ResourceType resourceType) {
        if (this.resourcesDemand[resourceType.ordinal()] == newDemand) {
            return;
        }

        this.resourcesDemand[resourceType.ordinal()] = newDemand;
        this.machineResourceEdges[resourceType.ordinal()].pushDemand(newDemand, false, resourceType);
    }

    /**
     * Add the connection to the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        ResourceType incommingResourceType = supplierEdge.getResourceType();

        if (machineResourceEdges[incommingResourceType.ordinal()] == (supplierEdge)) {
            return; // Skip if this exact edge is already registered
        }

        this.machineResourceEdges[incommingResourceType.ordinal()] = supplierEdge;
        if (supplierEdge.getSupplier() instanceof VirtualMachine vm) {
            for (ResourceType resourceType : vm.getUsedResourceTypes()) {
                if (resourceType == incommingResourceType || resourceType == ResourceType.AUXILIARY) {
                    continue;
                }

                if (this.machineResourceEdges[resourceType.ordinal()] == null) {
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
            if (this.machineResourceEdges[resourceType.ordinal()] != null) {
                connectedEdges.put(
                        FlowEdge.NodeType.CONSUMING, List.of(this.machineResourceEdges[resourceType.ordinal()]));
            }
        }
        return connectedEdges;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Util Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean areAllEdgesNull() {
        for (FlowEdge edge : this.machineResourceEdges) {
            if (edge != null) {
                return false;
            }
        }
        return true;
    }
}
