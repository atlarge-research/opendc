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

package org.opendc.simulator.compute.machine;

import java.util.function.Consumer;
import org.opendc.simulator.compute.cpu.SimCpu;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.compute.workload.Workload;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowGraph;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

/*
  A virtual Machine created to run a single workload
*/
public class VirtualMachine extends FlowNode implements FlowConsumer, FlowSupplier {
    private final SimMachine machine;

    private SimWorkload activeWorkload;

    private long lastUpdate;
    private final double d;

    private FlowEdge cpuEdge; // The edge to the cpu
    private FlowEdge workloadEdge; // The edge to the workload

    private double cpuDemand;
    private double cpuSupply;
    private double cpuCapacity;

    private PerformanceCounters performanceCounters = new PerformanceCounters();

    private Consumer<Exception> completion;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public PerformanceCounters getPerformanceCounters() {
        return performanceCounters;
    }

    public SimWorkload getActiveWorkload() {
        return activeWorkload;
    }

    public double getDemand() {
        return cpuDemand;
    }

    public void setDemand(double demand) {
        this.cpuDemand = demand;
    }

    public double getCpuCapacity() {
        return cpuCapacity;
    }

    public void setCpuCapacity(double cpuCapacity) {
        this.cpuCapacity = cpuCapacity;
    }

    public FlowGraph getGraph() {
        return this.parentGraph;
    }

    public SimCpu getCpu() {
        return machine.getCpu();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public VirtualMachine(SimMachine machine) {
        super(machine.getGraph());
        this.machine = machine;
        this.clock = this.machine.getClock();

        this.parentGraph = machine.getGraph();
        this.parentGraph.addEdge(this, this.machine.getCpuMux());

        this.lastUpdate = clock.millis();
        this.lastUpdate = clock.millis();

        this.d = 1 / machine.getCpu().getFrequency();
    }

    public void shutdown() {
        this.shutdown(null);
    }

    public void shutdown(Exception cause) {
        if (this.nodeState == NodeState.CLOSED) {
            return;
        }

        super.closeNode();

        this.activeWorkload = null;
        this.performanceCounters = null;

        this.completion.accept(cause);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Workload related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void startWorkload(Workload workload, Consumer<Exception> completion) {
        this.completion = completion;
        this.activeWorkload = workload.startWorkload(this, this.clock.millis());
    }

    public void updateCounters(long now) {
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;
        long delta = now - lastUpdate;

        if (delta > 0) {
            final double factor = this.d * delta;

            this.performanceCounters.addCpuActiveTime(Math.round(this.cpuSupply * factor));
            this.performanceCounters.setCpuIdleTime(Math.round((this.cpuCapacity - this.cpuSupply) * factor));
            this.performanceCounters.addCpuStealTime(Math.round((this.cpuDemand - this.cpuSupply) * factor));
        }

        this.performanceCounters.setCpuDemand(this.cpuDemand);
        this.performanceCounters.setCpuSupply(this.cpuSupply);
        this.performanceCounters.setCpuCapacity(this.cpuCapacity);
    }

    @Override
    public long onUpdate(long now) {
        updateCounters(now);

        return Long.MAX_VALUE;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add an edge to the workload
     * TODO: maybe add a check if there is already an edge
     */
    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.workloadEdge = consumerEdge;
    }

    /**
     * Add an edge to the cpuMux
     * TODO: maybe add a check if there is already an edge
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.cpuEdge = supplierEdge;
    }

    /**
     * Push demand to the cpuMux if the demand has changed
     **/
    @Override
    public void pushDemand(FlowEdge supplierEdge, double newDemand) {
        this.cpuEdge.pushDemand(newDemand);
    }

    /**
     * Push supply to the workload if the supply has changed
     **/
    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {
        this.workloadEdge.pushSupply(newSupply);
    }

    /**
     * Handle new demand from the workload by sending it through to the cpuMux
     **/
    @Override
    public void handleDemand(FlowEdge consumerEdge, double newDemand) {

        updateCounters(this.clock.millis());
        this.cpuDemand = newDemand;

        pushDemand(this.cpuEdge, newDemand);
    }

    /**
     * Handle a new supply pushed by the cpuMux by sending it through to the workload
     **/
    @Override
    public void handleSupply(FlowEdge supplierEdge, double newCpuSupply) {

        updateCounters(this.clock.millis());
        this.cpuSupply = newCpuSupply;

        pushSupply(this.workloadEdge, newCpuSupply);
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        this.workloadEdge = null;
        this.shutdown();
    }

    @Override
    public double getCapacity() {
        return this.cpuCapacity;
    }

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        this.cpuEdge = null;
        this.shutdown();
    }
}
