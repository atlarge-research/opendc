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
public final class CpuModel {
    private final int id;
    private final int coreCount;
    private final double coreSpeed;
    private final double totalCapacity;

    private final String vendor;
    private final String modelName;
    private final String arch;
    private final Double componentPrice;
    private final ComponentDegradationModel degradationModel;

    /**
     * Construct a {@link CpuModel} instance.
     *
     * @param id The identifier of the CPU core within the processing node.
     * @param coreCount The number of cores present in the CPU
     * @param coreSpeed The speed of a single core
     * @param vendor The vendor of the CPU
     * @param modelName The name of the CPU
     * @param arch The architecture of the CPU
     */
    public CpuModel(
            int id,
            int coreCount,
            double coreSpeed,
            String vendor,
            String modelName,
            String arch,
            Double componentPrice,
            ComponentDegradationModel degradationModel) {
        this.id = id;
        this.coreCount = coreCount;
        this.coreSpeed = coreSpeed;
        this.totalCapacity = coreSpeed * coreCount;
        this.vendor = vendor;
        this.modelName = modelName;
        this.arch = arch;
        this.componentPrice = componentPrice;
        this.degradationModel = degradationModel;
    }

    public CpuModel(int id, int coreCount, double coreSpeed, double componentPrice, ComponentDegradationModel degradationModel) {
        this(id, coreCount, coreSpeed, "unkown", "unkown", "unkown",componentPrice, degradationModel);
    }

    /**
     * Return the identifier of the CPU core within the processing node.
     */
    public int getId() {
        return id;
    }

    /**
     * Return the number of logical CPUs in the processor node.
     */
    public int getCoreCount() {
        return coreCount;
    }

    /**
     * Return the clock rate of a single core of the CPU MHz.
     */
    public double getCoreSpeed() {
        return coreSpeed;
    }

    /**
     * Return the clock rate of the CPU in MHz.
     */
    public double getTotalCapacity() {
        return totalCapacity;
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

    public Double getComponentPrice() {
        return componentPrice;
    }

    public ComponentDegradationModel getDegradationModel() {
        return degradationModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CpuModel that = (CpuModel) o;
        return id == that.id
                && Double.compare(that.totalCapacity, totalCapacity) == 0
                && Double.compare(that.coreSpeed, coreSpeed) == 0
                && Objects.equals(vendor, that.vendor)
                && Objects.equals(modelName, that.modelName)
                && Objects.equals(arch, that.arch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, coreCount, coreSpeed, totalCapacity, vendor, modelName, arch);
    }

    @Override
    public String toString() {
        return "ProcessingUnit[" + "id= " + id + ", cpuCoreCount= " + coreCount + ", coreSpeed= " + coreSpeed
                + ", frequency= " + totalCapacity + ", vendor= " + vendor + ", modelName= " + modelName + ", arch= "
                + arch + "]";
    }
}
