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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.opendc.compute.api.TaskState;
import org.opendc.compute.simulator.TaskWatcher;
import org.opendc.compute.simulator.host.SimHost;
import org.opendc.compute.simulator.scheduler.SchedulingRequest;
import org.opendc.simulator.compute.workload.Workload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ServiceTask} provided by {@link ComputeService}.
 */
public class ServiceTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTask.class);

    public ComputeService getService() {
        return service;
    }

    public void setService(ComputeService service) {
        this.service = service;
    }

    private ComputeService service;

    public boolean isDeferrable() {
        return deferrable;
    }

    private final int id;
    private final ArrayList<Integer> parents;
    private final Set<Integer> children;

    private final String name;
    private final boolean deferrable;

    private final long duration;
    private long deadline;
    public Workload workload;

    private final int cpuCoreCount;
    private final double cpuCapacity;

    public double getTotalCPULoad() {
        return totalCPULoad;
    }

    private final double totalCPULoad;
    private final long memorySize;
    private final int gpuCoreCount;
    private final double gpuCapacity;

    private final List<TaskWatcher> watchers = new ArrayList<>(1);
    private int stateOrdinal = TaskState.CREATED.ordinal();
    private long submittedAt;
    private long scheduledAt;
    private long finishedAt;
    private SimHost host = null;
    private String hostName = null;

    private SchedulingRequest request = null;

    private int numFailures = 0;
    private int numPauses = 0;

    public ServiceTask(
            int id,
            String name,
            long submissionTime,
            long duration,
            int cpuCoreCount,
            double cpuCapacity,
            double totalCPULoad,
            long memorySize,
            int gpuCoreCount,
            double gpuCapacity,
            long gpuMemorySize,
            Workload workload,
            boolean deferrable,
            long deadline,
            ArrayList<Integer> parents,
            Set<Integer> children) {
        //        this.service = service;
        this.id = id;
        this.name = name;
        this.deferrable = deferrable;
        this.duration = duration;
        this.deadline = deadline;
        this.workload = workload;

        this.cpuCoreCount = cpuCoreCount;
        this.cpuCapacity = cpuCapacity;
        this.totalCPULoad = totalCPULoad;
        this.memorySize = memorySize;
        this.gpuCoreCount = gpuCoreCount;
        this.gpuCapacity = gpuCapacity;

        this.parents = parents;
        this.children = children;

        this.submittedAt = submissionTime;
    }

    public int getId() {
        return id;
    }

    public int getCpuCoreCount() {
        return cpuCoreCount;
    }

    public double getCpuCapacity() {
        return cpuCapacity;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public int getGpuCoreCount() {
        return gpuCoreCount;
    }

    public double getGpuCapacity() {
        return gpuCapacity;
    }

    public boolean getDeferrable() {
        return deferrable;
    }

    public long getDuration() {
        return duration;
    }

    @NotNull
    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setWorkload(Workload newWorkload) {
        this.workload = newWorkload;
    }

    @NotNull
    public TaskState getState() {
        return TaskState.getEntries().get(stateOrdinal);
    }

    public void setScheduledAt(long scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void setFinishedAt(long finishedAt) {
        this.finishedAt = finishedAt;
    }

    public long getScheduledAt() {
        return scheduledAt;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    /**
     * Return the {@link SimHost} on which the task is running or <code>null</code> if it is not running on a host.
     */
    public SimHost getHost() {
        return host;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHost(SimHost newHost) {
        this.host = newHost;
        if (newHost != null) {
            this.hostName = newHost.getName();
        }
    }

    public int getNumFailures() {
        return this.numFailures;
    }

    public int getNumPauses() {
        return this.numPauses;
    }

    public void start() {
        switch (this.getState()) {
            case PROVISIONING:
                LOGGER.debug("User tried to start task but request is already pending: doing nothing");
            case RUNNING:
                LOGGER.debug("User tried to start task but task is already running");
                break;
            case COMPLETED:
            case TERMINATED:
                LOGGER.warn("User tried to start deleted task");
                throw new IllegalStateException("Task is deleted");
            case CREATED:
                LOGGER.info("User requested to start task {}", id);
                setState(TaskState.PROVISIONING);
                assert request == null : "Scheduling request already active";
                request = service.schedule(this);
                break;
            case PAUSED:
                LOGGER.info("User requested to start task after pause {}", id);
                setState(TaskState.PROVISIONING);
                request = service.schedule(this, true);
                break;
            case FAILED:
                LOGGER.info("User requested to start task after failure {}", id);
                setState(TaskState.PROVISIONING);
                request = service.schedule(this, true);
                break;
        }
    }

    public void watch(@NotNull TaskWatcher watcher) {
        watchers.add(watcher);
    }

    public void unwatch(@NotNull TaskWatcher watcher) {
        watchers.remove(watcher);
    }

    public void delete() {
        cancelProvisioningRequest();
        final SimHost host = this.host;
        if (host != null) {
            host.delete(this);
        }
        service.delete(this);

        this.workload = null;

        this.setState(TaskState.DELETED);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceTask task = (ServiceTask) o;
        return service.equals(task.service) && id == task.id;
    }

    public int hashCode() {
        return Objects.hash(service, id);
    }

    public String toString() {
        return "Task[uid=" + this.id + ",name=" + this.name + ",state=" + this.getState() + "]";
    }

    void setState(TaskState newState) {
        if (this.getState() == newState) {
            return;
        }

        for (TaskWatcher watcher : watchers) {
            watcher.onStateChanged(this, newState);
        }
        if (newState == TaskState.FAILED) {
            this.numFailures++;
        } else if (newState == TaskState.PAUSED) {
            this.numPauses++;
        }

        if ((newState == TaskState.COMPLETED) || (newState == TaskState.FAILED) || (newState == TaskState.TERMINATED)) {
            this.finishedAt = this.service.getClock().millis();
        }

        this.stateOrdinal = newState.ordinal();
    }

    /**
     * Cancel the provisioning request if active.
     */
    private void cancelProvisioningRequest() {
        final SchedulingRequest request = this.request;
        if (request != null) {
            this.request = null;
            request.setCancelled(true);
        }
    }

    public void removeFromParents(List<Integer> completedTasks) {
        if (this.parents == null) {
            return;
        }

        for (int task : completedTasks) {
            this.removeFromParents(task);
        }
    }

    public void removeFromParents(int completedTask) {
        if (this.parents == null) {
            return;
        }

        this.parents.remove(completedTask);
    }

    public ArrayList<Integer> getParents() {
        return parents;
    }

    public boolean hasChildren() {
        if (children == null) {
            return false;
        }

        return !children.isEmpty();
    }

    public boolean hasParents() {
        if (parents == null) {
            return false;
        }

        return !parents.isEmpty();
    }

    public Set<Integer> getChildren() {
        return children;
    }
}
