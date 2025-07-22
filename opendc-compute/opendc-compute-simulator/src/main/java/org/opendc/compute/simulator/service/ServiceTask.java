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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    private final ComputeService service;
    private final int id;

    private final String name;
    private final TaskNature nature;
    private final Duration duration;
    private final Long deadline;
    private ServiceFlavor flavor;
    public Workload workload;

    private final Map<String, ?> meta; // TODO: remove this

    private final List<TaskWatcher> watchers = new ArrayList<>();
    private TaskState state = TaskState.CREATED;
    Instant scheduledAt = null;
    Instant submittedAt;
    Instant finishedAt;
    private SimHost host = null;
    private String hostName = null;

    private SchedulingRequest request = null;

    private int numFailures = 0;
    private int numPauses = 0;

    ServiceTask(
            ComputeService service,
            int id,
            String name,
            TaskNature nature,
            Duration duration,
            Long deadline,
            ServiceFlavor flavor,
            Workload workload,
            Map<String, ?> meta) {
        this.service = service;
        this.id = id;
        this.name = name;
        this.nature = nature;
        this.duration = duration;
        this.deadline = deadline;
        this.flavor = flavor;
        this.workload = workload;
        this.meta = meta;

        this.submittedAt = this.service.getClock().instant();
    }

    public int getId() {
        return id;
    }

    @NotNull
    public TaskNature getNature() {
        return nature;
    }

    @NotNull
    public Duration getDuration() {
        return duration;
    }

    @NotNull
    public Long getDeadline() {
        return deadline;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public ServiceFlavor getFlavor() {
        return flavor;
    }

    @NotNull
    public Map<String, Object> getMeta() {
        return Collections.unmodifiableMap(meta);
    }

    public void setWorkload(Workload newWorkload) {
        this.workload = newWorkload;
    }

    @NotNull
    public TaskState getState() {
        return state;
    }

    @Nullable
    public Instant getScheduledAt() {
        return scheduledAt;
    }

    @Nullable
    public Instant getSubmittedAt() {
        return submittedAt;
    }

    @Nullable
    public Instant getFinishedAt() {
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
        switch (state) {
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
        this.flavor = null;

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
        return "Task[uid=" + id + ",name=" + name + ",state=" + state + "]";
    }

    void setState(TaskState newState) {
        if (this.state == newState) {
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
            this.finishedAt = this.service.getClock().instant();
        }

        this.state = newState;
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
}
