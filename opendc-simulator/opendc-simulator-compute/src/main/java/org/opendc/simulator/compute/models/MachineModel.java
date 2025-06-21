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

package org.opendc.simulator.compute.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.opendc.common.ResourceType;
import org.opendc.simulator.engine.graph.distributionPolicies.DistributionPolicy;

/**
 * A description of the physical or virtual machine on which a bootable image runs.
 */
public final class MachineModel {
    private final CpuModel cpuModel;
    private final MemoryUnit memory;
    //    private final List<GpuModel> gpuModels = new ArrayList<>(); // TODO: Implement multi GPU support
    private final List<GpuModel> gpuModels;
    private final DistributionPolicy cpuDistributionStrategy;
    private final DistributionPolicy gpuDistributionPolicy;
    private final List<ResourceType> availableResources = new ArrayList<>();
    /**
     * Construct a {@link MachineModel} instance.
     *
     * @param cpuModel The cpu available to the image.
     * @param memory The list of memory units available to the image.
     */
    public MachineModel(
            CpuModel cpuModel,
            MemoryUnit memory,
            @Nullable List<GpuModel> gpuModels,
            DistributionPolicy cpuDistributionPolicy,
            DistributionPolicy gpuDistributionPolicy) {
        this.cpuModel = cpuModel;
        this.memory = memory;
        this.cpuDistributionStrategy = cpuDistributionPolicy;
        this.gpuDistributionPolicy = gpuDistributionPolicy;
        this.availableResources.add(ResourceType.CPU);
        // TODO: Add Memory
        //        this.usedResources.add(ResourceType.Memory);
        if (gpuModels != null && !gpuModels.isEmpty()) {
            //            this.gpuModels = gpuModels;
            this.gpuModels = new ArrayList<>();
            this.gpuModels.add(new GpuModel(
                    0,
                    gpuModels.getFirst().getCoreCount() * gpuModels.size(), // merges multiple GPUs into one
                    gpuModels.getFirst().getCoreSpeed(),
                    gpuModels.getFirst().getMemoryBandwidth(),
                    gpuModels.getFirst().getMemorySize() * gpuModels.size(), // merges multiple GPUs into one
                    gpuModels.getFirst().getVendor(),
                    gpuModels.getFirst().getModelName(),
                    gpuModels.getFirst().getArchitecture()));
            this.availableResources.add(ResourceType.GPU);
        } else {
            this.gpuModels = new ArrayList<>();
        }
    }

    /**
     * Construct a {@link MachineModel} instance.
     * A list of the same cpus, are automatically converted to a single CPU with the number of cores of
     * all cpus in the list combined.
     *
     * @param cpus The list of processing units available to the image.
     * @param memory The list of memory units available to the image.
     */
    public MachineModel(List<CpuModel> cpus, MemoryUnit memory) {

        this(
                new CpuModel(
                        cpus.get(0).getId(),
                        cpus.get(0).getCoreCount() * cpus.size(),
                        cpus.get(0).getCoreSpeed(),
                        cpus.get(0).getVendor(),
                        cpus.get(0).getModelName(),
                        cpus.get(0).getArchitecture()),
                memory,
                null,
                null,
                null);
    }

    /**
     * Construct a {@link MachineModel} instance.
     * A list of the same cpus, are automatically converted to a single CPU with the number of cores of
     * all cpus in the list combined.
     *
     * @param cpus The list of processing units available to the image.
     * @param memory The list of memory units available to the image.
     * @param gpus The list of GPUs available to the image.
     */
    public MachineModel(
            List<CpuModel> cpus,
            MemoryUnit memory,
            List<GpuModel> gpus,
            DistributionPolicy cpuDistributionPolicy,
            DistributionPolicy gpuDistributionPolicy) {

        this(
                new CpuModel(
                        cpus.get(0).getId(),
                        cpus.get(0).getCoreCount() * cpus.size(), // merges multiple CPUs into one
                        cpus.get(0).getCoreSpeed(),
                        cpus.get(0).getVendor(),
                        cpus.get(0).getModelName(),
                        cpus.get(0).getArchitecture()),
                memory,
                gpus != null ? gpus : new ArrayList<>(),
                cpuDistributionPolicy,
                gpuDistributionPolicy);
    }

    /**
     * Return the processing units of this machine.
     */
    public CpuModel getCpuModel() {
        return this.cpuModel;
    }

    /**
     * Return the memory units of this machine.
     */
    public MemoryUnit getMemory() {
        return memory;
    }

    public List<GpuModel> getGpuModels() {
        return gpuModels;
    }

    /**
     * Return specific GPU model by id.
     * @param modelId The id of the GPU model to return.
     * @return The GPU model with the given id, or null if not found.
     */
    public GpuModel getGpuModel(int modelId) {
        for (GpuModel gpuModel : gpuModels) {
            if (gpuModel.getId() == modelId) {
                return gpuModel;
            }
        }
        return null;
    }

    /**
     * Return the distribution strategy for the CPU.
     */
    public DistributionPolicy getCpuDistributionStrategy() {
        return cpuDistributionStrategy;
    }

    /**
     * Return the distribution strategy for the GPU.
     */
    public DistributionPolicy getGpuDistributionStrategy() {
        return gpuDistributionPolicy;
    }

    /**
     * Return the resources of this machine.
     */
    public List<ResourceType> getUsedResources() {
        return availableResources;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MachineModel that = (MachineModel) o;
        return cpuModel.equals(that.cpuModel) && memory.equals(that.memory) && gpuModels.equals(that.gpuModels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpuModel, memory, gpuModels);
    }

    @Override
    public String toString() {
        return "MachineModel[cpus=" + cpuModel + ",memory=" + memory + ",gpus=" + gpuModels + "]";
    }
}
