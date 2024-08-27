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

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opendc.common.Dispatcher;
import org.opendc.common.util.Pacer;
import org.opendc.compute.api.ComputeClient;
import org.opendc.compute.api.Flavor;
import org.opendc.compute.api.Image;
import org.opendc.compute.api.Task;
import org.opendc.compute.api.TaskState;
import org.opendc.compute.service.driver.Host;
import org.opendc.compute.service.driver.HostListener;
import org.opendc.compute.service.driver.HostModel;
import org.opendc.compute.service.driver.HostState;
import org.opendc.compute.service.scheduler.ComputeScheduler;
import org.opendc.compute.service.telemetry.SchedulerStats;
import org.opendc.simulator.compute.workload.SimWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ComputeService} hosts the API implementation of the OpenDC Compute Engine.
 */
public final class ComputeService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeService.class);

    /**
     * The {@link InstantSource} representing the clock tracking the (simulation) time.
     */
    private final InstantSource clock;

    /**
     * The {@link ComputeScheduler} responsible for placing the tasks onto hosts.
     */
    private final ComputeScheduler scheduler;

    /**
     * The {@link Pacer} used to pace the scheduling requests.
     */
    private final Pacer pacer;

    /**
     * The {@link SplittableRandom} used to generate the unique identifiers for the service resources.
     */
    private final SplittableRandom random = new SplittableRandom(0);

    /**
     * A flag to indicate that the service is closed.
     */
    private boolean isClosed;

    /**
     * A mapping from host to host view.
     */
    private final Map<Host, HostView> hostToView = new HashMap<>();

    /**
     * The available hypervisors.
     */
    private final Set<HostView> availableHosts = new HashSet<>();

    /**
     * The tasks that should be launched by the service.
     */
    private final Deque<SchedulingRequest> taskQueue = new ArrayDeque<>();

    /**
     * The active tasks in the system.
     */
    private final Map<Task, Host> activeTasks = new HashMap<>();

    /**
     * The registered flavors for this compute service.
     */
    private final Map<UUID, ServiceFlavor> flavorById = new HashMap<>();

    private final List<ServiceFlavor> flavors = new ArrayList<>();

    /**
     * The registered images for this compute service.
     */
    private final Map<UUID, ServiceImage> imageById = new HashMap<>();

    private final List<ServiceImage> images = new ArrayList<>();

    /**
     * The registered tasks for this compute service.
     */
    private final Map<UUID, ServiceTask> taskById = new HashMap<>();

    private final List<ServiceTask> tasks = new ArrayList<>();

    /**
     * A [HostListener] used to track the active tasks.
     */
    private final HostListener hostListener = new HostListener() {
        @Override
        public void onStateChanged(@NotNull Host host, @NotNull HostState newState) {
            LOGGER.debug("Host {} state changed: {}", host, newState);

            final HostView hv = hostToView.get(host);

            if (hv != null) {
                if (newState == HostState.UP) {
                    availableHosts.add(hv);
                } else {
                    availableHosts.remove(hv);
                }
            }

            // Re-schedule on the new machine
            requestSchedulingCycle();
        }

        @Override
        public void onStateChanged(@NotNull Host host, @NotNull Task task, @NotNull TaskState newState) {
            final ServiceTask serviceTask = (ServiceTask) task;

            if (serviceTask.getHost() != host) {
                // This can happen when a task is rescheduled and started on another machine, while being deleted from
                // the old machine.
                return;
            }

            serviceTask.setState(newState);

            if (newState == TaskState.TERMINATED || newState == TaskState.DELETED || newState == TaskState.ERROR) {
                LOGGER.info("task {} {} {} finished", task.getUid(), task.getName(), task.getFlavor());

                if (activeTasks.remove(task) != null) {
                    tasksActive--;
                }

                HostView hv = hostToView.get(host);
                final ServiceFlavor flavor = serviceTask.getFlavor();
                if (hv != null) {
                    hv.provisionedCores -= flavor.getCoreCount();
                    hv.instanceCount--;
                    hv.availableMemory += flavor.getMemorySize();
                } else {
                    LOGGER.error("Unknown host {}", host);
                }

                // Try to reschedule if needed
                requestSchedulingCycle();
            }
        }
    };

    private int maxCores = 0;
    private long maxMemory = 0L;
    private long attemptsSuccess = 0L;
    private long attemptsFailure = 0L;
    private long attemptsError = 0L;
    private int tasksPending = 0;
    private int tasksActive = 0;

    /**
     * Construct a {@link ComputeService} instance.
     */
    ComputeService(Dispatcher dispatcher, ComputeScheduler scheduler, Duration quantum) {
        this.clock = dispatcher.getTimeSource();
        this.scheduler = scheduler;
        this.pacer = new Pacer(dispatcher, quantum.toMillis(), (time) -> doSchedule());
    }

    /**
     * Create a new {@link Builder} instance.
     */
    public static Builder builder(Dispatcher dispatcher, ComputeScheduler scheduler) {
        return new Builder(dispatcher, scheduler);
    }

    /**
     * Create a new {@link ComputeClient} to control the compute service.
     */
    public ComputeClient newClient() {
        if (isClosed) {
            throw new IllegalStateException("Service is closed");
        }
        return new Client(this);
    }

    /**
     * Return the {@link Task}s hosted by this service.
     */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Add a {@link Host} to the scheduling pool of the compute service.
     */
    public void addHost(Host host) {
        // Check if host is already known
        if (hostToView.containsKey(host)) {
            return;
        }

        HostView hv = new HostView(host);
        HostModel model = host.getModel();

        maxCores = Math.max(maxCores, model.coreCount());
        maxMemory = Math.max(maxMemory, model.memoryCapacity());
        hostToView.put(host, hv);

        if (host.getState() == HostState.UP) {
            availableHosts.add(hv);
        }

        scheduler.addHost(hv);
        host.addListener(hostListener);
    }

    /**
     * Remove a {@link Host} from the scheduling pool of the compute service.
     */
    public void removeHost(Host host) {
        HostView view = hostToView.remove(host);
        if (view != null) {
            availableHosts.remove(view);
            scheduler.removeHost(view);
            host.removeListener(hostListener);
        }
    }

    /**
     * Lookup the {@link Host} that currently hosts the specified {@link Task}.
     */
    public Host lookupHost(Task task) {
        if (task instanceof ServiceTask) {
            return ((ServiceTask) task).getHost();
        }

        ServiceTask internal = Objects.requireNonNull(taskById.get(task.getUid()), "Invalid task passed to lookupHost");
        return internal.getHost();
    }

    /**
     * Return the {@link Host}s that are registered with this service.
     */
    public Set<Host> getHosts() {
        return Collections.unmodifiableSet(hostToView.keySet());
    }

    /**
     * Collect the statistics about the scheduler component of this service.
     */
    public SchedulerStats getSchedulerStats() {
        return new SchedulerStats(
                availableHosts.size(),
                hostToView.size() - availableHosts.size(),
                attemptsSuccess,
                attemptsFailure,
                attemptsError,
                tasks.size(),
                tasksPending,
                tasksActive);
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }

        isClosed = true;
        pacer.cancel();
    }

    /**
     * Enqueue the specified [task] to be scheduled onto a host.
     */
    SchedulingRequest schedule(ServiceTask task) {
        LOGGER.debug("Enqueueing task {} to be assigned to host", task.getUid());

        long now = clock.millis();
        SchedulingRequest request = new SchedulingRequest(task, now);

        task.launchedAt = Instant.ofEpochMilli(now);
        taskQueue.add(request);
        tasksPending++;
        requestSchedulingCycle();
        return request;
    }

    void delete(ServiceFlavor flavor) {
        flavorById.remove(flavor.getUid());
        flavors.remove(flavor);
    }

    void delete(ServiceImage image) {
        imageById.remove(image.getUid());
        images.remove(image);
    }

    void delete(ServiceTask task) {
        taskById.remove(task.getUid());
        tasks.remove(task);
    }

    /**
     * Indicate that a new scheduling cycle is needed due to a change to the service's state.
     */
    private void requestSchedulingCycle() {
        // Bail out in case the queue is empty.
        if (taskQueue.isEmpty()) {
            return;
        }

        pacer.enqueue();
    }

    /**
     * Run a single scheduling iteration.
     */
    private void doSchedule() {
        // reorder tasks

        while (!taskQueue.isEmpty()) {
            SchedulingRequest request = taskQueue.peek();

            if (request.isCancelled) {
                taskQueue.poll();
                tasksPending--;
                continue;
            }

            final ServiceTask task = request.task;
            // Check if all dependencies are met
            // otherwise continue

            final ServiceFlavor flavor = task.getFlavor();
            final HostView hv = scheduler.select(request.task);

            if (hv == null || !hv.getHost().canFit(task)) {
                LOGGER.trace("Task {} selected for scheduling but no capacity available for it at the moment", task);

                if (flavor.getMemorySize() > maxMemory || flavor.getCoreCount() > maxCores) {
                    // Remove the incoming image
                    taskQueue.poll();
                    tasksPending--;
                    attemptsFailure++;

                    LOGGER.warn("Failed to spawn {}: does not fit", task);

                    task.setState(TaskState.TERMINATED);
                    continue;
                } else {
                    break;
                }
            }

            Host host = hv.getHost();

            // Remove request from queue
            taskQueue.poll();
            tasksPending--;

            LOGGER.info("Assigned task {} to host {}", task, host);

            try {
                task.host = host;

                host.spawn(task);
                host.start(task);

                tasksActive++;
                attemptsSuccess++;

                hv.instanceCount++;
                hv.provisionedCores += flavor.getCoreCount();
                hv.availableMemory -= flavor.getMemorySize();

                activeTasks.put(task, host);
            } catch (Exception cause) {
                LOGGER.error("Failed to deploy VM", cause);
                attemptsError++;
            }
        }
    }

    /**
     * Builder class for a {@link ComputeService}.
     */
    public static class Builder {
        private final Dispatcher dispatcher;
        private final ComputeScheduler computeScheduler;
        private Duration quantum = Duration.ofMinutes(5);

        Builder(Dispatcher dispatcher, ComputeScheduler computeScheduler) {
            this.dispatcher = dispatcher;
            this.computeScheduler = computeScheduler;
        }

        /**
         * Set the scheduling quantum of the service.
         */
        public Builder withQuantum(Duration quantum) {
            this.quantum = quantum;
            return this;
        }

        /**
         * Build a {@link ComputeService}.
         */
        public ComputeService build() {
            return new ComputeService(dispatcher, computeScheduler, quantum);
        }
    }

    /**
     * Implementation of {@link ComputeClient} using a {@link ComputeService}.
     */
    private static class Client implements ComputeClient {
        private final ComputeService service;
        private boolean isClosed;

        Client(ComputeService service) {
            this.service = service;
        }

        /**
         * Method to check if the client is still open and throw an exception if it is not.
         */
        private void checkOpen() {
            if (isClosed) {
                throw new IllegalStateException("Client is already closed");
            }
        }

        @NotNull
        @Override
        public List<Flavor> queryFlavors() {
            checkOpen();
            return new ArrayList<>(service.flavors);
        }

        @Override
        public Flavor findFlavor(@NotNull UUID id) {
            checkOpen();

            return service.flavorById.get(id);
        }

        @NotNull
        @Override
        public Flavor newFlavor(
                @NotNull String name,
                int cpuCount,
                long memorySize,
                @NotNull Map<String, String> labels,
                @NotNull Map<String, ?> meta) {
            checkOpen();

            final ComputeService service = this.service;
            UUID uid = new UUID(service.clock.millis(), service.random.nextLong());
            ServiceFlavor flavor = new ServiceFlavor(service, uid, name, cpuCount, memorySize, labels, meta);

            service.flavorById.put(uid, flavor);
            service.flavors.add(flavor);

            return flavor;
        }

        @NotNull
        @Override
        public List<Image> queryImages() {
            checkOpen();

            return new ArrayList<>(service.images);
        }

        @Override
        public Image findImage(@NotNull UUID id) {
            checkOpen();

            return service.imageById.get(id);
        }

        @NotNull
        public Image newImage(@NotNull String name, @NotNull Map<String, String> labels, @NotNull Map<String, ?> meta) {
            checkOpen();

            final ComputeService service = this.service;
            UUID uid = new UUID(service.clock.millis(), service.random.nextLong());

            ServiceImage image = new ServiceImage(service, uid, name, labels, meta);

            service.imageById.put(uid, image);
            service.images.add(image);

            return image;
        }

        @NotNull
        @Override
        public Task newTask(
                @NotNull String name,
                @NotNull Image image,
                @NotNull Flavor flavor,
                @NotNull Map<String, String> labels,
                @NotNull Map<String, ?> meta,
                boolean start) {
            checkOpen();

            final ComputeService service = this.service;
            UUID uid = new UUID(service.clock.millis(), service.random.nextLong());

            final ServiceFlavor internalFlavor =
                    Objects.requireNonNull(service.flavorById.get(flavor.getUid()), "Unknown flavor");
            final ServiceImage internalImage =
                    Objects.requireNonNull(service.imageById.get(image.getUid()), "Unknown image");

            ServiceTask task = new ServiceTask(service, uid, name, internalFlavor, internalImage, labels, meta);

            service.taskById.put(uid, task);
            service.tasks.add(task);

            if (start) {
                task.start();
            }

            return task;
        }

        @Nullable
        @Override
        public Task findTask(@NotNull UUID id) {
            checkOpen();
            return service.taskById.get(id);
        }

        @NotNull
        @Override
        public List<Task> queryTasks() {
            checkOpen();

            return new ArrayList<>(service.tasks);
        }

        @Override
        public void close() {
            isClosed = true;
        }

        @Override
        public String toString() {
            return "ComputeService.Client";
        }

        @Nullable
        @Override
        public void rescheduleTask(@NotNull Task task, @NotNull SimWorkload workload) {
            ServiceTask internalTask = (ServiceTask) findTask(task.getUid());
            Host from = service.lookupHost(internalTask);

            from.delete(internalTask);

            internalTask.host = null;

            internalTask.setWorkload(workload);
            internalTask.start();
        }
    }

    /**
     * A request to schedule a {@link ServiceTask} onto one of the {@link Host}s.
     */
    static class SchedulingRequest {
        final ServiceTask task;
        final long submitTime;

        boolean isCancelled;

        SchedulingRequest(ServiceTask task, long submitTime) {
            this.task = task;
            this.submitTime = submitTime;
        }
    }
}
