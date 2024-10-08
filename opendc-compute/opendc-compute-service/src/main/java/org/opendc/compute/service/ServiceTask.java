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

package org.opendc.compute.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opendc.compute.api.Task;
import org.opendc.compute.api.TaskState;
import org.opendc.compute.api.TaskWatcher;
import org.opendc.compute.service.driver.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Task} provided by {@link ComputeService}.
 */
public final class ServiceTask implements Task {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTask.class);

    private final ComputeService service;
    private final UUID uid;

    private final String name;
    private final ServiceFlavor flavor;
    private final ServiceImage image;
    private final Map<String, String> labels;
    private Map<String, ?> meta;

    private final List<TaskWatcher> watchers = new ArrayList<>();
    private TaskState state = TaskState.TERMINATED;
    Instant launchedAt = null;
    Host host = null;
    private ComputeService.SchedulingRequest request = null;

    private int numFailures = 0;

    ServiceTask(
            ComputeService service,
            UUID uid,
            String name,
            ServiceFlavor flavor,
            ServiceImage image,
            Map<String, String> labels,
            Map<String, ?> meta) {
        this.service = service;
        this.uid = uid;
        this.name = name;
        this.flavor = flavor;
        this.image = image;
        this.labels = labels;
        this.meta = meta;
    }

    @NotNull
    @Override
    public UUID getUid() {
        return uid;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public ServiceFlavor getFlavor() {
        return flavor;
    }

    @NotNull
    @Override
    public ServiceImage getImage() {
        return image;
    }

    @NotNull
    @Override
    public Map<String, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    @NotNull
    @Override
    public Map<String, Object> getMeta() {
        return Collections.unmodifiableMap(meta);
    }

    public void setWorkload(Object _workload) {
        Map<String, Object> new_meta = new HashMap<String, Object>();
        new_meta.put("workload", _workload);

        meta = new_meta;
    }

    @NotNull
    @Override
    public TaskState getState() {
        return state;
    }

    @Nullable
    @Override
    public Instant getLaunchedAt() {
        return launchedAt;
    }

    /**
     * Return the {@link Host} on which the task is running or <code>null</code> if it is not running on a host.
     */
    public Host getHost() {
        return host;
    }

    @Override
    public void start() {
        switch (state) {
            case PROVISIONING:
                LOGGER.debug("User tried to start task but request is already pending: doing nothing");
            case RUNNING:
                LOGGER.debug("User tried to start task but task is already running");
                break;
            case DELETED:
                LOGGER.warn("User tried to start deleted task");
                throw new IllegalStateException("Task is deleted");
            default:
                LOGGER.info("User requested to start task {}", uid);
                setState(TaskState.PROVISIONING);
                assert request == null : "Scheduling request already active";
                request = service.schedule(this);
                break;
        }
    }

    @Override
    public void stop() {
        switch (state) {
            case PROVISIONING:
                cancelProvisioningRequest();
                setState(TaskState.TERMINATED);
                break;
            case RUNNING:
            case ERROR:
                final Host host = this.host;
                if (host == null) {
                    throw new IllegalStateException("Task not running");
                }
                host.stop(this);
                break;
        }
    }

    @Override
    public void watch(@NotNull TaskWatcher watcher) {
        watchers.add(watcher);
    }

    @Override
    public void unwatch(@NotNull TaskWatcher watcher) {
        watchers.remove(watcher);
    }

    @Override
    public void reload() {
        // No-op: this object is the source-of-truth
    }

    @Override
    public void delete() {
        switch (state) {
            case PROVISIONING:
            case TERMINATED:
                cancelProvisioningRequest();
                service.delete(this);
                setState(TaskState.DELETED);
                break;
            case RUNNING:
            case ERROR:
                final Host host = this.host;
                if (host == null) {
                    throw new IllegalStateException("Task not running");
                }
                host.delete(this);
                service.delete(this);
                setState(TaskState.DELETED);
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceTask task = (ServiceTask) o;
        return service.equals(task.service) && uid.equals(task.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, uid);
    }

    @Override
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
        if (newState == TaskState.ERROR) {
            this.numFailures++;
        }

        this.state = newState;
    }

    /**
     * Cancel the provisioning request if active.
     */
    private void cancelProvisioningRequest() {
        final ComputeService.SchedulingRequest request = this.request;
        if (request != null) {
            this.request = null;
            request.isCancelled = true;
        }
    }

    @Override
    public int getNumFailures() {
        return this.numFailures;
    }
}
