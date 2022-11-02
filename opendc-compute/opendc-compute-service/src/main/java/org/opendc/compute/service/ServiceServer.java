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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opendc.compute.api.Server;
import org.opendc.compute.api.ServerState;
import org.opendc.compute.api.ServerWatcher;
import org.opendc.compute.service.driver.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Server} provided by {@link ComputeService}.
 */
public final class ServiceServer implements Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceServer.class);

    private final ComputeService service;
    private final UUID uid;
    private final String name;
    private final ServiceFlavor flavor;
    private final ServiceImage image;
    private final Map<String, String> labels;
    private final Map<String, ?> meta;

    private final List<ServerWatcher> watchers = new ArrayList<>();
    private ServerState state = ServerState.TERMINATED;
    Instant launchedAt = null;
    Host host = null;
    private ComputeService.SchedulingRequest request = null;

    ServiceServer(
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

    @NotNull
    @Override
    public ServerState getState() {
        return state;
    }

    @Nullable
    @Override
    public Instant getLaunchedAt() {
        return launchedAt;
    }

    /**
     * Return the {@link Host} on which the server is running or <code>null</code> if it is not running on a host.
     */
    public Host getHost() {
        return host;
    }

    @Override
    public void start() {
        switch (state) {
            case PROVISIONING:
                LOGGER.debug("User tried to start server but request is already pending: doing nothing");
            case RUNNING:
                LOGGER.debug("User tried to start server but server is already running");
                break;
            case DELETED:
                LOGGER.warn("User tried to start deleted server");
                throw new IllegalStateException("Server is deleted");
            default:
                LOGGER.info("User requested to start server {}", uid);
                setState(ServerState.PROVISIONING);
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
                setState(ServerState.TERMINATED);
                break;
            case RUNNING:
            case ERROR:
                final Host host = this.host;
                if (host == null) {
                    throw new IllegalStateException("Server not running");
                }
                host.stop(this);
                break;
        }
    }

    @Override
    public void watch(@NotNull ServerWatcher watcher) {
        watchers.add(watcher);
    }

    @Override
    public void unwatch(@NotNull ServerWatcher watcher) {
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
                setState(ServerState.DELETED);
                break;
            case RUNNING:
            case ERROR:
                final Host host = this.host;
                if (host == null) {
                    throw new IllegalStateException("Server not running");
                }
                host.delete(this);
                service.delete(this);
                setState(ServerState.DELETED);
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceServer server = (ServiceServer) o;
        return service.equals(server.service) && uid.equals(server.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, uid);
    }

    @Override
    public String toString() {
        return "Server[uid=" + uid + ",name=" + name + ",state=" + state + "]";
    }

    void setState(ServerState state) {
        if (this.state != state) {
            for (ServerWatcher watcher : watchers) {
                watcher.onStateChanged(this, state);
            }
        }

        this.state = state;
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
}
