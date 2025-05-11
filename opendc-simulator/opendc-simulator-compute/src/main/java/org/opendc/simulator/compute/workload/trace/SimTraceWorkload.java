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

import java.io.Console;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.compute.workload.VirtualMachine;
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

public class SimTraceWorkload extends SimWorkload implements FlowConsumer {
    private LinkedList<TraceFragment> remainingFragments;
    private int fragmentIndex;

    private TraceFragment currentFragment;
    private long startOfFragment;

    private FlowEdge machineEdge;
    private final FlowEdge[] machineResourceEdges = new FlowEdge[ResourceType.values().length];


    // TODO: limit to the number of actually involved resources
    // TODO: Currently GPU memory is not considered and can not be used
    private final int resourceEnmumCount = ResourceType.values().length;
    private final ArrayList<ResourceType> usedResourceTypes = new ArrayList<>();
    private final long checkpointDuration;
    private final double[] resourcesSupplied = new double[ResourceType.values().length]; // the currently supplied resources
    private final double[] newResourcesSupply = new double[ResourceType.values().length]; // The supplied resources with next update
    private final double[] resourcesDemand = new double[ResourceType.values().length]; // The demands per resource type
    private final double[] remainingWork = new double[ResourceType.values().length]; // The duration of the fragment at the demanded speeds
    private double totalRemainingWork = 0.0; // The total remaining work of the fragment across all resources, used to determine the end of the fragment

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
            usedResourceTypes.addAll(((VirtualMachine) supplier).getAvailableResources());
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
                new FlowEdge(this, supplier, supplier.getResourceType());
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

        for (int resourceTypeIdx = 0; resourceTypeIdx < resourceEnmumCount; resourceTypeIdx++) {
            if (ResourceType.values()[resourceTypeIdx] == ResourceType.AUXILIARY) {
                continue;
            }
            // The amount of work done since last update
            // TODO: it is the same for every resource.
            double finishedWork = this.scalingPolicy.getFinishedWork(this.resourcesDemand[resourceTypeIdx], this.resourcesSupplied[resourceTypeIdx], passedTime);
            this.remainingWork[resourceTypeIdx] -= finishedWork;
            this.totalRemainingWork -= finishedWork;
        }

        // If this.totalRemainingWork <= 0, the fragment has been completed across all resources
        if (this.totalRemainingWork <= 0) {
            this.startNextFragment();

            this.invalidate();
            return Long.MAX_VALUE;
        }

        for (int resourceTypeIdx = 0; resourceTypeIdx < resourceEnmumCount; resourceTypeIdx++) {
            if (this.machineResourceEdges[resourceTypeIdx] != null) {
                this.pushOutgoingDemand(this.machineResourceEdges[resourceTypeIdx], this.resourcesDemand[resourceTypeIdx]);
            }
        }

        // Update the supplied resources
        System.arraycopy(this.newResourcesSupply, 0, this.resourcesSupplied, 0, ResourceType.values().length);


        long timeUntilNextUpdate = Long.MAX_VALUE;
        for (int resourceTypeIdx = 0; resourceTypeIdx < resourceEnmumCount; resourceTypeIdx++) {
            if (ResourceType.values()[resourceTypeIdx] == ResourceType.AUXILIARY) {
                continue;
            }

            // The amount of time required to finish the fragment at this speed
            long remainingDuration = this.scalingPolicy.getRemainingDuration(
                this.resourcesDemand[resourceTypeIdx], this.resourcesSupplied[resourceTypeIdx], this.remainingWork[resourceTypeIdx]); // for multi resource this needs to be the lowest, to get new update

            if (remainingDuration == 0.0) {
                // if resource not initialized, then nothing happens
                this.totalRemainingWork -= this.remainingWork[resourceTypeIdx];

                this.remainingWork[resourceTypeIdx] = 0.0;
            }

            // The next update should happen when the fastest resource is done, so that it is no longer tracked when unused
            // TODO: what if fragment is actually finished? Use supplied resources as indicator?
            if (remainingDuration < timeUntilNextUpdate && remainingDuration > 0) {
                timeUntilNextUpdate = remainingDuration;
            }
        }

        return timeUntilNextUpdate == Long.MAX_VALUE ? now : now + timeUntilNextUpdate;
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

        // TODO: FIX this, only acceleration is considered, not memory
        for (int resourceTypeIdx = 0; resourceTypeIdx < resourceEnmumCount; resourceTypeIdx++) {
            if (ResourceType.values()[resourceTypeIdx] == ResourceType.AUXILIARY) {
                continue;
            }
            double demand = nextFragment.getResourceUsage(ResourceType.values()[resourceTypeIdx]);
            // TODO: not correct for multiple resources, because it is the same for all resources, if only duration is used
            this.remainingWork[resourceTypeIdx] = this.scalingPolicy.getRemainingWork(demand, nextFragment.duration());
            this.totalRemainingWork += this.remainingWork[resourceTypeIdx];
            if (this.machineResourceEdges[resourceTypeIdx] != null){
                this.pushOutgoingDemand(this.machineResourceEdges[resourceTypeIdx], demand);
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

        for (int resourceTypeIdx = 0; resourceTypeIdx < resourceEnmumCount; resourceTypeIdx++) {
            this.machineResourceEdges[resourceTypeIdx] = null;
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
        for (int resourceTypeIdx = 0; resourceTypeIdx < resourceEnmumCount; resourceTypeIdx++) {
            if (ResourceType.values()[resourceTypeIdx] == ResourceType.AUXILIARY) {
                continue;
            }
            double finishedWork = this.scalingPolicy.getFinishedWork(this.resourcesDemand[resourceTypeIdx], this.resourcesSupplied[resourceTypeIdx], passedTime);
            this.remainingWork[resourceTypeIdx] -= finishedWork;
            this.totalRemainingWork -= finishedWork;
        }

        long remainingDuration = 0;
        for (int resourceTypeIdx = 0; resourceTypeIdx < resourcesSupplied.length; resourceTypeIdx++) {
            if (ResourceType.values()[resourceTypeIdx] == ResourceType.AUXILIARY) {
                continue;
            }

            // The amount of time required to finish the fragment at this speed
            // probably wrong
            remainingDuration = Math.max(remainingDuration, this.scalingPolicy.getRemainingDuration(
                this.resourcesDemand[resourceTypeIdx], this.resourcesSupplied[resourceTypeIdx], this.remainingWork[resourceTypeIdx]));
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
        // TODO: Check if correct
        if (resourcesSupplied[supplierEdge.getResourceType().ordinal()] == newSupply){
            return;
        }
        this.resourcesSupplied[supplierEdge.getResourceType().ordinal()] =  this.newResourcesSupply[supplierEdge.getResourceType().ordinal()];
        this.newResourcesSupply[supplierEdge.getResourceType().ordinal()] = newSupply;

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
        if (this.resourcesDemand[supplierEdge.getResourceType().ordinal()] == newDemand){
            return;
        }

        this.resourcesDemand[supplierEdge.getResourceType().ordinal()] = newDemand;
        this.machineResourceEdges[supplierEdge.getResourceType().ordinal()].pushDemand(newDemand);
    }

    /**
     * Add the connection to the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.machineResourceEdges[supplierEdge.getResourceType().ordinal()] = supplierEdge;
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
                connectedEdges.put(FlowEdge.NodeType.CONSUMING, List.of(this.machineResourceEdges[resourceType.ordinal()]));
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
