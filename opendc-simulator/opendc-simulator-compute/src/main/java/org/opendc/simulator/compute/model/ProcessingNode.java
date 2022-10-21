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

package org.opendc.simulator.compute.model;

import java.util.Objects;

/**
 * A processing node/package/socket containing possibly several CPU cores.
 */
public final class ProcessingNode {
    private final String vendor;
    private final String modelName;
    private final String arch;
    private final int coreCount;

    /**
     * Construct a {@link ProcessingNode} instance.
     *
     * @param vendor The vendor of the storage device.
     * @param modelName The model name of the device.
     * @param arch The micro-architecture of the processor node.
     * @param coreCount The number of logical CPUs in the processor node.
     */
    public ProcessingNode(String vendor, String modelName, String arch, int coreCount) {
        this.vendor = vendor;
        this.modelName = modelName;
        this.arch = arch;
        this.coreCount = coreCount;
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

    /**
     * Return the number of logical CPUs in the processor node.
     */
    public int getCoreCount() {
        return coreCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingNode that = (ProcessingNode) o;
        return coreCount == that.coreCount
                && vendor.equals(that.vendor)
                && modelName.equals(that.modelName)
                && arch.equals(that.arch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendor, modelName, arch, coreCount);
    }

    @Override
    public String toString() {
        return "ProcessingNode[vendor='" + vendor + "',modelName='" + modelName + "',arch=" + arch + ",coreCount="
                + coreCount + "]";
    }
}
