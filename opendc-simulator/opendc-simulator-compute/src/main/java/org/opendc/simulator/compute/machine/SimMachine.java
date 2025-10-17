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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.ComputeResource;
import org.opendc.simulator.compute.cpu.SimCpu;
import org.opendc.simulator.compute.gpu.SimGpu;
import org.opendc.simulator.compute.memory.Memory;
import org.opendc.simulator.compute.models.GpuModel;
import org.opendc.simulator.compute.models.MachineModel;
import org.opendc.simulator.compute.power.PowerModel;
import org.opendc.simulator.compute.power.SimPsu;
import org.opendc.simulator.compute.workload.ChainWorkload;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.opendc.simulator.compute.workload.VirtualMachine;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowDistributor;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory;
import org.opendc.simulator.engine.graph.distributionPolicies.MaxMinFairnessFlowDistributor;

/**
 * A machine that is able to execute {@link SimWorkload} objects.
 */
public class SimMachine {
    private final MachineModel machineModel;
    private final FlowEngine engine;

    private final InstantSource clock;

    private SimPsu psu;
    private Memory memory;

    private final FlowDistributor[] distributors = new FlowDistributor[ResourceType.values().length];
    //    private final Hashtable<ResourceType, FlowDistributor> distributors = new Hashtable<>();

    private final List<ResourceType> availableResourceTypes;
    private final Hashtable<ResourceType, ArrayList<ComputeResource>> computeResources = new Hashtable<>();

    private final Consumer<Exception> completion;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public ComputeResource getResource(ResourceType resourceType, int id) {
        if (!this.computeResources.containsKey(resourceType)) {
            throw new RuntimeException("No such resource type: " + resourceType);
        }
        for (ComputeResource resource : this.computeResources.get(resourceType)) {
            if (resource.getId() == id) {
                return resource;
            }
        }
        throw new RuntimeException("No such resource with id: " + id + " of type: " + resourceType);
    }

    public ArrayList<ComputeResource> getResources(ResourceType resourceType) {
        if (!this.computeResources.containsKey(resourceType)) {
            throw new RuntimeException("No such resource type: " + resourceType);
        }
        return this.computeResources.get(resourceType);
    }

    public PerformanceCounters getPerformanceCounters() {

        return this.computeResources.get(ResourceType.CPU).getFirst().getPerformanceCounters();
    }

    public List<PerformanceCounters> getGpuPerformanceCounters() {
        List<PerformanceCounters> counters = new ArrayList<>();
        List<ComputeResource> gpus = this.computeResources.get(ResourceType.GPU) == null
                ? new ArrayList<>()
                : this.computeResources.get(ResourceType.GPU);

        for (ComputeResource gpu : gpus) {
            counters.add(gpu.getPerformanceCounters());
        }
        return counters;
    }

    public PerformanceCounters getGpuPerformanceCounters(int GpuId) {
        for (ComputeResource gpu : this.computeResources.get(ResourceType.GPU)) {
            if (gpu.getId() == GpuId) {
                return gpu.getPerformanceCounters();
            }
        }
        throw new RuntimeException("No such gpu id: " + GpuId);
    }

    public MachineModel getMachineModel() {
        return machineModel;
    }

    public FlowEngine getEngine() {
        return engine;
    }

    public InstantSource getClock() {
        return clock;
    }

    public SimCpu getCpu() {
        return (SimCpu) this.computeResources.get(ResourceType.CPU).getFirst();
    }

    public Memory getMemory() {
        return memory;
    }

    public SimPsu getPsu() {
        return psu;
    }

    public ArrayList<SimGpu> getGpus() {
        ArrayList<SimGpu> gpus = new ArrayList<>();
        if (!this.computeResources.containsKey(ResourceType.GPU)) {
            return gpus;
        }
        for (ComputeResource gpu : this.computeResources.get(ResourceType.GPU)) {
            if (gpu instanceof SimGpu) {
                gpus.add((SimGpu) gpu);
            }
        }
        return gpus;
    }

    public SimGpu getGpu(int gpuId) {
        for (ComputeResource gpu : this.computeResources.get(ResourceType.GPU)) {
            if (gpu.getId() == gpuId) {
                return (SimGpu) gpu;
            }
        }
        throw new RuntimeException("No such gpu id: " + gpuId);
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

    public List<ResourceType> getAvailableResourceTypes() {
        return availableResourceTypes;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimMachine(
            FlowEngine engine,
            MachineModel machineModel,
            FlowDistributor powerDistributor,
            PowerModel cpuPowerModel,
            @Nullable PowerModel gpuPowerModel,
            Consumer<Exception> completion) {
        this.engine = engine;
        this.machineModel = machineModel;
        this.clock = engine.getClock();

        this.availableResourceTypes = this.machineModel.getUsedResources();

        int maxTasks = Math.max(
                this.machineModel.getCpuModel().getCoreCount(),
                this.machineModel.getGpuModels().size() * 100);

        // Create the psu and cpu and connect them
        this.psu = new SimPsu(engine);
        new FlowEdge(this.psu, powerDistributor);
        this.distributors[ResourceType.POWER.ordinal()] = new MaxMinFairnessFlowDistributor(
                engine, 1 + this.machineModel.getGpuModels().size(), 1); // Maybe First fit
        new FlowEdge(this.distributors[ResourceType.POWER.ordinal()], this.psu);

        this.computeResources.put(
                ResourceType.CPU,
                new ArrayList<>(List.of(new SimCpu(engine, this.machineModel.getCpuModel(), cpuPowerModel, 0))));

        // Connect the CPU to the PSU
        new FlowEdge(
                (FlowConsumer) this.computeResources.get(ResourceType.CPU).getFirst(),
                (FlowSupplier) this.distributors[ResourceType.POWER.ordinal()],
                ResourceType.POWER,
                0,
                -1);

        // Create a FlowDistributor and add the cpu as supplier
        this.distributors[ResourceType.CPU.ordinal()] = FlowDistributorFactory.getFlowDistributor(
                engine, this.machineModel.getCpuDistributionStrategy(), maxTasks, 1);
        new FlowEdge(
                this.distributors[ResourceType.CPU.ordinal()],
                (FlowSupplier) this.computeResources.get(ResourceType.CPU).getFirst(),
                ResourceType.CPU,
                -1,
                0);

        // TODO: include memory as flow node
        this.memory = new Memory(engine, this.machineModel.getMemory());

        if (this.availableResourceTypes.contains(ResourceType.GPU)) {
            this.distributors[ResourceType.GPU.ordinal()] = FlowDistributorFactory.getFlowDistributor(
                    engine,
                    this.machineModel.getGpuDistributionStrategy(),
                    maxTasks,
                    this.machineModel.getGpuModels().size());
            ArrayList<ComputeResource> gpus = new ArrayList<>();

            for (GpuModel gpuModel : machineModel.getGpuModels()) {
                // create a new GPU
                SimGpu gpu = new SimGpu(
                        engine, gpuModel, gpuPowerModel, gpuModel.getId(), gpuModel.getVirtualizationOverheadModel());
                gpus.add(gpu);
                // Connect the GPU to the distributor
                new FlowEdge(
                        this.distributors[ResourceType.GPU.ordinal()],
                        gpu,
                        ResourceType.GPU,
                        gpuModel.getId(),
                        gpuModel.getId());
                // Connect the GPU to the PSU
                new FlowEdge(
                        gpu,
                        this.distributors[ResourceType.POWER.ordinal()],
                        ResourceType.POWER,
                        gpuModel.getId(),
                        gpuModel.getId());
            }
            this.computeResources.put(ResourceType.GPU, gpus);
        }

        this.completion = completion;
    }

    public void shutdown() {
        shutdown(null);
    }

    /**
     * Close all related hardware
     */
    public void shutdown(Exception cause) {
        this.psu.closeNode();
        this.psu = null;

        // Close resource Flow Nodes
        for (List<ComputeResource> resources : this.computeResources.values()) {
            for (ComputeResource resource : resources) {
                ((FlowNode) resource).closeNode();
            }
            resources.clear();
        }
        this.memory = null;

        for (ResourceType resourceType : this.availableResourceTypes) {
            this.distributors[resourceType.ordinal()].closeNode();
            this.distributors[resourceType.ordinal()] = null;
        }

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
     * @param workload The workload that needs to be executed
     * @param completion The completion callback that needs to be called when the workload is done
     */
    public VirtualMachine startWorkload(ChainWorkload workload, Consumer<Exception> completion) {

        ArrayList<FlowSupplier> distributors = new ArrayList<>();
        for (ResourceType resourceType : this.availableResourceTypes) {
            distributors.add(this.distributors[resourceType.ordinal()]);
        }

        return (VirtualMachine) workload.startWorkload(distributors, this, completion);
    }
}
