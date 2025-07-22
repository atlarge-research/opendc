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

package org.opendc.compute.simulator.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.opendc.compute.api.Flavor;

/**
 * Implementation of {@link Flavor} provided by {@link ComputeService}.
 */
public final class ServiceFlavor implements Flavor {
    private final ComputeService service;
    private final int taskId;
    private final int cpuCoreCount;
    private final long memorySize;
    private final int gpuCoreCount;
    private final Set<Integer> parents;
    private final Set<Integer> children;
    private final Set<Integer> dependencies;
    private final Map<String, ?> meta;

    ServiceFlavor(
            ComputeService service,
            int taskId,
            int cpuCoreCount,
            long memorySize,
            int gpuCoreCount,
            Set<Integer> parents,
            Set<Integer> children,
            Map<String, ?> meta) {
        this.service = service;
        this.taskId = taskId;
        this.cpuCoreCount = cpuCoreCount;
        this.memorySize = memorySize;
        this.gpuCoreCount = gpuCoreCount;
        this.parents = parents;
        this.dependencies = new HashSet<>(parents);
        this.children = children;
        this.meta = meta;
    }

    @Override
    public int getCpuCoreCount() {
        return cpuCoreCount;
    }

    @Override
    public long getMemorySize() {
        return memorySize;
    }

    @Override
    public int getGpuCoreCount() {
        return gpuCoreCount;
    }

    @Override
    public int getTaskId() {
        return taskId;
    }

    @NotNull
    @Override
    public Map<String, Object> getMeta() {
        return Collections.unmodifiableMap(meta);
    }

    @Override
    public void reload() {
        // No-op: this object is the source-of-truth
    }

    @Override
    public void delete() {
        service.delete(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceFlavor flavor = (ServiceFlavor) o;
        return service.equals(flavor.service) && taskId == flavor.taskId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, taskId);
    }

    @Override
    public String toString() {
        return "Flavor[name=" + taskId + "]";
    }

    @Override
    public @NotNull Set<Integer> getDependencies() {
        return dependencies;
    }

    public void updatePendingDependencies(List<Integer> completedTasks) {
        for (int task : completedTasks) {
            this.updatePendingDependencies(task);
        }
    }

    public void updatePendingDependencies(int completedTask) {
        this.dependencies.remove(completedTask);
    }

    public boolean isInDependencies(int task) {
        return this.dependencies.contains(task);
    }

    @Override
    public @NotNull Set<Integer> getParents() {
        return parents;
    }

    @Override
    public @NotNull Set<Integer> getChildren() {
        return children;
    }
}
