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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opendc.simulator.compute.device.SimNetworkAdapter;
import org.opendc.simulator.compute.model.MachineModel;
import org.opendc.simulator.compute.model.MemoryUnit;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.Inlet;
import org.opendc.simulator.flow2.Outlet;
import org.opendc.simulator.flow2.sink.SimpleFlowSink;
import org.opendc.simulator.flow2.util.FlowTransformer;
import org.opendc.simulator.flow2.util.FlowTransforms;

/**
 * Abstract implementation of the {@link SimMachine} interface.
 */
public abstract class SimAbstractMachine implements SimMachine {
    private final MachineModel model;

    private Context activeContext;

    /**
     * Construct a {@link SimAbstractMachine} instance.
     *
     * @param model The model of the machine.
     */
    public SimAbstractMachine(MachineModel model) {
        this.model = model;
    }

    @Override
    public final MachineModel getModel() {
        return model;
    }

    @Override
    public final SimMachineContext startWorkload(
            SimWorkload workload, Map<String, Object> meta, Consumer<Exception> completion) {
        if (activeContext != null) {
            throw new IllegalStateException("A machine cannot run multiple workloads concurrently");
        }

        final Context ctx = createContext(workload, new HashMap<>(meta), completion);
        ctx.start();
        return ctx;
    }

    @Override
    public final void cancel() {
        final Context context = activeContext;
        if (context != null) {
            context.shutdown();
        }
    }

    /**
     * Construct a new {@link Context} instance representing the active execution.
     *
     * @param workload   The workload to start on the machine.
     * @param meta       The metadata to pass to the workload.
     * @param completion A block that is invoked when the workload completes carrying an exception if thrown by the workload.
     */
    protected abstract Context createContext(
            SimWorkload workload, Map<String, Object> meta, Consumer<Exception> completion);

    /**
     * Return the active {@link Context} instance (if any).
     */
    protected Context getActiveContext() {
        return activeContext;
    }

    /**
     * The execution context in which the workload runs.
     */
    public abstract static class Context implements SimMachineContext {
        private final SimAbstractMachine machine;
        private final SimWorkload workload;
        private final Map<String, Object> meta;
        private final Consumer<Exception> completion;
        private boolean isClosed;

        /**
         * Construct a new {@link Context} instance.
         *
         * @param machine The {@link SimAbstractMachine} to which the context belongs.
         * @param workload The {@link SimWorkload} to which the context belongs.
         * @param meta The metadata passed to the context.
         * @param completion A block that is invoked when the workload completes carrying an exception if thrown by the workload.
         */
        public Context(
                SimAbstractMachine machine,
                SimWorkload workload,
                Map<String, Object> meta,
                Consumer<Exception> completion) {
            this.machine = machine;
            this.workload = workload;
            this.meta = meta;
            this.completion = completion;
        }

        @Override
        public final Map<String, Object> getMeta() {
            return meta;
        }

        @Override
        public SimWorkload snapshot() {
            return workload.snapshot();
        }

        @Override
        public void reset() {
            final FlowGraph graph = getMemory().getInput().getGraph();

            for (SimProcessingUnit cpu : getCpus()) {
                final Inlet inlet = cpu.getInput();
                graph.disconnect(inlet);
            }

            graph.disconnect(getMemory().getInput());

            for (SimNetworkInterface ifx : getNetworkInterfaces()) {
                ((NetworkAdapter) ifx).disconnect();
            }

            for (SimStorageInterface storage : getStorageInterfaces()) {
                StorageDevice impl = (StorageDevice) storage;
                graph.disconnect(impl.getRead());
                graph.disconnect(impl.getWrite());
            }
        }

        @Override
        public final void shutdown() {
            shutdown(null);
        }

        @Override
        public final void shutdown(Exception cause) {
            if (isClosed) {
                return;
            }

            isClosed = true;
            final SimAbstractMachine machine = this.machine;
            assert machine.activeContext == this : "Invariant violation: multiple contexts active for a single machine";
            machine.activeContext = null;

            // Cancel all the resources associated with the machine
            doCancel();

            try {
                workload.onStop(this);
            } catch (Exception e) {
                if (cause == null) {
                    cause = e;
                } else {
                    cause.addSuppressed(e);
                }
            }

            completion.accept(cause);
        }

        /**
         * Start this context.
         */
        final void start() {
            try {
                machine.activeContext = this;
                workload.onStart(this);
            } catch (Exception cause) {
                shutdown(cause);
            }
        }

        /**
         * Run the stop procedures for the resources associated with the machine.
         */
        protected void doCancel() {
            reset();
        }

        @Override
        public String toString() {
            return "SimAbstractMachine.Context";
        }
    }

    /**
     * The [SimMemory] implementation for a machine.
     */
    public static final class Memory implements SimMemory {
        private final SimpleFlowSink sink;
        private final List<MemoryUnit> models;

        public Memory(FlowGraph graph, List<MemoryUnit> models) {
            long memorySize = 0;
            for (MemoryUnit mem : models) {
                memorySize += mem.getSize();
            }

            this.sink = new SimpleFlowSink(graph, (float) memorySize);
            this.models = models;
        }

        @Override
        public double getCapacity() {
            return sink.getCapacity();
        }

        @Override
        public List<MemoryUnit> getModels() {
            return models;
        }

        @Override
        public Inlet getInput() {
            return sink.getInput();
        }

        @Override
        public String toString() {
            return "SimAbstractMachine.Memory";
        }
    }

    /**
     * A {@link SimNetworkAdapter} implementation for a machine.
     */
    public static class NetworkAdapter extends SimNetworkAdapter implements SimNetworkInterface {
        private final org.opendc.simulator.compute.model.NetworkAdapter model;
        private final FlowTransformer tx;
        private final FlowTransformer rx;
        private final String name;

        /**
         * Construct a {@link NetworkAdapter}.
         */
        public NetworkAdapter(FlowGraph graph, org.opendc.simulator.compute.model.NetworkAdapter model, int index) {
            this.model = model;
            this.tx = new FlowTransformer(graph, FlowTransforms.noop());
            this.rx = new FlowTransformer(graph, FlowTransforms.noop());
            this.name = "eth" + index;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Inlet getTx() {
            return tx.getInput();
        }

        @Override
        public Outlet getRx() {
            return rx.getOutput();
        }

        @Override
        public double getBandwidth() {
            return model.getBandwidth();
        }

        @Override
        protected Outlet getOutlet() {
            return tx.getOutput();
        }

        @Override
        protected Inlet getInlet() {
            return rx.getInput();
        }

        @Override
        public String toString() {
            return "SimAbstractMachine.NetworkAdapterImpl[name=" + name + ", bandwidth=" + model.getBandwidth()
                    + "Mbps]";
        }
    }

    /**
     * A {@link SimStorageInterface} implementation for a machine.
     */
    public static class StorageDevice implements SimStorageInterface {
        private final org.opendc.simulator.compute.model.StorageDevice model;
        private final SimpleFlowSink read;
        private final SimpleFlowSink write;
        private final String name;

        /**
         * Construct a {@link StorageDevice}.
         */
        public StorageDevice(FlowGraph graph, org.opendc.simulator.compute.model.StorageDevice model, int index) {
            this.model = model;
            this.read = new SimpleFlowSink(graph, (float) model.getReadBandwidth());
            this.write = new SimpleFlowSink(graph, (float) model.getWriteBandwidth());
            this.name = "disk" + index;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Inlet getRead() {
            return read.getInput();
        }

        @Override
        public Inlet getWrite() {
            return write.getInput();
        }

        @Override
        public double getCapacity() {
            return model.getCapacity();
        }

        @Override
        public String toString() {
            return "SimAbstractMachine.StorageDeviceImpl[name=" + name + ", capacity=" + model.getCapacity() + "MB]";
        }
    }
}
