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

import java.util.Arrays;
import java.util.List;
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

    private ComputeService service;
    private final int id;

    /**
     * Ids of the parent tasks that must complete before this task may start.
     * {@code null} means no (remaining) parents. Stored as a primitive array (instead of a
     * boxed {@code List<Integer>}) to avoid per-element boxing and collection overhead, since
     * a large fraction of tasks in a workload have no dependencies at all.
     */
    private int[] parents;

    /**
     * Ids of the child tasks that depend on this task. {@code null} means no children.
     * Never mutated after construction, so unlike {@link #parents} this can stay {@code final}.
     */
    private final int[] children;

    private final boolean deferrable;

    private final long duration;
    private long deadline;
    public Workload workload;

    private final short cpuCoreCount;
    private final double cpuCapacity;
    private final int memorySize;

    private final short gpuCoreCount;
    private final double gpuCapacity;
    private final int gpuMemorySize;

    /**
     * A task only ever has a single watcher in practice, so this is stored directly instead of
     * in a {@code List}, avoiding an extra {@code ArrayList} + backing array allocation per task.
     */
    private TaskWatcher watcher = null;

    private byte stateOrdinal = (byte) TaskState.CREATED.ordinal();
    private long submittedAt;
    private long scheduledAt;
    private long finishedAt;
    private SimHost host = null;
    private String hostName = null;
    // TODO: This is currently needed because host gets deleted before the final exporting. When exporting has been
    // updated, remove hostName.

    private SchedulingRequest request = null;

    private short numFailures = 0;
    private short numPauses = 0;

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

    public int[] getParents() {
        return parents;
    }

    public int[] getChildren() {
        return children;
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

    @NotNull
    public TaskState getState() {
        return TaskState.getEntries().get(stateOrdinal);
    }

    void setState(TaskState newState) {
        if (this.getState() == newState) {
            return;
        }

        if (watcher != null) {
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

        this.stateOrdinal = (byte) newState.ordinal();
    }

    public int getStateOrdinal() {
        return stateOrdinal;
    }

    public void setStateOrdinal(int stateOrdinal) {
        this.stateOrdinal = (byte) stateOrdinal;
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
        this.numFailures = (short) numFailures;
    }

    public int getNumPauses() {
        return numPauses;
    }

    public void setNumPauses(int numPauses) {
        this.numPauses = (short) numPauses;
    }

    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constructor and Public Methods
    /// //////////////////////////////////////////////////////////////////////////////////////////////////

    public ServiceTask(
            int id,
            long submissionTime,
            long duration,
            int cpuCoreCount,
            double cpuCapacity,
            long memorySize,
            int gpuCoreCount,
            double gpuCapacity,
            long gpuMemorySize,
            Workload workload,
            boolean deferrable,
            long deadline,
            int[] parents,
            int[] children) {
        this.id = id;
        this.submittedAt = submissionTime;
        this.duration = duration;
        this.workload = workload;

        this.cpuCoreCount = (short) cpuCoreCount;
        this.cpuCapacity = cpuCapacity;
        this.memorySize = (int) memorySize;

        this.gpuCoreCount = (short) gpuCoreCount;
        this.gpuCapacity = gpuCapacity;
        this.gpuMemorySize = (int) gpuMemorySize;

        this.deferrable = deferrable;
        this.deadline = deadline;

        this.parents = (parents == null || parents.length == 0) ? null : parents;
        this.children = (children == null || children.length == 0) ? null : children;
    }

    public ServiceTask copy() {
        return new ServiceTask(
                this.id,
                this.submittedAt,
                this.duration,
                this.cpuCoreCount,
                this.cpuCapacity,
                this.memorySize,
                this.gpuCoreCount,
                this.gpuCapacity,
                0,
                this.workload,
                this.deferrable,
                this.deadline,
                this.parents == null ? null : Arrays.copyOf(this.parents, this.parents.length),
                this.children == null ? null : Arrays.copyOf(this.children, this.children.length));
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
                request = service.schedule(this, false);
                break;
            case FAILED:
                LOGGER.info("User requested to start task after failure {}", id);
                setState(TaskState.PROVISIONING);
                request = service.schedule(this, false);
                break;
        }
    }

    public void watch(@NotNull TaskWatcher watcher) {
        this.watcher = watcher;
    }

    public void unwatch(@NotNull TaskWatcher watcher) {
        if (this.watcher == watcher) {
            this.watcher = null;
        }
    }

    public void delete() {
        cancelProvisioningRequest();
        final SimHost host = this.host;
        if (host != null) {
            host.delete(this);
        }
        service.delete(this);

        this.workload = null;

        if (this.watcher != null) {
            this.unwatch(this.watcher);
        }

        this.setState(TaskState.DELETED);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceTask task = (ServiceTask) o;
        return service.equals(task.service) && id == task.id;
    }

    public int hashCode() {
        // Deliberately not Objects.hash(service, id): that allocates a varargs array and boxes the id
        // on every call, and tasks are used as HashMap keys on hot lookup paths. Ids are unique within
        // a service, so they alone satisfy the equals/hashCode contract.
        return id;
    }

    public String toString() {
        return "Task[uid=" + this.id + ",state=" + this.getState() + "]";
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
        int[] current = this.parents;
        if (current == null) {
            return;
        }

        int idx = -1;
        for (int i = 0; i < current.length; i++) {
            if (current[i] == completedTask) {
                idx = i;
                break;
            }
        }

        if (idx == -1) {
            return;
        }

        if (current.length == 1) {
            this.parents = null;
            return;
        }

        int[] updated = new int[current.length - 1];
        System.arraycopy(current, 0, updated, 0, idx);
        System.arraycopy(current, idx + 1, updated, idx, current.length - idx - 1);
        this.parents = updated;
    }

    public boolean hasChildren() {
        return children != null && children.length > 0;
    }

    public boolean hasParents() {
        return parents != null && parents.length > 0;
    }

    public long getSchedulingDelay() {
        return schedulingDelay;
    }

    public void setSchedulingDelay(long schedulingDelay) {
        this.schedulingDelay = schedulingDelay;
    }
}
