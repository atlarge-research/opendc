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
public class ServiceTaskBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskBase.class);

    private ComputeService service;
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
    private final double totalCPULoad;
    private final long memorySize;

    private final int gpuCoreCount;
    private final double gpuCapacity;
    private final long gpuMemorySize;

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

    private long schedulingDelay = 0;

    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    /// Getters and Setters
    /// //////////////////////////////////////////////////////////////////////////////////////////////////

    public ComputeService getService() {
        return service;
    }

    public void setService(ComputeService service) {
        this.service = service;
    }

    public int getId() {
        return id;
    }

    public ArrayList<Integer> getParents() {
        return parents;
    }

    public Set<Integer> getChildren() {
        return children;
    }

    public String getName() {
        return name;
    }

    public boolean getDeferrable() {
        return deferrable;
    }

    public long getDuration() {
        return duration;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public Workload getWorkload() {
        return workload;
    }

    public void setWorkload(Workload workload) {
        this.workload = workload;
    }

    public int getCpuCoreCount() {
        return cpuCoreCount;
    }

    public double getCpuCapacity() {
        return cpuCapacity;
    }

    public double getTotalCPULoad() {
        return totalCPULoad;
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

    public long getGpuMemorySize() {
        return gpuMemorySize;
    }

    public List<TaskWatcher> getWatchers() {
        return watchers;
    }

    @NotNull
    public TaskState getState() {
        return TaskState.getEntries().get(stateOrdinal);
    }

    // void setState(TaskState newState) {
    //     if (this.getState() == newState) {
    //         return;
    //     }

    //     for (TaskWatcher watcher : watchers) {
    //         watcher.onStateChanged(this, newState);
    //     }
    //     if (newState == TaskState.FAILED) {
    //         this.numFailures++;
    //     } else if (newState == TaskState.PAUSED) {
    //         this.numPauses++;
    //     }

    //     if ((newState == TaskState.COMPLETED) || (newState == TaskState.FAILED) || (newState == TaskState.TERMINATED)) {
    //         this.finishedAt = this.service.getClock().millis();
    //     }

    //     this.stateOrdinal = newState.ordinal();
    // }

    public int getStateOrdinal() {
        return stateOrdinal;
    }

    public void setStateOrdinal(int stateOrdinal) {
        this.stateOrdinal = stateOrdinal;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }

    public long getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(long scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(long finishedAt) {
        this.finishedAt = finishedAt;
    }

    public SimHost getHost() {
        return host;
    }

    public void setHost(SimHost newHost) {
        this.host = newHost;
        if (newHost != null) {
            this.setHostName(newHost.getName());
        }
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public SchedulingRequest getRequest() {
        return request;
    }

    public void setRequest(SchedulingRequest request) {
        this.request = request;
    }

    public int getNumFailures() {
        return numFailures;
    }

    public void setNumFailures(int numFailures) {
        this.numFailures = numFailures;
    }

    public int getNumPauses() {
        return numPauses;
    }

    public void setNumPauses(int numPauses) {
        this.numPauses = numPauses;
    }

    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constructor and Public Methods
    /// //////////////////////////////////////////////////////////////////////////////////////////////////

    public ServiceTaskBase(
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
        this.id = id;
        this.name = name;
        this.submittedAt = submissionTime;
        this.duration = duration;
        this.workload = workload;

        this.cpuCoreCount = cpuCoreCount;
        this.cpuCapacity = cpuCapacity;
        this.totalCPULoad = totalCPULoad;
        this.memorySize = memorySize;

        this.gpuCoreCount = gpuCoreCount;
        this.gpuCapacity = gpuCapacity;
        this.gpuMemorySize = gpuMemorySize;

        this.deferrable = deferrable;
        this.deadline = deadline;

        this.parents = parents;
        this.children = children;
    }

    public ServiceTaskBase copy() {
        return new ServiceTaskBase(
                this.id,
                this.name,
                this.submittedAt,
                this.duration,
                this.cpuCoreCount,
                this.cpuCapacity,
                this.totalCPULoad,
                this.memorySize,
                this.gpuCoreCount,
                this.gpuCapacity,
                0,
                this.workload,
                this.deferrable,
                this.deadline,
                this.parents == null ? null : new ArrayList<>(this.parents),
                this.children == null ? null : Set.copyOf(this.children));
    }

    // public void start() {
    //     switch (this.getState()) {
    //         case PROVISIONING:
    //             LOGGER.debug("User tried to start task but request is already pending: doing nothing");
    //         case RUNNING:
    //             LOGGER.debug("User tried to start task but task is already running");
    //             break;
    //         case COMPLETED:
    //         case TERMINATED:
    //             LOGGER.warn("User tried to start deleted task");
    //             throw new IllegalStateException("Task is deleted");
    //         case CREATED:
    //             LOGGER.info("User requested to start task {}", id);
    //             setState(TaskState.PROVISIONING);
    //             assert request == null : "Scheduling request already active";
    //             request = service.schedule(this);
    //             break;
    //         case PAUSED:
    //             LOGGER.info("User requested to start task after pause {}", id);
    //             setState(TaskState.PROVISIONING);
    //             request = service.schedule(this, false);
    //             break;
    //         case FAILED:
    //             LOGGER.info("User requested to start task after failure {}", id);
    //             setState(TaskState.PROVISIONING);
    //             request = service.schedule(this, false);
    //             break;
    //     }
    // }

    public void watch(@NotNull TaskWatcher watcher) {
        watchers.add(watcher);
    }

    public void unwatch(@NotNull TaskWatcher watcher) {
        watchers.remove(watcher);
    }

    // public void delete() {
    //     cancelProvisioningRequest();
    //     final SimHost host = this.host;
    //     if (host != null) {
    //         host.delete(this);
    //     }
    //     service.delete(this);

    //     this.workload = null;

    //     this.setState(TaskState.DELETED);
    // }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceTaskBase task = (ServiceTaskBase) o;
        return service.equals(task.service) && id == task.id;
    }

    public int hashCode() {
        return Objects.hash(service, id);
    }

    public String toString() {
        return "Task[uid=" + this.id + ",name=" + this.name + ",state=" + this.getState() + "]";
    }

    /**
     * Cancel the provisioning request if active.
     */
    protected void cancelProvisioningRequest() {
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

        this.parents.remove(Integer.valueOf(completedTask));
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

    public long getSchedulingDelay() {
        return schedulingDelay;
    }

    public void setSchedulingDelay(long schedulingDelay) {
        this.schedulingDelay = schedulingDelay;
    }
}
