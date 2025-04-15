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

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A description of the physical or virtual machine on which a bootable image runs.
 */
public final class MachineModel {
    private final CpuModel cpuModel;
    private final MemoryUnit memory;
    private final List<GpuModel> gpuModels = new ArrayList<>();

    /**
     * Construct a {@link MachineModel} instance.
     *
     * @param cpuModel The cpu available to the image.
     * @param memory The list of memory units available to the image.
     */
    public MachineModel(CpuModel cpuModel, MemoryUnit memory, List<GpuModel> gpuModels) {
        this.cpuModel = cpuModel;
        this.memory = memory;
        if (gpuModels != null && !gpuModels.isEmpty()) {
            this.gpuModels.addAll(gpuModels);
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
    public MachineModel(List<CpuModel> cpus, MemoryUnit memory, List<GpuModel> gpus) {

        this(
            new CpuModel(
                cpus.get(0).getId(),
                cpus.get(0).getCoreCount() * cpus.size(),
                cpus.get(0).getCoreSpeed(),
                cpus.get(0).getVendor(),
                cpus.get(0).getModelName(),
                cpus.get(0).getArchitecture()),
            memory,
            gpus);
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

    public List<GpuModel> getGpuModel() {return gpuModels;}

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
