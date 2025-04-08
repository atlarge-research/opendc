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

import java.util.List;
import java.util.Objects;

/**
 * A description of the physical or virtual machine on which a bootable image runs.
 */
public final class MachineModel {
    private final CpuModel cpuModel;
    private final CpuModel accelModel;
    private final MemoryUnit memory;

    /**
     * Construct a {@link MachineModel} instance.
     *
     * @param cpuModel The cpu available to the image.
     * @param memory The list of memory units available to the image.
     */
    public MachineModel(CpuModel cpuModel, CpuModel accelModel, MemoryUnit memory) {
        this.cpuModel = cpuModel;
        this.accelModel = accelModel;
        this.memory = memory;
    }

    /**
     * Construct a {@link MachineModel} instance.
     * A list of the same cpus, are automatically converted to a single CPU with the number of cores of
     * all cpus in the list combined.
     *
     * @param cpus The list of processing units available to the image.
     * @param memory The list of memory units available to the image.
     */
    public MachineModel(List<CpuModel> cpus, List<CpuModel> accelerators, MemoryUnit memory) {

        this(
                new CpuModel(
                        cpus.get(0).getId(),
                        cpus.get(0).getCoreCount() * cpus.size(),
                        cpus.get(0).getCoreSpeed(),
                        cpus.get(0).getVendor(),
                        cpus.get(0).getModelName(),
                        cpus.get(0).getArchitecture()),
                new CpuModel(
                        accelerators.get(0).getId(),
                        accelerators.get(0).getCoreCount() * cpus.size(),
                        accelerators.get(0).getCoreSpeed(),
                        accelerators.get(0).getVendor(),
                        accelerators.get(0).getModelName(),
                        accelerators.get(0).getArchitecture()),
                memory);
    }

    /**
     * Return the processing units of this machine.
     */
    public CpuModel getCpuModel() {
        return this.cpuModel;
    }

    public CpuModel getAccelModel() {
        return this.accelModel;
    }

    /**
     * Return the memory units of this machine.
     */
    public MemoryUnit getMemory() {
        return memory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MachineModel that = (MachineModel) o;
        return cpuModel.equals(that.cpuModel) && accelModel.equals(that.accelModel) && memory.equals(that.memory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpuModel, memory);
    }

    @Override
    public String toString() {
        return "MachineModel[cpus=" + cpuModel + ",memory=" + memory + "]";
    }
}
