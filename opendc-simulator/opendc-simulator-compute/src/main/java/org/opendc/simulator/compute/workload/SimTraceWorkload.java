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

package org.opendc.simulator.compute.workload;

import java.util.LinkedList;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowGraph;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

public class SimTraceWorkload extends SimWorkload implements FlowConsumer {
    private LinkedList<TraceFragment> remainingFragments;
    private int fragmentIndex;

    private TraceFragment currentFragment;
    private long startOfFragment;

    private FlowEdge machineEdge;
    private double currentDemand;
    private double currentSupply;

    private long checkpointDuration;

    private TraceWorkload snapshot;

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
    long getCheckpointInterval() {
        return 0;
    }

    @Override
    long getCheckpointDuration() {
        return 0;
    }

    @Override
    double getCheckpointIntervalScaling() {
        return 0;
    }

    public TraceFragment getNextFragment() {
        this.currentFragment = this.remainingFragments.pop();
        this.fragmentIndex++;

        return this.currentFragment;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimTraceWorkload(FlowSupplier supplier, TraceWorkload workload, long now) {
        super(((FlowNode) supplier).getGraph());

        this.snapshot = workload;
        this.checkpointDuration = workload.getCheckpointDuration();
        this.remainingFragments = new LinkedList<>(workload.getFragments());
        this.fragmentIndex = 0;

        final FlowGraph graph = ((FlowNode) supplier).getGraph();
        graph.addEdge(this, supplier);

        this.currentFragment = this.getNextFragment();
        pushOutgoingDemand(machineEdge, this.currentFragment.cpuUsage());
        this.startOfFragment = now;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Fragment related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {
        long passedTime = getPassedTime(now);
        long duration = this.currentFragment.duration();

        // The current Fragment has not yet been finished, continue
        if (passedTime < duration) {
            return now + (duration - passedTime);
        }

        // Loop through fragments until the passed time is filled.
        // We need a while loop to account for skipping of fragments.
        while (passedTime >= duration) {
            if (this.remainingFragments.isEmpty()) {
                this.stopWorkload();
                return Long.MAX_VALUE;
            }

            passedTime = passedTime - duration;

            // get next Fragment
            currentFragment = this.getNextFragment();
            duration = currentFragment.duration();
        }

        // start new fragment
        this.startOfFragment = now - passedTime;

        // Change the cpu Usage to the new Fragment
        pushOutgoingDemand(machineEdge, this.currentFragment.cpuUsage());

        // Return the time when the current fragment will complete
        return this.startOfFragment + duration;
    }

    @Override
    public void stopWorkload() {
        if (this.machineEdge == null) {
            return;
        }

        this.closeNode();

        this.machineEdge = null;
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
    void createCheckpointModel() {}

    /**
     * Create a new snapshot based on the current status of the workload.
     * @param now Moment on which the snapshot is made in milliseconds
     */
    public void makeSnapshot(long now) {

        // Check if fragments is empty

        // Get remaining time of current fragment
        long passedTime = getPassedTime(now);
        long remainingTime = currentFragment.duration() - passedTime;

        // If this is the end of the Task, don't make a snapshot
        if (remainingTime <= 0 && remainingFragments.isEmpty()) {
            return;
        }

        // Create a new fragment based on the current fragment and remaining duration
        TraceFragment newFragment =
                new TraceFragment(remainingTime, currentFragment.cpuUsage(), currentFragment.coreCount());

        // Alter the snapshot by removing finished fragments
        this.snapshot.removeFragments(this.fragmentIndex);
        this.snapshot.addFirst(newFragment);

        this.remainingFragments.addFirst(newFragment);

        // Create and add a fragment for processing the snapshot process
        // TODO: improve the implementation of cpuUsage and coreCount
        TraceFragment snapshotFragment = new TraceFragment(this.checkpointDuration, 123456, 1);
        this.remainingFragments.addFirst(snapshotFragment);

        this.fragmentIndex = -1;
        this.currentFragment = getNextFragment();
        pushOutgoingDemand(this.machineEdge, this.currentFragment.cpuUsage());
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
        if (newSupply == this.currentSupply) {
            return;
        }

        this.currentSupply = newSupply;
    }

    /**
     * Push a new demand to the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     * @param newDemand The new demand that needs to be sent to the VM
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {
        if (newDemand == this.currentDemand) {
            return;
        }

        this.currentDemand = newDemand;
        this.machineEdge.pushDemand(newDemand);
    }

    /**
     * Add the connection to the Virtual Machine
     *
     * @param supplierEdge edge to the VM on which this is running
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.machineEdge = supplierEdge;
    }

    /**
     * Handle the removal of the connection to the Virtual Machine
     * When the connection to the Virtual Machine is removed, the SimTraceWorkload is removed
     *
     * @param supplierEdge edge to the VM on which this is running
     */
    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        if (this.machineEdge == null) {
            return;
        }
        this.stopWorkload();
    }
}
