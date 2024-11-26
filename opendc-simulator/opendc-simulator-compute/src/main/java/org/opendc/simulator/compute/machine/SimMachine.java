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

import java.time.InstantSource;
import java.util.function.Consumer;
import org.opendc.simulator.Multiplexer;
import org.opendc.simulator.compute.cpu.CpuPowerModel;
import org.opendc.simulator.compute.cpu.SimCpu;
import org.opendc.simulator.compute.memory.Memory;
import org.opendc.simulator.compute.models.MachineModel;
import org.opendc.simulator.compute.power.SimPsu;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.compute.workload.Workload;
import org.opendc.simulator.engine.FlowGraph;

/**
 * A machine that is able to execute {@link SimWorkload} objects.
 */
public class SimMachine {
    private final MachineModel machineModel;
    private final FlowGraph graph;

    private final InstantSource clock;

    private SimCpu cpu;
    private Multiplexer cpuMux;
    private SimPsu psu;
    private Memory memory;

    private final Consumer<Exception> completion;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public PerformanceCounters getPerformanceCounters() {
        return this.cpu.getPerformanceCounters();
    }

    public MachineModel getMachineModel() {
        return machineModel;
    }

    public FlowGraph getGraph() {
        return graph;
    }

    public InstantSource getClock() {
        return clock;
    }

    public SimCpu getCpu() {
        return cpu;
    }

    public Multiplexer getCpuMux() {
        return cpuMux;
    }

    public Memory getMemory() {
        return memory;
    }

    public SimPsu getPsu() {
        return psu;
    }

    /**
     * Return the CPU capacity of the hypervisor in MHz.
     */
    public double getCpuCapacity() {
        return 0.0;
    }

    /**
     * The CPU demand of the hypervisor in MHz.
     */
    public double getCpuDemand() {
        return 0.0;
    }

    /**
     * The CPU usage of the hypervisor in MHz.
     */
    public double getCpuUsage() {
        return 0.0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimMachine(
        FlowGraph graph, MachineModel machineModel, Multiplexer powerMux,
        CpuPowerModel cpuPowerModel, Consumer<Exception> completion) {
        this.graph = graph;
        this.machineModel = machineModel;
        this.clock = graph.getEngine().getClock();

        // Create the psu and cpu and connect them
        this.psu = new SimPsu(graph);

        graph.addEdge(this.psu, powerMux);

        this.cpu = new SimCpu(graph, this.machineModel.getCpuModel(), cpuPowerModel, 0);

        graph.addEdge(this.cpu, this.psu);

        this.memory = new Memory(graph, this.machineModel.getMemory());

        // Create a Multiplexer and add the cpu as supplier
        this.cpuMux = new Multiplexer(this.graph);
        graph.addEdge(this.cpuMux, this.cpu);

        this.completion = completion;
    }

    public void shutdown() {
        shutdown(null);
    }

    /**
     * Close all related hardware
     */
    public void shutdown(Exception cause) {
        this.graph.removeNode(this.psu);
        this.psu = null;

        this.graph.removeNode(this.cpu);
        this.cpu = null;

        this.graph.removeNode(this.cpuMux);
        this.cpuMux = null;

        this.memory = null;

        this.completion.accept(cause);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Workload related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Determine whether the specified machine characterized by <code>model</code> can fit on this hypervisor at this
     * moment.
     * TODO: This currently alwasy returns True, maybe remove?
     */
    public boolean canFit(MachineModel model) {
        return true;
    }

    /**
     * Create a Virtual Machine, and start the given workload on it.
     *
     * @param workload
     * @param completion
     * @return
     */
    public VirtualMachine startWorkload(Workload workload, Consumer<Exception> completion) {
        final VirtualMachine vm = new VirtualMachine(this);

        vm.startWorkload(workload, completion);

        return vm;
    }
}
