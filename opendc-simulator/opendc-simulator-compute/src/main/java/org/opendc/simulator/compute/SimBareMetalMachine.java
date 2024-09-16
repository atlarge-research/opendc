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
import org.opendc.simulator.compute.model.Cpu;
import org.opendc.simulator.compute.model.MachineModel;
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
    private final SimCpu cpu;

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

        this.cpu = new SimCpu(psu, model.getCpu(), 0);
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
        final SimAbstractMachineContext context = (SimAbstractMachineContext) getActiveContext();

        if (context == null) {
            return 0.0;
        }

        return cpu.getFrequency();
    }

    /**
     * The CPU demand of the machine in MHz.
     */
    public double getCpuDemand() {
        final SimAbstractMachineContext context = (SimAbstractMachineContext) getActiveContext();

        if (context == null) {
            return 0.0;
        }

        return cpu.getDemand();
    }

    /**
     * The CPU usage of the machine in MHz.
     */
    public double getCpuUsage() {
        final SimAbstractMachineContext context = (SimAbstractMachineContext) getActiveContext();

        if (context == null) {
            return 0.0;
        }

        return cpu.getSpeed();
    }

    @Override
    protected SimAbstractMachine.SimAbstractMachineContext createContext(
            SimWorkload workload, Map<String, Object> meta, Consumer<Exception> completion) {
        return new SimAbstractMachineContext(this, workload, meta, completion);
    }

    /**
     * The execution context for a {@link SimBareMetalMachine}.
     */
    private static final class SimAbstractMachineContext extends SimAbstractMachine.SimAbstractMachineContext {
        private final FlowGraph graph;
        private final SimCpu cpu;
        private final Memory memory;
        private final List<NetworkAdapter> net;
        private final List<StorageDevice> disk;

        private SimAbstractMachineContext(
                SimBareMetalMachine machine,
                SimWorkload workload,
                Map<String, Object> meta,
                Consumer<Exception> completion) {
            super(machine, workload, meta, completion);

            this.graph = machine.graph;
            this.cpu = machine.cpu;
            this.memory = machine.memory;
            this.net = machine.net;
            this.disk = machine.disk;
        }

        @Override
        public FlowGraph getGraph() {
            return graph;
        }

        @Override
        public SimCpu getCpu() {
            return cpu;
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
    private static final class SimCpu implements SimProcessingUnit {
        private final SimPsu psu;
        private final Cpu cpuModel;
        private final InPort port;

        private SimCpu(SimPsu psu, Cpu cpuModel, int id) {
            this.psu = psu;
            this.cpuModel = cpuModel;
            this.port = psu.getCpuPower(id, cpuModel);

            this.port.pull((float) cpuModel.getTotalCapacity());
        }

        @Override
        public double getFrequency() {
            return port.getCapacity();
        }

        @Override
        public void setFrequency(double frequency) {
            // Clamp the capacity of the CPU between [0.0, maxFreq]
            frequency = Math.max(0, Math.min(cpuModel.getTotalCapacity(), frequency));
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
        public org.opendc.simulator.compute.model.Cpu getCpuModel() {
            return cpuModel;
        }

        @Override
        public Inlet getInput() {
            return port;
        }

        @Override
        public String toString() {
            return "SimBareMetalMachine.Cpu[model=" + cpuModel + "]";
        }
    }
}
