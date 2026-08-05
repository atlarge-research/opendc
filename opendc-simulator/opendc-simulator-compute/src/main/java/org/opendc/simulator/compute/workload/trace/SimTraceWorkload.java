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

import java.util.Arrays;
import java.util.HashMap;
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

    // Cached so ordinal -> ResourceType lookups don't repeatedly clone the array via ResourceType.values()
    private static final ResourceType[] RESOURCE_TYPES = ResourceType.values();

    private TraceWorkload workload;
    private int fragmentIndex;
    private TraceFragment currentFragment;
    private long startOfFragment;

    // The ordinals of the ResourceTypes actually used by this workload's fragments, and the edges to the components
    private final int[] usedResourceTypesOrdinals;
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
    private boolean makingSnapshot = false;

    private final ScalingPolicy scalingPolicy;
    private final int taskId;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public long getPassedTime(long now) {
        return now - this.startOfFragment;
    }

    public TraceWorkload getSnapshot() {
        return this.workload;
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
        return this.workload.failureDelay;
    }

    public long getCheckpointDelay() {
        return this.workload.checkpointDelay;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimTraceWorkload(FlowSupplier supplier, TraceWorkload workload) {
        super(((FlowNode) supplier).getEngine());

        this.workload = workload;
        this.checkpointDuration = workload.checkpointDuration();
        this.scalingPolicy = workload.getScalingPolicy();
        this.taskId = workload.getTaskId();

        new FlowEdge(this, supplier);

        // The resources required are those used by the fragments, not those provided by the VM
        this.usedResourceTypesOrdinals = trueIndices(workload.getUsedResourceTypes());

        this.startOfFragment = this.clock.millis();
        this.fragmentIndex = workload.getStartingIndex();
        this.currentFragment = workload.getFragment(this.fragmentIndex);
        this.startFragment(this.currentFragment);
    }

    // Needed if workload not started by VM
    public SimTraceWorkload(List<FlowSupplier> resourceSuppliers, TraceWorkload workload) {
        // same engine for all suppliers
        super(((FlowNode) resourceSuppliers.getFirst()).getEngine());

        throw new UnsupportedOperationException(
                "It is currently not possible to run a Task directly on a machine without going through a Virtual Machine");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Fragment related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if all resources have finished their work for the current fragment
     */
    public boolean getAllResourcesFinished() {
        for (int ordinal : this.usedResourceTypesOrdinals) {
            if (!this.resourceFinished[ordinal]) {
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
        for (int ordinal : this.usedResourceTypesOrdinals) {
            // The amount of work done since last update
            double finishedWork = this.scalingPolicy.getFinishedWork(
                    this.resourcesDemand[ordinal], this.resourcesSupplied[ordinal], passedTime);

            this.remainingWork[ordinal] = Math.max(0, this.remainingWork[ordinal] - finishedWork);

            this.totalRemainingWork -= finishedWork;

            if (this.remainingWork[ordinal] <= 0) {
                this.resourceFinished[ordinal] = true;
            }
        }
    }

    /**
     * Update the remaining time for each resource using the ScalingPolicy, remaining work and supplied resources
     */
    private void updateRemainingTime() {
        for (int ordinal : this.usedResourceTypesOrdinals) {
            this.remainingTime[ordinal] = this.scalingPolicy.getRemainingDuration(
                    this.resourcesDemand[ordinal], this.resourcesSupplied[ordinal], this.remainingWork[ordinal]);
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

        for (int ordinal : this.usedResourceTypesOrdinals) {
            long remainingTime = this.remainingTime[ordinal];

            // The next update should happen when the fastest resource is done
            if (!this.resourceFinished[ordinal] && remainingTime < timeUntilNextUpdate) {
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
        this.updateRemainingTime();

        // If this.totalRemainingWork <= 0, the fragment has been completed across all resources
        if ((int) this.totalRemainingWork <= 0) {
            this.startNextFragment();

            if (this.nodeState == NodeState.CLOSING || this.nodeState == NodeState.CLOSED) {
                return Long.MAX_VALUE;
            }

            return this.onUpdate(now);
        }

        return getNextUpdateTime(this.startOfFragment);
    }

    public void startFragment(TraceFragment fragment) {
        this.currentFragment = fragment;

        // Reset the remaining work for all resources
        this.totalRemainingWork = 0.0;

        // Set the remaining Work of each resource based on the given Fragment.
        for (int ordinal : this.usedResourceTypesOrdinals) {
            ResourceType resourceType = RESOURCE_TYPES[ordinal];
            double demand = fragment.getResourceUsage(resourceType);

            this.remainingWork[ordinal] = this.scalingPolicy.getRemainingWork(demand, fragment.duration());
            this.totalRemainingWork += this.remainingWork[ordinal];
            this.resourceFinished[ordinal] = false;

            if (this.machineResourceEdges[ordinal] != null) {
                this.pushOutgoingDemand(this.machineResourceEdges[ordinal], demand, resourceType);
            }
        }

        this.updateRemainingTime();
    }

    /**
     * Get the next fragment to be executed
     *
     * @return The next TraceFragment or null if there are no more fragments
     */
    public TraceFragment getNextFragment() {

        // If the previous fragment was making a snapshot, set makingSnapshot to false.
        // If the previous fragment was a normal fragment, increment fragment index.
        if (this.makingSnapshot) {
            this.makingSnapshot = false;
        } else {
            this.fragmentIndex++;
        }

        if (!this.workload.hasFragmentAt(this.fragmentIndex)) {
            return null;
        }

        return this.workload.getFragment(this.fragmentIndex);
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

        this.startFragment(nextFragment);
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
        if (this.totalRemainingWork > 0.0 || this.workload.hasFragmentAt(this.fragmentIndex + 1)) {
            // Failure

            this.updateRemainingWork(this.clock.millis() - this.startOfFragment);

            for (int i = 0; i < this.fragmentIndex; i++) {
                this.workload.failureDelay += this.workload.getFragment(i).duration();
            }
            this.workload.failureDelay -= (long) this.totalRemainingWork;
        }

        // The workload has already been stopped
        if (areAllEdgesNull()) {
            return;
        }

        // TODO: Maybe move this to the end
        this.closeNode();

        for (int ordinal : this.usedResourceTypesOrdinals) {
            this.machineResourceEdges[ordinal] = null;
            this.resourceFinished[ordinal] = true;
        }

        this.workload = null;
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

        // Update the remaining time and work
        long passedTime = getPassedTime(now);
        this.startOfFragment = now;

        this.updateRemainingWork(passedTime);
        this.updateRemainingTime();

        // TODO: Does this line still do anything?
        long remainingDuration = Arrays.stream(this.remainingTime).max().orElseThrow();

        // If this is the end of the Task, don't make a snapshot
        if (this.currentFragment == null
                || (remainingDuration <= 0 && !workload.hasFragmentAt(this.fragmentIndex + 1))) {
            return;
        }

        // Update the starting index of the workload so it will not rerun the whole workload after a failure
        this.workload.setStartingIndex(this.fragmentIndex);

        // Update the duration of the current fragment so only the remaining time has to be run.
        this.workload.updateFragment(
                this.fragmentIndex,
                remainingDuration,
                currentFragment.cpuUsage(),
                currentFragment.gpuUsage(),
                currentFragment.gpuMemoryUsage());

        // Add a checkpointing fragment
        TraceFragment snapshotFragment = new TraceFragment(
                this.checkpointDuration,
                this.workload.getMaxCpuDemand(),
                this.workload.getMaxGpuDemand(),
                this.workload.getMaxGpuMemoryDemand());

        // Add delay for bookkeeping
        this.workload.checkpointDelay += this.checkpointDuration;

        this.makingSnapshot = true;
        this.startFragment(snapshotFragment);

        // Update the index and start the checkpointing fragment
        // TODO: see if this is still needed
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
        if (!this.usesResourceType(resourceType)) {
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
     * Determine whether the given resource type is used by this workload's fragments.
     */
    private boolean usesResourceType(ResourceType resourceType) {
        for (int ordinal : this.usedResourceTypesOrdinals) {
            if (ordinal == resourceType.ordinal()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Push a new CPU demand to the Virtual Machine
     * This function does not specify the type of resources demanded and thus defaults to CPU
     * TODO: Maybe delete this function because it creates confusion. It also does not have better performance
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

    /**
     * Get the indices at which the given array is {@code true}.
     *
     * @param flags An array indexed by {@link ResourceType#ordinal()}.
     * @return The ordinals for which {@code flags} is {@code true}.
     */
    private static int[] trueIndices(boolean[] flags) {
        int count = 0;
        for (boolean flag : flags) {
            if (flag) {
                count++;
            }
        }

        int[] indices = new int[count];
        int idx = 0;
        for (int i = 0; i < flags.length; i++) {
            if (flags[i]) {
                indices[idx++] = i;
            }
        }
        return indices;
    }
}
