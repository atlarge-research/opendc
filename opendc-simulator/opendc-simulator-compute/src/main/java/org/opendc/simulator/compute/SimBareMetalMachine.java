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

package org.opendc.simulator.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opendc.simulator.compute.device.SimPeripheral;
import org.opendc.simulator.compute.model.MachineModel;
import org.opendc.simulator.compute.model.ProcessingUnit;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.InPort;
import org.opendc.simulator.flow2.Inlet;

/**
 * A simulated bare-metal machine that is able to run a single workload.
 *
 * <p>
 * A {@link SimBareMetalMachine} is a stateful object, and you should be careful when operating this object concurrently. For
 * example, the class expects only a single concurrent call to {@link #startWorkload(SimWorkload, Map, Consumer)} )}.
 */
public final class SimBareMetalMachine extends SimAbstractMachine {
    /**
     * The {@link FlowGraph} in which the simulation takes places.
     */
    private final FlowGraph graph;

    /**
     * The {@link SimPsu} of this bare metal machine.
     */
    private final SimPsu psu;

    /**
     * The resources of this machine.
     */
    private final List<Cpu> cpus;

    private final Memory memory;
    private final List<NetworkAdapter> net;
    private final List<StorageDevice> disk;

    /**
     * Construct a {@link SimBareMetalMachine} instance.
     *
     * @param graph The {@link FlowGraph} to which the machine belongs.
     * @param model The machine model to simulate.
     * @param psuFactory The {@link SimPsuFactory} to construct the power supply of the machine.
     */
    private SimBareMetalMachine(FlowGraph graph, MachineModel model, SimPsuFactory psuFactory) {
        super(model);

        this.graph = graph;
        this.psu = psuFactory.newPsu(this, graph);

        int cpuIndex = 0;
        final ArrayList<Cpu> cpus = new ArrayList<>();
        this.cpus = cpus;
        for (ProcessingUnit cpu : model.getCpus()) {
            cpus.add(new Cpu(psu, cpu, cpuIndex++));
        }

        this.memory = new Memory(graph, model.getMemory());

        int netIndex = 0;
        final ArrayList<NetworkAdapter> net = new ArrayList<>();
        this.net = net;
        for (org.opendc.simulator.compute.model.NetworkAdapter adapter : model.getNetwork()) {
            net.add(new NetworkAdapter(graph, adapter, netIndex++));
        }

        int diskIndex = 0;
        final ArrayList<StorageDevice> disk = new ArrayList<>();
        this.disk = disk;
        for (org.opendc.simulator.compute.model.StorageDevice device : model.getStorage()) {
            disk.add(new StorageDevice(graph, device, diskIndex++));
        }
    }

    /**
     * Create a {@link SimBareMetalMachine} instance.
     *
     * @param graph The {@link FlowGraph} to which the machine belongs.
     * @param model The machine model to simulate.
     * @param psuFactory The {@link SimPsuFactory} to construct the power supply of the machine.
     */
    public static SimBareMetalMachine create(FlowGraph graph, MachineModel model, SimPsuFactory psuFactory) {
        return new SimBareMetalMachine(graph, model, psuFactory);
    }

    /**
     * Create a {@link SimBareMetalMachine} instance with a no-op PSU.
     *
     * @param graph The {@link FlowGraph} to which the machine belongs.
     * @param model The machine model to simulate.
     */
    public static SimBareMetalMachine create(FlowGraph graph, MachineModel model) {
        return new SimBareMetalMachine(graph, model, SimPsuFactories.noop());
    }

    /**
     * Return the {@link SimPsu} belonging to this bare metal machine.
     */
    public SimPsu getPsu() {
        return psu;
    }

    /**
     * Return the list of peripherals attached to this bare metal machine.
     */
    @Override
    public List<? extends SimPeripheral> getPeripherals() {
        return Collections.unmodifiableList(net);
    }

    /**
     * Return the CPU capacity of the machine in MHz.
     */
    public double getCpuCapacity() {
        final Context context = (Context) getActiveContext();

        if (context == null) {
            return 0.0;
        }

        float capacity = 0.f;

        for (SimProcessingUnit cpu : context.cpus) {
            capacity += cpu.getFrequency();
        }

        return capacity;
    }

    /**
     * The CPU demand of the machine in MHz.
     */
    public double getCpuDemand() {
        final Context context = (Context) getActiveContext();

        if (context == null) {
            return 0.0;
        }

        float demand = 0.f;

        for (SimProcessingUnit cpu : context.cpus) {
            demand += cpu.getDemand();
        }

        return demand;
    }

    /**
     * The CPU usage of the machine in MHz.
     */
    public double getCpuUsage() {
        final Context context = (Context) getActiveContext();

        if (context == null) {
            return 0.0;
        }

        float rate = 0.f;

        for (SimProcessingUnit cpu : context.cpus) {
            rate += cpu.getSpeed();
        }

        return rate;
    }

    @Override
    protected SimAbstractMachine.Context createContext(
            SimWorkload workload, Map<String, Object> meta, Consumer<Exception> completion) {
        return new Context(this, workload, meta, completion);
    }

    /**
     * The execution context for a {@link SimBareMetalMachine}.
     */
    private static final class Context extends SimAbstractMachine.Context {
        private final FlowGraph graph;
        private final List<Cpu> cpus;
        private final Memory memory;
        private final List<NetworkAdapter> net;
        private final List<StorageDevice> disk;

        private Context(
                SimBareMetalMachine machine,
                SimWorkload workload,
                Map<String, Object> meta,
                Consumer<Exception> completion) {
            super(machine, workload, meta, completion);

            this.graph = machine.graph;
            this.cpus = machine.cpus;
            this.memory = machine.memory;
            this.net = machine.net;
            this.disk = machine.disk;
        }

        @Override
        public FlowGraph getGraph() {
            return graph;
        }

        @Override
        public List<? extends SimProcessingUnit> getCpus() {
            return cpus;
        }

        @Override
        public SimMemory getMemory() {
            return memory;
        }

        @Override
        public List<? extends SimNetworkInterface> getNetworkInterfaces() {
            return net;
        }

        @Override
        public List<? extends SimStorageInterface> getStorageInterfaces() {
            return disk;
        }
    }

    /**
     * A {@link SimProcessingUnit} of a bare-metal machine.
     */
    private static final class Cpu implements SimProcessingUnit {
        private final SimPsu psu;
        private final ProcessingUnit model;
        private final InPort port;

        private Cpu(SimPsu psu, ProcessingUnit model, int id) {
            this.psu = psu;
            this.model = model;
            this.port = psu.getCpuPower(id, model);

            this.port.pull((float) model.getFrequency());
        }

        @Override
        public double getFrequency() {
            return port.getCapacity();
        }

        @Override
        public void setFrequency(double frequency) {
            // Clamp the capacity of the CPU between [0.0, maxFreq]
            frequency = Math.max(0, Math.min(model.getFrequency(), frequency));
            psu.setCpuFrequency(port, frequency);
        }

        @Override
        public double getDemand() {
            return port.getDemand();
        }

        @Override
        public double getSpeed() {
            return port.getRate();
        }

        @Override
        public ProcessingUnit getModel() {
            return model;
        }

        @Override
        public Inlet getInput() {
            return port;
        }

        @Override
        public String toString() {
            return "SimBareMetalMachine.Cpu[model=" + model + "]";
        }
    }
}
