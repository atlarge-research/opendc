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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A description of the physical or virtual machine on which a bootable image runs.
 */
public final class MachineModel {
    private final Cpu cpu;
    private final MemoryUnit memory;
    private final List<NetworkAdapter> net;
    private final List<StorageDevice> storage;

    /**
     * Construct a {@link MachineModel} instance.
     *
     * @param cpu The cpu available to the image.
     * @param memory The list of memory units available to the image.
     * @param net A list of network adapters available to the machine.
     * @param storage A list of storage devices available to the machine.
     */
    public MachineModel(Cpu cpu, MemoryUnit memory, Iterable<NetworkAdapter> net, Iterable<StorageDevice> storage) {
        this.cpu = cpu;

        this.memory = memory;

        this.net = new ArrayList<>();
        net.forEach(this.net::add);

        this.storage = new ArrayList<>();
        storage.forEach(this.storage::add);
    }

    public MachineModel(Cpu cpu, MemoryUnit memory) {
        this(cpu, memory, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Construct a {@link MachineModel} instance.
     * A list of the same cpus, are automatically converted to a single CPU with the number of cores of
     * all cpus in the list combined.
     *
     * @param cpus The list of processing units available to the image.
     * @param memory The list of memory units available to the image.
     */
    public MachineModel(
            List<Cpu> cpus, MemoryUnit memory, Iterable<NetworkAdapter> net, Iterable<StorageDevice> storage) {

        this(
                new Cpu(
                        cpus.get(0).getId(),
                        cpus.get(0).getCoreCount() * cpus.size(),
                        cpus.get(0).getCoreSpeed(),
                        cpus.get(0).getVendor(),
                        cpus.get(0).getModelName(),
                        cpus.get(0).getArchitecture()),
                memory,
                net,
                storage);
    }

    public MachineModel(List<Cpu> cpus, MemoryUnit memory) {
        this(cpus, memory, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Return the processing units of this machine.
     */
    public Cpu getCpu() {
        return this.cpu;
    }

    /**
     * Return the memory units of this machine.
     */
    public MemoryUnit getMemory() {
        return memory;
    }

    /**
     * Return the network adapters of this machine.
     */
    public List<NetworkAdapter> getNetwork() {
        return Collections.unmodifiableList(net);
    }

    /**
     * Return the storage devices of this machine.
     */
    public List<StorageDevice> getStorage() {
        return Collections.unmodifiableList(storage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MachineModel that = (MachineModel) o;
        return cpu.equals(that.cpu)
                && memory.equals(that.memory)
                && net.equals(that.net)
                && storage.equals(that.storage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpu, memory, net, storage);
    }

    @Override
    public String toString() {
        return "MachineModel[cpus=" + cpu + ",memory=" + memory + ",net=" + net + ",storage=" + storage + "]";
    }
}
