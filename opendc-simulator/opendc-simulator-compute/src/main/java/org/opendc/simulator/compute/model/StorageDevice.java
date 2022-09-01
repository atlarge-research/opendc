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
 * Model for a physical storage device attached to a machine.
 */
public final class StorageDevice {
    private final String vendor;
    private final String modelName;
    private final double capacity;
    private final double readBandwidth;
    private final double writeBandwidth;

    /**
     * Construct a {@link StorageDevice} instance.
     *
     * @param vendor The vendor of the storage device.
     * @param modelName The model name of the device.
     * @param capacity The capacity of the device.
     * @param readBandwidth The read bandwidth of the device in MBps.
     * @param writeBandwidth The write bandwidth of the device in MBps.
     */
    public StorageDevice(
            String vendor, String modelName, double capacity, double readBandwidth, double writeBandwidth) {
        this.vendor = vendor;
        this.modelName = modelName;
        this.capacity = capacity;
        this.readBandwidth = readBandwidth;
        this.writeBandwidth = writeBandwidth;
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
     * Return the capacity of the device.
     */
    public double getCapacity() {
        return capacity;
    }

    /**
     * Return the read bandwidth of the device in MBps.
     */
    public double getReadBandwidth() {
        return readBandwidth;
    }

    /**
     * Return the write bandwidth of the device in MBps.
     */
    public double getWriteBandwidth() {
        return writeBandwidth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageDevice that = (StorageDevice) o;
        return Double.compare(that.capacity, capacity) == 0
                && Double.compare(that.readBandwidth, readBandwidth) == 0
                && Double.compare(that.writeBandwidth, writeBandwidth) == 0
                && vendor.equals(that.vendor)
                && modelName.equals(that.modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendor, modelName, capacity, readBandwidth, writeBandwidth);
    }

    @Override
    public String toString() {
        return "StorageDevice[vendor='" + vendor + "',modelName='" + modelName + "',capacity=" + capacity
                + ",readBandwidth=" + readBandwidth + ",writeBandwidth=" + writeBandwidth + "]";
    }
}
