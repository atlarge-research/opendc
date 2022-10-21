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
    private final List<ProcessingUnit> cpus;
    private final List<MemoryUnit> memory;
    private final List<NetworkAdapter> net;
    private final List<StorageDevice> storage;

    /**
     * Construct a {@link MachineModel} instance.
     *
     * @param cpus The list of processing units available to the image.
     * @param memory The list of memory units available to the image.
     * @param net A list of network adapters available to the machine.
     * @param storage A list of storage devices available to the machine.
     */
    public MachineModel(
            Iterable<ProcessingUnit> cpus,
            Iterable<MemoryUnit> memory,
            Iterable<NetworkAdapter> net,
            Iterable<StorageDevice> storage) {
        this.cpus = new ArrayList<>();
        cpus.forEach(this.cpus::add);

        this.memory = new ArrayList<>();
        memory.forEach(this.memory::add);

        this.net = new ArrayList<>();
        net.forEach(this.net::add);

        this.storage = new ArrayList<>();
        storage.forEach(this.storage::add);
    }

    /**
     * Construct a {@link MachineModel} instance.
     *
     * @param cpus The list of processing units available to the image.
     * @param memory The list of memory units available to the image.
     */
    public MachineModel(Iterable<ProcessingUnit> cpus, Iterable<MemoryUnit> memory) {
        this(cpus, memory, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Optimize the [MachineModel] by merging all resources of the same type into a single resource with the combined
     * capacity. Such configurations can be simulated more efficiently by OpenDC.
     */
    public MachineModel optimize() {
        ProcessingUnit originalCpu = cpus.get(0);

        double freq = 0.0;
        for (ProcessingUnit cpu : cpus) {
            freq += cpu.getFrequency();
        }

        ProcessingNode originalNode = originalCpu.getNode();
        ProcessingNode processingNode = new ProcessingNode(
                originalNode.getVendor(), originalNode.getModelName(), originalNode.getArchitecture(), 1);
        ProcessingUnit processingUnit = new ProcessingUnit(processingNode, originalCpu.getId(), freq);

        long memorySize = 0;
        for (MemoryUnit mem : memory) {
            memorySize += mem.getSize();
        }
        MemoryUnit memoryUnit = new MemoryUnit("Generic", "Generic", 3200.0, memorySize);

        return new MachineModel(List.of(processingUnit), List.of(memoryUnit));
    }

    /**
     * Return the processing units of this machine.
     */
    public List<ProcessingUnit> getCpus() {
        return Collections.unmodifiableList(cpus);
    }

    /**
     * Return the memory units of this machine.
     */
    public List<MemoryUnit> getMemory() {
        return Collections.unmodifiableList(memory);
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
        return cpus.equals(that.cpus)
                && memory.equals(that.memory)
                && net.equals(that.net)
                && storage.equals(that.storage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpus, memory, net, storage);
    }

    @Override
    public String toString() {
        return "MachineModel[cpus=" + cpus + ",memory=" + memory + ",net=" + net + ",storage=" + storage + "]";
    }
}
