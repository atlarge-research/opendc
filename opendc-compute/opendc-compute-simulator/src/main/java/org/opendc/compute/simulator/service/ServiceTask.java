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
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
    private final UUID uid;

    private final String name;
    private final TaskNature nature;
    private final TemporalAmount duration;
    private final Long deadline;
    private ServiceFlavor flavor;
    public Workload workload;

    private Map<String, ?> meta; // TODO: remove this

    private final List<TaskWatcher> watchers = new ArrayList<>();
    private TaskState state = TaskState.CREATED;
    Instant launchedAt = null;
    Instant createdAt;
    Instant finishedAt;
    SimHost host = null;
    private SchedulingRequest request = null;

    private int numFailures = 0;

    ServiceTask(
            ComputeService service,
            UUID uid,
            String name,
            TaskNature nature,
            TemporalAmount duration,
            Long deadline,
            ServiceFlavor flavor,
            Workload workload,
            Map<String, ?> meta) {
        this.service = service;
        this.uid = uid;
        this.name = name;
        this.nature = nature;
        this.duration = duration;
        this.deadline = deadline;
        this.flavor = flavor;
        this.workload = workload;
        this.meta = meta;

        this.createdAt = this.service.getClock().instant();
    }

    @NotNull
    public UUID getUid() {
        return uid;
    }

    @NotNull
    public TaskNature getNature() {
        return nature;
    }

    @NotNull
    public TemporalAmount getDuration() {
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
    public Instant getLaunchedAt() {
        return launchedAt;
    }

    @Nullable
    public Instant getCreatedAt() {
        return createdAt;
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

    public void setHost(SimHost host) {
        this.host = host;
    }

    public int getNumFailures() {
        return this.numFailures;
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
                LOGGER.info("User requested to start task {}", uid);
                setState(TaskState.PROVISIONING);
                assert request == null : "Scheduling request already active";
                request = service.schedule(this);
                break;
            case FAILED:
                LOGGER.info("User requested to start task after failure {}", uid);
                setState(TaskState.PROVISIONING);
                request = service.schedule(this);
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
        return service.equals(task.service) && uid.equals(task.uid);
    }

    public int hashCode() {
        return Objects.hash(service, uid);
    }

    public String toString() {
        return "Task[uid=" + uid + ",name=" + name + ",state=" + state + "]";
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
        }

        if ((newState == TaskState.COMPLETED) || newState == TaskState.FAILED) {
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
