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

import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

public class SimTraceWorkload extends SimWorkload implements FlowConsumer {
    private LinkedList<TraceFragment> remainingFragments;
    private LinkedList<TraceFragment> remainingAccelFragments;
    private int fragmentIndex;
    private int accelFragmentIndex;

    private boolean cpuFragmentsComplete = false;
    private boolean accelFragmentsComplete = false;

    private TraceFragment currentFragment;
    private TraceFragment currentAccelFragment;
    private long startOfFragment;

    private FlowEdge machineEdge;
    private FlowEdge accelMachineEdge;

    private double cpuFreqDemand = 0.0; // The Cpu demanded by fragment
    private double cpuFreqSupplied = 0.0; // The Cpu speed supplied
    private double newCpuFreqSupplied = 0.0; // The Cpu speed supplied
    private double remainingWork = 0.0; // The duration of the fragment at the demanded speed

    private double accelFreqDemand = 0.0; // The Cpu demanded by fragment
    private double accelFreqSupplied = 0.0; // The Cpu speed supplied
    private double newAccelFreqSupplied = 0.0; // The Cpu speed supplied
    private double remainingAccelWork = 0.0;

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

    public SimTraceWorkload(FlowSupplier supplier, FlowSupplier accelSupplier, TraceWorkload workload) {
        super(((FlowNode) supplier).getEngine());

        this.snapshot = workload;
        this.checkpointDuration = workload.checkpointDuration();
        this.scalingPolicy = workload.getScalingPolicy();
        this.remainingFragments = new LinkedList<>(workload.getFragments());
        this.remainingAccelFragments = new LinkedList<>(workload.getAccelFragments());
        this.fragmentIndex = 0;
        this.accelFragmentIndex = 0;
        this.taskName = workload.getTaskName();

        this.startOfFragment = this.clock.millis();

        new FlowEdge(this, supplier, FlowEdge.ResourceType.CPU);
        new FlowEdge(this, accelSupplier, FlowEdge.ResourceType.ACCEL);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Fragment related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {
        long passedTime = getPassedTime(now);
        this.startOfFragment = now;

        // The amount of work done since last update
        double finishedWork = this.scalingPolicy.getFinishedWork(this.cpuFreqDemand, this.cpuFreqSupplied, passedTime);
        double finishedAccelWork =
                this.scalingPolicy.getFinishedWork(this.accelFreqDemand, this.accelFreqSupplied, passedTime);

        this.remainingWork -= finishedWork;
        this.remainingAccelWork -= finishedAccelWork;

        // If this.remainingWork <= 0, the fragment has been completed
        if (!this.cpuFragmentsComplete && this.remainingWork <= 0) {
            this.startNextFragment();
            this.invalidate();
        }
        if (!this.accelFragmentsComplete && this.remainingAccelWork <= 0) {
            this.startNextAccelFragment();
            this.invalidate();
        }
        if (this.cpuFragmentsComplete && this.accelFragmentsComplete) {
            this.stopWorkload();
            this.invalidate();
            return Long.MAX_VALUE;
        }

        // Set demand for the cpu and accel to 0 after fragments of that resource are complete
        // For example, if cpu runs longer than gpu, then gpu demand should be 0
        if (this.cpuFragmentsComplete && this.cpuFreqDemand > 0) {
            this.pushOutgoingDemand(this.machineEdge, 0);
        }
        if (this.accelFragmentsComplete && this.accelFreqDemand > 0) {
            this.pushOutgoingDemand(this.accelMachineEdge, 0);
        }

        this.cpuFreqSupplied = this.newCpuFreqSupplied;
        this.accelFreqSupplied = this.newAccelFreqSupplied;

        // The amount of time required to finish the fragment at this speed
        long remainingDuration = this.scalingPolicy.getRemainingDuration(
                this.cpuFreqDemand, this.newCpuFreqSupplied, this.remainingWork);
        long remainingAccelDuration = this.scalingPolicy.getRemainingDuration(
                this.accelFreqDemand, this.newAccelFreqSupplied, this.remainingAccelWork);

        long nextUpdate;
        if (remainingDuration > 0.0 && remainingAccelDuration > 0.0) {
            nextUpdate = min(remainingDuration, remainingAccelDuration);
        } else if (remainingDuration > 0.0) {
            nextUpdate = remainingDuration;
        } else {
            nextUpdate = remainingAccelDuration;
        }

        return now + nextUpdate;
    }

    public TraceFragment getNextFragment() {
        if (this.remainingFragments.isEmpty()) {
            this.cpuFragmentsComplete = true;
            return null;
        }
        this.currentFragment = this.remainingFragments.pop();
        this.fragmentIndex++;

        return this.currentFragment;
    }

    public TraceFragment getNextAccelFragment() {
        if (this.remainingAccelFragments.isEmpty()) {
            this.accelFragmentsComplete = true;
            return null;
        }
        this.currentAccelFragment = this.remainingAccelFragments.pop();
        this.accelFragmentIndex++;

        return this.currentAccelFragment;
    }

    private void startNextFragment() {
        TraceFragment nextFragment = this.getNextFragment();
        if (nextFragment == null) {
            this.remainingWork = Double.NEGATIVE_INFINITY;
            return;
        }
        double demand = nextFragment.cpuUsage();
        this.remainingWork = this.scalingPolicy.getRemainingWork(demand, nextFragment.duration());
        this.pushOutgoingDemand(this.machineEdge, demand);
    }

    private void startNextAccelFragment() {
        TraceFragment nextFragment = this.getNextAccelFragment();
        if (nextFragment == null) {
            this.remainingAccelWork = Double.NEGATIVE_INFINITY;
            return;
        }
        double demand = nextFragment.cpuUsage();
        this.remainingAccelWork = this.scalingPolicy.getRemainingWork(demand, nextFragment.duration());
        this.pushOutgoingDemand(this.accelMachineEdge, demand);
    }

    @Override
    public void stopWorkload() {
        if (this.machineEdge == null && this.accelMachineEdge == null) {
            return;
        }

        // TODO: Maybe move this to the end
        // Currently stopWorkload is called twice
        this.closeNode();

        this.machineEdge = null;
        this.accelMachineEdge = null;
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
        double finishedWork = this.scalingPolicy.getFinishedWork(this.cpuFreqDemand, this.cpuFreqSupplied, passedTime);
        double finishedAccelWork = this.scalingPolicy.getFinishedWork(this.accelFreqDemand, this.accelFreqSupplied, passedTime);

        this.remainingWork -= finishedWork;
        this.remainingAccelWork -= finishedAccelWork;

        // The amount of time required to finish the fragment at this speed
        long remainingTime =
                this.scalingPolicy.getRemainingDuration(this.cpuFreqDemand, this.cpuFreqDemand, this.remainingWork);
        long remainingAccelTime =
            this.scalingPolicy.getRemainingDuration(this.accelFreqDemand, this.accelFreqDemand, this.remainingAccelWork);

        // If this is the end of the Task, don't make a snapshot
        if (remainingTime <= 0 && remainingFragments.isEmpty()) {
            return;
        } else {
            // Create a new fragment based on the current fragment and remaining duration
            TraceFragment newFragment =
                new TraceFragment(remainingTime, currentFragment.cpuUsage(), currentFragment.coreCount());

            // Alter the snapshot by removing finished fragments
            this.snapshot.removeFragments(this.fragmentIndex);
            this.snapshot.addFirst(newFragment);

            this.remainingFragments.addFirst(newFragment);

            // Create and add a fragment for processing the snapshot process
            TraceFragment snapshotFragment = new TraceFragment(
                this.checkpointDuration, this.snapshot.getMaxCpuDemand(), this.snapshot.getMaxCoreCount());
            this.remainingFragments.addFirst(snapshotFragment);

            this.fragmentIndex = -1;
            startNextFragment();
        }

        if (remainingAccelTime <= 0 && remainingAccelFragments.isEmpty()) {
            return;
        } else {
            // Create a new fragment based on the current fragment and remaining duration
            TraceFragment newAccelFragment =
                new TraceFragment(remainingAccelTime, currentAccelFragment.cpuUsage(), currentAccelFragment.coreCount());

            // Alter the snapshot by removing finished fragments
            this.snapshot.removeAccelFragments(this.accelFragmentIndex);
            this.snapshot.addFirstAccel(newAccelFragment);

            this.remainingAccelFragments.addFirst(newAccelFragment);

            // Create and add a fragment for processing the snapshot process
            // Not adding a snapshot cost to accel fragments for now
//            TraceFragment snapshotAccelFragment = new TraceFragment(
//                this.checkpointDuration, this.snapshot.getMaxCpuDemand(), this.snapshot.getMaxCoreCount());
//            this.remainingAccelFragments.addFirst(snapshotAccelFragment);

            this.accelFragmentIndex = -1;
            startNextAccelFragment();
        }

        if (remainingFragments.isEmpty() && remainingAccelFragments.isEmpty()) {
            return;
        }

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
        if (supplierEdge.getResourceType() == FlowEdge.ResourceType.CPU) {
            if (newSupply == this.cpuFreqSupplied) {
                return;
            }

            this.cpuFreqSupplied = this.newCpuFreqSupplied;
            this.newCpuFreqSupplied = newSupply;
        } else if (supplierEdge.getResourceType() == FlowEdge.ResourceType.ACCEL) {
            if (newSupply == this.accelFreqSupplied) {
                return;
            }

            this.accelFreqSupplied = this.newAccelFreqSupplied;
            this.newAccelFreqSupplied = newSupply;
        }

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
        if (supplierEdge.getResourceType() == FlowEdge.ResourceType.CPU) {
            if (newDemand == this.cpuFreqDemand) {
                return;
            }

            this.cpuFreqDemand = newDemand;
            this.machineEdge.pushDemand(newDemand);
        } else if (supplierEdge.getResourceType() == FlowEdge.ResourceType.ACCEL) {
            if (newDemand == this.accelFreqDemand) {
                return;
            }

            this.accelFreqDemand = newDemand;
            this.accelMachineEdge.pushDemand(newDemand);
        }
    }

    /**
     * Add the connection to the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        if (supplierEdge.getResourceType() == FlowEdge.ResourceType.CPU) {
            this.machineEdge = supplierEdge;
        } else if (supplierEdge.getResourceType() == FlowEdge.ResourceType.ACCEL) {
            this.accelMachineEdge = supplierEdge;
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
        if (this.machineEdge == null && this.accelMachineEdge == null) {
            return;
        }

        this.stopWorkload();
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        ArrayList<FlowEdge> consumerEdges = new ArrayList<>();
        if (this.machineEdge != null) consumerEdges.add(this.machineEdge);
        if (this.accelMachineEdge != null) consumerEdges.add(this.accelMachineEdge);
        return Map.of(FlowEdge.NodeType.CONSUMING, consumerEdges);
    }
}
