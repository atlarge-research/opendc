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

import java.util.ArrayList;
import java.util.Collections;
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

    private final Map<String, ?> meta;

    ServiceFlavor(
            ComputeService service,
            int taskId,
            Map<String, ?> meta) {
        this.service = service;
        this.taskId = taskId;
        this.meta = meta;
    }

    @Override
    public int getCpuCoreCount() {
        return 0;
    }

    @Override
    public long getMemorySize() {
        return 0;
    }

    @Override
    public int getGpuCoreCount() {
        return 0;
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

    public void removeFromParents(List<Integer> completedTasks) {
        for (int task : completedTasks) {
            this.removeFromParents(task);
        }
    }

    public void removeFromParents(int completedTask) {

    }

    public boolean isInDependencies(int task) {
        return false;
    }

    @Override
    public @NotNull ArrayList<Integer> getParents() {
        return new ArrayList<>();
    }

    @Override
    public @NotNull Set<Integer> getChildren() {
        return Collections.emptySet();
    }
}
