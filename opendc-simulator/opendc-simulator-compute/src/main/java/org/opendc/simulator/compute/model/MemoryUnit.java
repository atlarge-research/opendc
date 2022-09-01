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
 * A memory unit of a compute resource, either virtual or physical.
 */
public final class MemoryUnit {
    private final String vendor;
    private final String modelName;
    private final double speed;
    private final long size;

    /**
     * Construct a {@link ProcessingNode} instance.
     *
     * @param vendor The vendor of the storage device.
     * @param modelName The model name of the device.
     * @param speed The access speed of the memory in MHz.
     * @param size The size of the memory unit in MBs.
     */
    public MemoryUnit(String vendor, String modelName, double speed, long size) {
        this.vendor = vendor;
        this.modelName = modelName;
        this.speed = speed;
        this.size = size;
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
     * Return the access speed of the memory in MHz.
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Return the size of the memory unit in MBs.
     */
    public long getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryUnit that = (MemoryUnit) o;
        return Double.compare(that.speed, speed) == 0
                && size == that.size
                && vendor.equals(that.vendor)
                && modelName.equals(that.modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendor, modelName, speed, size);
    }

    @Override
    public String toString() {
        return "ProcessingNode[vendor='" + vendor + "',modelName='" + modelName + "',speed=" + speed + "MHz,size="
                + size + "MB]";
    }
}
