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
import java.util.Arrays;
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

    // The resources used by this workload and the edges to the components
    private final ArrayList<ResourceType> usedResourceTypes = new ArrayList<>();
    private final FlowEdge[] machineResourceEdges = new FlowEdge[ResourceType.values().length];

    // the currently supplied resources
    private final double[] resourcesSupplied = new double[ResourceType.values().length];

    // The demands per resource type
    private final double[] resourcesDemand = new double[ResourceType.values().length];

    // The remaining work of the current fragment per resource type (depends on the scaling policy)
    private final double[] remainingWork = new double[ResourceType.values().length];

    // The remaining time for each resource type
    private final long[] remainingTime = new long[ResourceType.values().length];

    // Finished resources for the current fragment (Only relevant when multiple resource types are used)
    private final boolean[] resourceFinished = new boolean[ResourceType.values().length];

    // The total remaining work of the fragment across all resources, used to determine the end of the
    // fragment
    private double totalRemainingWork = 0.0;

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

    public long getFailureDelay() {
        return this.snapshot.failureDelay;
    }

    public long getCheckpointDelay() {
        return this.snapshot.checkpointDelay;
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
            this.usedResourceTypes.addAll(Arrays.asList(workload.getResourceTypes()));
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
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Fragment related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if all resources have finished their work for the current fragment
     */
    public boolean getAllResourcesFinished() {
        for (ResourceType resourceType : this.usedResourceTypes) {
            if (!this.resourceFinished[resourceType.ordinal()]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Use the ScalingPolicy, time since the last update, demands and supplied resources
     * to update the remaining work for each resource
     *
     * @param passedTime Time passed since the last update in milliseconds
     */
    private void updateRemainingWork(long passedTime) {
        for (ResourceType resourceType : this.usedResourceTypes) {
            // The amount of work done since last update
            double finishedWork = this.scalingPolicy.getFinishedWork(
                    this.resourcesDemand[resourceType.ordinal()],
                    this.resourcesSupplied[resourceType.ordinal()],
                    passedTime);

            this.remainingWork[resourceType.ordinal()] =
                    Math.max(0, this.remainingWork[resourceType.ordinal()] - finishedWork);

            this.totalRemainingWork -= finishedWork;

            if (this.remainingWork[resourceType.ordinal()] <= 0) {
                this.resourceFinished[resourceType.ordinal()] = true;
            }
        }
    }

    /**
     * Update the remaining time for each resource using the ScalingPolicy, remaining work and supplied resources
     */
    private void updateRemainingTime() {
        for (ResourceType resourceType : this.usedResourceTypes) {
            this.remainingTime[resourceType.ordinal()] = this.scalingPolicy.getRemainingDuration(
                    this.resourcesDemand[resourceType.ordinal()],
                    this.resourcesSupplied[resourceType.ordinal()],
                    this.remainingWork[resourceType.ordinal()]);
        }
    }

    /**
     * Get the next update time based on the remaining time of each resource
     * The next update time is when the fastest resource that is not yet finished will finish
     *
     * @param now Current time in milliseconds
     * @return The next update time in milliseconds
     */
    private long getNextUpdateTime(long now) {
        if (this.getAllResourcesFinished()) {
            return now;
        }

        long timeUntilNextUpdate = Long.MAX_VALUE;

        for (ResourceType resourceType : this.usedResourceTypes) {
            long remainingTime = this.remainingTime[resourceType.ordinal()];

            // The next update should happen when the fastest resource is done
            if (!this.resourceFinished[resourceType.ordinal()] && remainingTime < timeUntilNextUpdate) {
                timeUntilNextUpdate = remainingTime;
            }
        }

        return timeUntilNextUpdate == Long.MAX_VALUE ? Long.MAX_VALUE : now + timeUntilNextUpdate;
    }

    /**
     * Handle an update event for this workload
     * <p>
     * There are three possible scenarios:
     * <ol>
     *     <li>The fragment is completed across all resources: start the next fragment and call onUpdate again</li>
     *     <li>The fragment is not yet completed: push new demands and update remaining time</li>
     *     <li>The workload is completed: stop the workload</li>
     * </ol>
     *
     * @param now The virtual timestamp in milliseconds at which the update is occurring.
     * @return The next update time in milliseconds.
     */
    @Override
    public long onUpdate(long now) {
        long passedTime = getPassedTime(now);
        this.startOfFragment = now;

        this.updateRemainingWork(passedTime);

        // If this.totalRemainingWork <= 0, the fragment has been completed across all resources
        if ((int) this.totalRemainingWork <= 0) {
            this.startNextFragment();

            if (this.nodeState == NodeState.CLOSING || this.nodeState == NodeState.CLOSED) {
                return Long.MAX_VALUE;
            }

            return this.onUpdate(now);
        }

        this.pushNewDemands();
        this.updateRemainingTime();

        return getNextUpdateTime(this.startOfFragment);
    }

    /**
     * Get the next fragment to be executed
     *
     * @return The next TraceFragment or null if there are no more fragments
     */
    public TraceFragment getNextFragment() {
        if (this.remainingFragments.isEmpty()) {
            return null;
        }
        this.currentFragment = this.remainingFragments.pop();
        this.fragmentIndex++;

        return this.currentFragment;
    }

    /**
     * Start the next fragment by resetting the remaining work and pushing new demands to the VM
     *
     * If no more fragments are left, stopWorkload is called.
     */
    private void startNextFragment() {
        TraceFragment nextFragment = this.getNextFragment();
        if (nextFragment == null) {
            this.stopWorkload();
            return;
        }

        // Reset the remaining work for all resources
        this.totalRemainingWork = 0.0;

        for (ResourceType resourceType : usedResourceTypes) {
            double demand = nextFragment.getResourceUsage(resourceType);

            this.remainingWork[resourceType.ordinal()] =
                    this.scalingPolicy.getRemainingWork(demand, nextFragment.duration());
            this.totalRemainingWork += this.remainingWork[resourceType.ordinal()];
            this.resourceFinished[resourceType.ordinal()] = false;

            if (this.machineResourceEdges[resourceType.ordinal()] != null) {
                this.pushOutgoingDemand(this.machineResourceEdges[resourceType.ordinal()], demand, resourceType);
            }
        }
    }

    /**
     * Stop the workload and clean up resources. All connected edges are notified.
     * <p>
     * stopWorkload can be called in two scenarios:
     * <ol>
     *     <li>The workload has completed successfully</li>
     *     <li>The workload is stopped because the task is paused of the host has failed
     * </ol>
     * <p>
     * On failure, the wasted time is calculated for bookkeeping purposes.
     */
    @Override
    public void stopWorkload() {
        // If the workload is stopped due to an error or failure, calculate the wasted time for bookkeeping.
        if (this.totalRemainingWork > 0.0 || !this.remainingFragments.isEmpty()) {
            // Failure

            this.updateRemainingWork(this.clock.millis() - this.startOfFragment);

            for (int i = 0; i < this.fragmentIndex; i++) {
                this.snapshot.failureDelay +=
                        this.snapshot.getFragments().get(i).duration();
            }
            this.snapshot.failureDelay -= (long) this.totalRemainingWork;
        }

        // The workload has already been stopped
        if (areAllEdgesNull()) {
            return;
        }

        // TODO: Maybe move this to the end
        this.closeNode();

        for (ResourceType resourceType : this.usedResourceTypes) {
            this.machineResourceEdges[resourceType.ordinal()] = null;
            this.resourceFinished[resourceType.ordinal()] = true;
        }

        this.remainingFragments = null;
        this.currentFragment = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Checkpoint related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * SimTraceWorkload does not make a checkpoint, checkpointing is handled by SimChainWorkload
     */
    @Override
    public void createCheckpointModel() {}

    /**
     * Create a snapshot of the current state of the workload
     * <p>
     * First calculate the remaining work of the current fragment based on the time passed since the last update.
     * Then, Create a new fragment based on the current fragment and the remaining work.
     * Finally, update the snapshot and the remaining fragments.
     * <p>
     * The snapshot contains all remaining fragments, with the current fragment adjusted to only include the remaining
     * work.
     *
     * @param now Current time in milliseconds
     */
    public void makeSnapshot(long now) {
        long passedTime = getPassedTime(now);
        this.startOfFragment = now;

        this.updateRemainingWork(passedTime);
        this.updateRemainingTime();

        long remainingDuration = Arrays.stream(this.remainingTime).max().orElseThrow();

        // If this is the end of the Task, don't make a snapshot
        if (this.currentFragment == null || (remainingDuration <= 0 && remainingFragments.isEmpty())) {
            return;
        }

        // Remove all fragments up to and including the current fragment from the snapshot
        // These fragments will have to be re-executed after a failure
        this.snapshot.removeFragments(this.fragmentIndex);

        // Create a new fragment with the same resource usage as the current fragment,
        // but with the remaining duration. Put the adjusted fragment at the front of the
        // remaining fragments and snapshot
        if (remainingDuration > 0) {
            TraceFragment adjustedFragment = new TraceFragment(
                    remainingDuration,
                    currentFragment.cpuUsage(),
                    currentFragment.gpuUsage(),
                    currentFragment.gpuMemoryUsage());

            this.snapshot.addFirst(adjustedFragment);
            this.remainingFragments.addFirst(adjustedFragment);
        }

        // Create a fragment for processing the snapshot process and add it to the front of the remaining fragments
        TraceFragment snapshotFragment = new TraceFragment(
                this.checkpointDuration,
                this.snapshot.getMaxCpuDemand(),
                this.snapshot.getMaxGpuDemand(),
                this.snapshot.getMaxGpuMemoryDemand());
        this.remainingFragments.addFirst(snapshotFragment);

        // Add the checkpoint duration for bookkeeping
        this.snapshot.checkpointDelay += this.checkpointDuration;

        this.fragmentIndex = -1;
        startNextFragment();

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

        this.resourcesSupplied[suppliedResourceType.ordinal()] = newSupply;

        // TODO: Change this to just update deadline
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

        this.resourcesSupplied[resourceType.ordinal()] = newSupply;

        long now = this.clock.millis();
        long passedTime = getPassedTime(now);
        this.startOfFragment = now;

        this.updateRemainingWork(passedTime);
        this.updateRemainingTime();

        long next_deadline = this.getNextUpdateTime(now);

        // Remove stage from the timer queue
        this.setDeadline(next_deadline);
        this.engine.scheduleDelayedInContext(this);
    }

    /**
     * Push new demands for all resource types to the Virtual Machine
     */
    private void pushNewDemands() {
        for (ResourceType resourceType : this.usedResourceTypes) {
            if (this.machineResourceEdges[resourceType.ordinal()] != null) {
                this.pushOutgoingDemand(
                        this.machineResourceEdges[resourceType.ordinal()],
                        this.resourcesDemand[resourceType.ordinal()],
                        resourceType);
            }
        }
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

    /**
     * Get all connected edges to this workload
     *
     * @return A map of connected edges categorized by their node type.
     */
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

    private boolean areAllEdgesNull() {
        for (FlowEdge edge : this.machineResourceEdges) {
            if (edge != null) {
                return false;
            }
        }
        return true;
    }
}
