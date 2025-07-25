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
import java.time.InstantSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opendc.common.Dispatcher;
import org.opendc.common.util.Pacer;
import org.opendc.compute.api.Flavor;
import org.opendc.compute.api.TaskState;
import org.opendc.compute.simulator.host.HostListener;
import org.opendc.compute.simulator.host.HostModel;
import org.opendc.compute.simulator.host.HostState;
import org.opendc.compute.simulator.host.SimHost;
import org.opendc.compute.simulator.scheduler.ComputeScheduler;
import org.opendc.compute.simulator.scheduler.SchedulingRequest;
import org.opendc.compute.simulator.scheduler.SchedulingResult;
import org.opendc.compute.simulator.scheduler.SchedulingResultType;
import org.opendc.compute.simulator.telemetry.ComputeMetricReader;
import org.opendc.compute.simulator.telemetry.SchedulerStats;
import org.opendc.simulator.compute.power.CarbonModel;
import org.opendc.simulator.compute.power.CarbonReceiver;
import org.opendc.simulator.compute.power.SimPowerSource;
import org.opendc.simulator.compute.power.batteries.SimBattery;
import org.opendc.simulator.compute.workload.Workload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ComputeService} hosts the API implementation of the OpenDC Compute Engine.
 */
public final class ComputeService implements AutoCloseable, CarbonReceiver {
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

    private final int maxNumFailures;

    /**
     * A flag to indicate that the service is closed.
     */
    private boolean isClosed;

    /**
     * A mapping from host to host view.
     */
    private final Map<SimHost, HostView> hostToView = new HashMap<>();

    /**
     * The available hypervisors.
     */
    private final Set<HostView> availableHosts = new HashSet<>();

    /**
     * The available powerSources
     */
    private final Set<SimPowerSource> powerSources = new HashSet<>();

    /**
     * The available powerSources
     */
    private final Set<SimBattery> batteries = new HashSet<>();

    /**
     * The tasks that should be launched by the service.
     */
    private final Deque<SchedulingRequest> taskQueue = new ArrayDeque<>();

    private final List<SchedulingRequest> blockedTasks = new ArrayList<>();

    /**
     * The active tasks in the system.
     */
    private final Map<ServiceTask, SimHost> activeTasks = new HashMap<>();

    private final List<Integer> completedTasks = new ArrayList<>();

    private final List<Integer> terminatedTasks = new ArrayList<>();

    /**
     * The registered flavors for this compute service.
     */
    private final Map<Integer, ServiceFlavor> flavorById = new HashMap<>();

    private final List<ServiceFlavor> flavors = new ArrayList<>();

    /**
     * The registered tasks for this compute service.
     */
    private final Map<Integer, ServiceTask> taskById = new HashMap<>();

    private final List<ServiceTask> tasksToRemove = new ArrayList<>();

    private ComputeMetricReader metricReader;

    /**
     * A [HostListener] used to track the active tasks.
     */
    private final HostListener hostListener = new HostListener() {
        @Override
        public void onStateChanged(@NotNull SimHost host, @NotNull HostState newState) {
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
        public void onStateChanged(@NotNull SimHost host, @NotNull ServiceTask task, @NotNull TaskState newState) {
            if (task.getHost() != host) {
                // This can happen when a task is rescheduled and started on another machine, while being deleted from
                // the old machine.
                return;
            }

            task.setState(newState);

            if (newState == TaskState.COMPLETED
                    || newState == TaskState.PAUSED
                    || newState == TaskState.TERMINATED
                    || newState == TaskState.FAILED) {
                LOGGER.info("task {} {} {} finished", task.getId(), task.getName(), task.getFlavor());

                if (activeTasks.remove(task) != null) {
                    tasksActive--;
                }

                HostView hv = hostToView.get(host);
                final ServiceFlavor flavor = task.getFlavor();
                if (hv != null) {
                    hv.provisionedCpuCores -= flavor.getCpuCoreCount();
                    hv.instanceCount--;
                    hv.availableMemory += flavor.getMemorySize();
                } else {
                    LOGGER.error("Unknown host {}", host);
                }

                host.delete(task);

                if (newState == TaskState.COMPLETED) {
                    tasksCompleted++;
                    addCompletedTask(task);
                }
                if (newState == TaskState.TERMINATED) {
                    tasksTerminated++;
                    addTerminatedTask(task);
                }

                if (task.getState() == TaskState.COMPLETED || task.getState() == TaskState.TERMINATED) {
                    setTaskToBeRemoved(task);
                }

                scheduler.removeTask(task, hv);

                // Try to reschedule if needed
                requestSchedulingCycle();
            }
        }
    };

    private int maxCores = 0;
    private long maxMemory = 0L;
    private long attemptsSuccess = 0L;
    private long attemptsFailure = 0L;
    private int tasksExpected = 0; // Number of tasks expected from the input trace
    private int tasksTotal = 0; // Number of tasks seen by the service
    private int tasksPending = 0; // Number of tasks waiting to be scheduled
    private int tasksActive = 0; // Number of tasks that are currently running
    private int tasksTerminated = 0; // Number of tasks that were terminated due to too much failures
    private int tasksCompleted = 0; // Number of tasks completed successfully

    /**
     * Construct a {@link ComputeService} instance.
     */
    public ComputeService(Dispatcher dispatcher, ComputeScheduler scheduler, Duration quantum, int maxNumFailures) {
        this.clock = dispatcher.getTimeSource();
        this.scheduler = scheduler;
        this.pacer = new Pacer(dispatcher, quantum.toMillis(), (time) -> doSchedule());
        this.maxNumFailures = maxNumFailures;
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
        return new ComputeClient(this);
    }

    /**
     * Return the {@link ServiceTask}s hosted by this service.
     */
    public Map<Integer, ServiceTask> getTasks() {
        return taskById;
    }

    /**
     * Return the {@link ServiceTask}s hosted by this service.
     */
    public List<ServiceTask> getTasksToRemove() {
        return Collections.unmodifiableList(tasksToRemove);
    }

    public void clearTasksToRemove() {
        this.tasksToRemove.clear();
    }

    /**
     * Add a {@link SimHost} to the scheduling pool of the compute service.
     */
    public void addHost(SimHost host) {
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

    public void addPowerSource(SimPowerSource simPowerSource) {
        // Check if host is already known
        if (powerSources.contains(simPowerSource)) {
            return;
        }

        powerSources.add(simPowerSource);
    }

    public void addBattery(SimBattery simBattery) {
        // Check if host is already known
        if (batteries.contains(simBattery)) {
            return;
        }

        batteries.add(simBattery);
    }

    /**
     * Remove a {@link SimHost} from the scheduling pool of the compute service.
     */
    public void removeHost(SimHost host) {
        HostView view = hostToView.remove(host);
        if (view != null) {
            availableHosts.remove(view);
            scheduler.removeHost(view);
            host.removeListener(hostListener);
        }
    }

    /**
     * Lookup the {@link SimHost} that currently hosts the specified {@link ServiceTask}.
     */
    public SimHost lookupHost(ServiceTask task) {
        return task.getHost();
    }

    /**
     * Return the {@link SimHost}s that are registered with this service.
     */
    public Set<SimHost> getHosts() {
        return Collections.unmodifiableSet(hostToView.keySet());
    }

    public InstantSource getClock() {
        return this.clock;
    }

    public Set<SimPowerSource> getPowerSources() {
        return Collections.unmodifiableSet(this.powerSources);
    }

    public Set<SimBattery> getBatteries() {
        return Collections.unmodifiableSet(this.batteries);
    }

    public void setMetricReader(ComputeMetricReader metricReader) {
        this.metricReader = metricReader;
    }

    public void setTasksExpected(int numberOfTasks) {
        this.tasksExpected = numberOfTasks;
    }

    public void setTaskToBeRemoved(ServiceTask task) {
        this.tasksToRemove.add(task);
        if ((this.tasksTerminated + this.tasksCompleted) == this.tasksExpected) {
            if (this.metricReader != null) {
                this.metricReader
                        .loggState(); // Logg the state for the final time. This will also delete all remaining tasks.
            }
        }
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
                tasksTotal,
                tasksPending,
                tasksActive,
                tasksCompleted,
                tasksTerminated);
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
        return schedule(task, false);
    }

    SchedulingRequest schedule(ServiceTask task, boolean atFront) {
        LOGGER.debug("Enqueueing task {} to be assigned to host", task.getId());

        if (task.getNumFailures() >= maxNumFailures) {
            LOGGER.warn("task {} has been terminated because it failed {} times", task, task.getNumFailures());

            tasksTerminated++;
            task.setState(TaskState.TERMINATED);

            this.setTaskToBeRemoved(task);
            return null;
        }

        long now = clock.millis();
        SchedulingRequest request = new SchedulingRequest(task, now);

        ServiceFlavor flavor = task.getFlavor();
        for (int taskId : this.terminatedTasks) {
            if (flavor.isInDependencies(taskId)) {
                // Terminate task
                task.setState(TaskState.TERMINATED);
            }
        }

        // Remove all completed tasks from the pending dependencies
        flavor.updatePendingDependencies(this.completedTasks);

        // If there are still pending dependencies, we cannot schedule the task yet
        Set<Integer> pendingDependencies = flavor.getDependencies();
        if (!pendingDependencies.isEmpty()) {
            // If the task has pending dependencies, we cannot schedule it yet
            LOGGER.debug("Task {} has pending dependencies: {}", task.getId(), pendingDependencies);
            blockedTasks.add(request);
            return null;
        }

        // Add the request at the front or the back of the queue
        if (atFront) taskQueue.addFirst(request);
        else taskQueue.add(request);

        tasksPending++;

        requestSchedulingCycle();
        return request;
    }

    void addCompletedTask(ServiceTask task) {
        int taskId = task.getId();

        if (!this.completedTasks.contains(taskId)) {
            this.completedTasks.add(taskId);
        }

        List<SchedulingRequest> requestsToRemove = new ArrayList<>();

        for (SchedulingRequest request : blockedTasks) {
            request.getTask().getFlavor().updatePendingDependencies(taskId);

            Set<Integer> pendingDependencies = request.getTask().getFlavor().getDependencies();

            if (pendingDependencies.isEmpty()) {
                requestsToRemove.add(request);
                taskQueue.add(request);
                tasksPending++;
            }
        }

        for (SchedulingRequest request : requestsToRemove) {
            blockedTasks.remove(request);
        }
    }

    void addTerminatedTask(ServiceTask task) {
        int taskId = task.getId();

        List<SchedulingRequest> requestsToRemove = new ArrayList<>();

        if (!this.terminatedTasks.contains(taskId)) {
            this.terminatedTasks.add(taskId);
        }

        for (SchedulingRequest request : blockedTasks) {
            if (request.getTask().getFlavor().isInDependencies(taskId)) {
                requestsToRemove.add(request);
                request.getTask().setState(TaskState.TERMINATED);
            }
        }

        for (SchedulingRequest request : requestsToRemove) {
            blockedTasks.remove(request);
        }
    }

    void delete(ServiceFlavor flavor) {
        flavorById.remove(flavor.getTaskId());
        flavors.remove(flavor);
    }

    void delete(ServiceTask task) {
        completedTasks.remove(task);
        taskById.remove(task.getId());
    }

    public void updateCarbonIntensity(double newCarbonIntensity) {
        requestSchedulingCycle();
    }

    public void setCarbonModel(CarbonModel carbonModel) {}

    public void removeCarbonModel(CarbonModel carbonModel) {}

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
        for (Iterator<SchedulingRequest> iterator = taskQueue.iterator();
                iterator.hasNext();
                iterator = taskQueue.iterator()) {
            final SchedulingResult result = scheduler.select(iterator);
            if (result.getResultType() == SchedulingResultType.EMPTY) {
                break;
            }
            final HostView hv = result.getHost();
            final SchedulingRequest req = result.getReq();
            final ServiceTask task = req.getTask();

            final ServiceFlavor flavor = task.getFlavor();

            if (result.getResultType() == SchedulingResultType.FAILURE) {
                LOGGER.trace("Task {} selected for scheduling but no capacity available for it at the moment", task);

                if (flavor.getMemorySize() > maxMemory || flavor.getCpuCoreCount() > maxCores) {
                    // Remove the incoming image
                    taskQueue.remove(req);
                    tasksPending--;
                    tasksTerminated++;

                    LOGGER.warn("Failed to spawn {}: does not fit", task);

                    task.setState(TaskState.TERMINATED);

                    this.setTaskToBeRemoved(task);
                    continue;
                } else {
                    // VM fits, but we don't have enough capacity
                    break;
                }
            }

            SimHost host = hv.getHost();

            // Remove request from queue
            tasksPending--;

            LOGGER.info("Assigned task {} to host {}", task, host);

            try {
                task.setHost(host);
                task.scheduledAt = clock.instant();

                host.spawn(task);

                tasksActive++;
                attemptsSuccess++;

                hv.instanceCount++;
                hv.provisionedCpuCores += flavor.getCpuCoreCount();
                hv.availableMemory -= flavor.getMemorySize();

                activeTasks.put(task, host);
            } catch (Exception cause) {
                LOGGER.error("Failed to deploy VM", cause);
                scheduler.removeTask(task, hv);
                attemptsFailure++;
            }
        }
    }

    /**
     * Builder class for a {@link ComputeService}.
     */
    public static class Builder {
        private final Dispatcher dispatcher;
        private final ComputeScheduler computeScheduler;
        private Duration quantum = Duration.ofSeconds(1);
        private int maxNumFailures = 10;

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

        public Builder withMaxNumFailures(int maxNumFailures) {
            this.maxNumFailures = maxNumFailures;
            return this;
        }

        /**
         * Build a {@link ComputeService}.
         */
        public ComputeService build() {
            return new ComputeService(dispatcher, computeScheduler, quantum, maxNumFailures);
        }
    }

    /**
     * Implementation of {@link ComputeClient} using a {@link ComputeService}.
     */
    public static class ComputeClient {
        private final ComputeService service;
        private boolean isClosed;

        ComputeClient(ComputeService service) {
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
        public List<Flavor> queryFlavors() {
            checkOpen();
            return new ArrayList<>(service.flavors);
        }

        @NotNull
        public ServiceFlavor newFlavor(
                int taskId,
                int cpuCount,
                long memorySize,
                int gpuCoreCount,
                @NotNull Set<Integer> parents,
                @NotNull Set<Integer> children,
                @NotNull Map<String, ?> meta) {
            checkOpen();

            final ComputeService service = this.service;

            return new ServiceFlavor(service, taskId, cpuCount, memorySize, gpuCoreCount, parents, children, meta);
        }

        @NotNull
        public ServiceTask newTask(
                int id,
                @NotNull String name,
                @NotNull TaskNature nature,
                @NotNull Duration duration,
                @NotNull Long deadline,
                @NotNull ServiceFlavor flavor,
                @NotNull Workload workload,
                @NotNull Map<String, ?> meta) {
            checkOpen();

            final ComputeService service = this.service;

            ServiceTask task = new ServiceTask(service, id, name, nature, duration, deadline, flavor, workload, meta);

            service.taskById.put(id, task);

            service.tasksTotal++;

            task.start();

            return task;
        }

        @Nullable
        public ServiceTask findTask(int id) {
            checkOpen();
            return service.taskById.get(id);
        }

        public void close() {
            isClosed = true;
        }

        @Override
        public String toString() {
            return "ComputeService.Client";
        }

        @Nullable
        public void rescheduleTask(@NotNull ServiceTask task, @NotNull Workload workload) {
            ServiceTask internalTask = findTask(task.getId());
            //            SimHost from = service.lookupHost(internalTask);

            //            from.delete(internalTask);

            internalTask.setHost(null);

            internalTask.setWorkload(workload);
            internalTask.start();
        }
    }
}
