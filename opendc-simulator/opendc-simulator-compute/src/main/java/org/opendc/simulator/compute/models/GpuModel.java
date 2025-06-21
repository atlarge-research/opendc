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

import java.util.Objects;

/**
 * A single logical compute unit of processor node, either virtual or physical.
 */
public final class GpuModel {
    private final int id;
    private final int coreCount;
    private final double coreSpeed;
    private final double totalCoreCapacity;
    private final double memoryBandwidth;
    private final long memorySize;
    private final String vendor;
    private final String modelName;
    private final String arch;

    /**
     * Construct a {@link GpuModel} instance.
     *
     * @param id The identifier of the GPU core within the processing node.
     * @param coreCount The number of cores present in the GPU
     * @param coreSpeed The speed of a single core
     * @param memoryBandwidth The speed of the memory in  MHz
     * @param memorySize The memory size of the GPU
     * @param vendor The vendor of the GPU
     * @param modelName The name of the GPU
     * @param arch The architecture of the GPU
     */
    public GpuModel(
            int id,
            int coreCount,
            double coreSpeed,
            double memoryBandwidth,
            long memorySize,
            String vendor,
            String modelName,
            String arch) {
        this.id = id;
        this.coreCount = coreCount;
        this.coreSpeed = coreSpeed;
        this.memoryBandwidth = memoryBandwidth;
        this.memorySize = memorySize;
        this.totalCoreCapacity = coreSpeed * coreCount;
        this.vendor = vendor;
        this.modelName = modelName;
        this.arch = arch;
    }

    /**
     * Construct a {@link GpuModel} instance. Purely as a processing unit
     *
     * @param id The identifier of the GPU core within the processing node.
     * @param coreCount The number of cores present in the GPU
     * @param coreSpeed The speed of a single core
     */
    public GpuModel(int id, int coreCount, double coreSpeed) {
        this(id, coreCount, coreSpeed, 0, 0, "unkown", "unkown", "unkown");
    }

    public GpuModel(int id, int coreCount, double coreSpeed, double memoryBandwidth, long memorySize) {
        this(id, coreCount, coreSpeed, memoryBandwidth, memorySize, "unkown", "unkown", "unkown");
    }

    /**
     * Return the identifier of the GPU core within the processing node.
     */
    public int getId() {
        return id;
    }

    /**
     * Return the number of logical GPUs in the processor node.
     */
    public int getCoreCount() {
        return coreCount;
    }

    /**
     * Return the clock rate of a single core of the GPU MHz.
     */
    public double getCoreSpeed() {
        return coreSpeed;
    }

    /**
     * Return the clock rate of the GPU in MHz.
     */
    public double getTotalCoreCapacity() {
        return totalCoreCapacity;
    }

    /**
     * Return the speed of the memory in Mhz.
     */
    public double getMemoryBandwidth() {
        return memoryBandwidth;
    }

    /**
     * Return the size of the memory in MB.
     */
    public long getMemorySize() {
        return memorySize;
    }

    /**
     * Return the vendor of the storage device.
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Return the model name of the device.
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Return the micro-architecture of the processor node.
     */
    public String getArchitecture() {
        return arch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GpuModel that = (GpuModel) o;
        return id == that.id
                && Double.compare(that.totalCoreCapacity, totalCoreCapacity) == 0
                && Double.compare(that.coreSpeed, coreSpeed) == 0
                && Double.compare(that.memoryBandwidth, memoryBandwidth) == 0
                && Double.compare(that.memorySize, memorySize) == 0
                && Objects.equals(vendor, that.vendor)
                && Objects.equals(modelName, that.modelName)
                && Objects.equals(arch, that.arch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, coreCount, coreSpeed, totalCoreCapacity, memoryBandwidth, memorySize, vendor, modelName, arch);
    }

    @Override
    public String toString() {
        return "ProcessingUnit[" + "id= " + id + ", gpuCoreCount= " + coreCount + ", gpuCoreSpeed= " + coreSpeed
                + ", frequency= " + totalCoreCapacity + ", gpuMemoryBandwidth" + memoryBandwidth + ", gpuMemorySize"
                + memorySize + ", vendor= " + vendor + ", modelName= " + modelName + ", arch= "
                + arch + "]";
    }
}
