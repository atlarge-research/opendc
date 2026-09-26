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
import org.opendc.compute.api.TaskState;
import org.opendc.compute.simulator.cluster.SimCluster;
import org.opendc.compute.simulator.datacenter.SimDataCenter;
import org.opendc.compute.simulator.infrastructure.HostListener;
import org.opendc.compute.simulator.infrastructure.HostModel;
import org.opendc.compute.simulator.infrastructure.HostState;
import org.opendc.compute.simulator.infrastructure.SimHost;
import org.opendc.compute.simulator.scheduler.ComputeScheduler;
import org.opendc.compute.simulator.scheduler.SchedulingRequest;
import org.opendc.compute.simulator.scheduler.SchedulingResult;
import org.opendc.compute.simulator.scheduler.SchedulingResultType;
import org.opendc.compute.simulator.telemetry.TaskListener;
import org.opendc.simulator.compute.carbon.CarbonModel;
import org.opendc.simulator.compute.carbon.CarbonReceiver;
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

    // ==================================================================================
    // Fields
    // Internal state: infrastructure registries, task bookkeeping, and scheduling counters.
    // ==================================================================================

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
     * The hosts registered with this service.
     */
    private final Set<SimHost> hosts = new HashSet<>();

    /**
     * The available hypervisors.
     */
    private final Set<SimHost> availableHosts = new HashSet<>();

    /**
     * The available clusters
     */
    private final Set<SimCluster> clusters = new HashSet<>();

    /**
     * The available dataCenters
     */
    private final Set<SimDataCenter> dataCenters = new HashSet<>();

    /**
     * The available powerSources
     */
    private final Set<SimPowerSource> powerSources = new HashSet<>();

    /**
     * The available batteries
     */
    private final Set<SimBattery> batteries = new HashSet<>();

    /**
     * The tasks that should be launched by the service.
     */
    private final Deque<SchedulingRequest> taskQueue = new ArrayDeque<>();

    private final Map<Integer, SchedulingRequest> blockedTasks = new HashMap<>();

    /**
     * The active tasks in the system.
     */
    private final Set<SimTask> activeTasks = new HashSet<>();

    /**
     * The registered tasks for this compute service.
     */
    private final Map<Integer, SimTask> taskById = new HashMap<>();

    private final List<TaskListener> taskListeners = new ArrayList<>();

    private int maxCores = 0;
    private long maxMemory = 0L;
    private long attemptsSuccess = 0L;
    private long attemptsFailure = 0L;
    private int tasksTotal = 0; // Number of tasks seen by the service
    private int tasksTerminated = 0; // Number of tasks that were terminated due to too much failures
    private int tasksCompleted = 0; // Number of tasks completed successfully

    // ==================================================================================
    // Construction
    // How to obtain a ComputeService instance: the constructor, and the Builder that wraps it.
    // ==================================================================================

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
     * Builder class for a {@link ComputeService}.
     */
    public static class Builder {
        private final Dispatcher dispatcher;
        private final ComputeScheduler computeScheduler;
        private Duration quantum = Duration.ofMillis(1);
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

    // ==================================================================================
    // Lifecycle
    // All functions related to changing the ComputeService state during its lifecycle
    // ==================================================================================

    @Override
    public void close() {
        if (isClosed) {
            return;
        }

        isClosed = true;
        pacer.cancel();
    }

    // ==================================================================================
    // Task API
    // Public surface for submitting, looking up, and rescheduling tasks.
    // ==================================================================================

    /**
     * Submit a {@link SimTask} to be scheduled by this service.
     */
    @NotNull
    public SimTask submitTask(SimTask task) {
        if (isClosed) {
            throw new IllegalStateException("Service is closed");
        }

        task.setService(this);

        taskById.put(task.getId(), task);

        tasksTotal++;

        task.start();

        return task;
    }

    /**
     * Find the {@link SimTask} with the specified id, or {@code null} if no such task exists.
     */
    @Nullable
    public SimTask findTask(int id) {
        return taskById.get(id);
    }

    /**
     * Reschedule the given {@link SimTask} with a new {@link Workload}.
     */
    public void rescheduleTask(@NotNull SimTask task, @NotNull Workload workload) {
        task.reschedule(workload);
    }

    /**
     * Return the {@link SimTask}s hosted by this service.
     */
    public Map<Integer, SimTask> getTasks() {
        return Collections.unmodifiableMap(taskById);
    }

    /**
     * Notify the listeners that the given {@link SimTask} is done, and delete it.
     */
    public void deleteTask(SimTask task) {
        for (TaskListener listener : this.taskListeners) {
            listener.onTaskDeletion(task);
        }

        task.delete();
    }

    public void addTaskListener(TaskListener listener) {
        this.taskListeners.add(listener);
    }

    // ==================================================================================
    // Host management
    // Registering hosts with the scheduling pool and reacting to their availability.
    // ==================================================================================

    /**
     * Add a {@link SimHost} to the scheduling pool of the compute service.
     */
    public void addHost(SimHost host) {
        // Check if host is already known
        if (hosts.contains(host)) {
            return;
        }

        HostModel model = host.getModel();

        maxCores = Math.max(maxCores, model.coreCount());
        maxMemory = Math.max(maxMemory, model.memoryCapacity());
        hosts.add(host);

        if (host.getState() == HostState.UP) {
            availableHosts.add(host);
        }

        scheduler.addHost(host);
        host.addListener(hostListener);
    }

    /**
     * Remove a {@link SimHost} from the scheduling pool of the compute service.
     */
    public void removeHost(SimHost host) {
        if (hosts.remove(host)) {
            availableHosts.remove(host);
            scheduler.removeHost(host);
            host.removeListener(hostListener);
        }
    }

    public void updateHost(SimHost host) {
        if (!hosts.contains(host)) {
            return;
        }

        this.scheduler.updateHost(host);
    }

    public void failHost(SimHost host) {
        this.scheduler.failHost(host);
    }

    public void restartHost(SimHost host) {
        this.scheduler.restartHost(host);
    }

    /**
     * Return the {@link SimHost}s that are registered with this service.
     */
    public Set<SimHost> getHosts() {
        return Collections.unmodifiableSet(hosts);
    }

    // ==================================================================================
    // Power & energy infrastructure
    // Registering the clusters, data centers, power sources, and batteries backing the service.
    // ==================================================================================

    public void addCluster(SimCluster cluster) {
        this.clusters.add(cluster);
    }

    public void removeCluster(SimCluster cluster) {
        this.clusters.remove(cluster);
    }

    public void addDataCenter(SimDataCenter dataCenter) {
        this.dataCenters.add(dataCenter);
    }

    public void removeDataCenter(SimDataCenter dataCenter) {
        this.dataCenters.remove(dataCenter);
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

    public Set<SimCluster> getClusters() {
        return Collections.unmodifiableSet(this.clusters);
    }

    public Set<SimDataCenter> getDataCenters() {
        return Collections.unmodifiableSet(this.dataCenters);
    }

    public Set<SimPowerSource> getPowerSources() {
        return Collections.unmodifiableSet(this.powerSources);
    }

    public Set<SimBattery> getBatteries() {
        return Collections.unmodifiableSet(this.batteries);
    }

    // ==================================================================================
    // Carbon receiver
    // Implementation of CarbonReceiver; carbon intensity itself is modeled per power source.
    // ==================================================================================

    @Override
    public void updateCarbonIntensity(double newCarbonIntensity) {
        requestSchedulingCycle();
    }

    // ComputeService does not hold a carbon model itself; carbon intensity is modeled per power source.
    @Override
    public void setCarbonModel(CarbonModel carbonModel) {}

    @Override
    public void removeCarbonModel(CarbonModel carbonModel) {}

    // ==================================================================================
    // Statistics
    // Read-only counters exposing scheduling and task outcomes, for monitoring.
    // ==================================================================================

    public int getHostsAvailable() {
        return this.availableHosts.size();
    }

    public int getHostsUnavailable() {
        return this.hosts.size() - this.availableHosts.size();
    }

    public long getAttemptsSuccess() {
        return this.attemptsSuccess;
    }

    public long getAttemptsFailure() {
        return this.attemptsFailure;
    }

    public int getTasksTotal() {
        return this.tasksTotal;
    }

    public int getTasksPending() {
        return this.taskQueue.size();
    }

    public int getTasksActive() {
        return this.activeTasks.size();
    }

    public int getTasksCompleted() {
        return this.tasksCompleted;
    }

    public int getTasksTerminated() {
        return this.tasksTerminated;
    }

    public InstantSource getClock() {
        return this.clock;
    }

    // ==================================================================================
    // Scheduling internals
    // Package-private machinery that queues, selects, and deploys tasks onto hosts.
    // ==================================================================================

    /**
     * A [HostListener] used to track the active tasks.
     */
    private final HostListener hostListener = new HostListener() {
        @Override
        public void onStateChanged(@NotNull SimHost host, @NotNull HostState newState) {
            LOGGER.debug("Host {} state changed: {}", host, newState);

            if (hosts.contains(host)) {
                if (newState == HostState.UP) {
                    availableHosts.add(host);
                    restartHost(host);
                } else {
                    availableHosts.remove(host);
                    failHost(host);
                }
            }

            // Re-schedule on the new machine
            requestSchedulingCycle();
        }

        @Override
        public void onStateChanged(@NotNull SimHost host, @NotNull SimTask task, @NotNull TaskState newState) {
            if (task.getHost() != host) {
                // This can happen when a task is rescheduled and started on another machine, while being deleted from
                // the old machine.
                return;
            }

            if (newState == TaskState.COMPLETED
                    || newState == TaskState.PAUSED
                    || newState == TaskState.TERMINATED
                    || newState == TaskState.FAILED) {
                LOGGER.info("task {} finished", task.getId());

                activeTasks.remove(task);

                final boolean isKnownHost = hosts.contains(host);
                if (!isKnownHost) {
                    LOGGER.error("Unknown host {}", host);
                }

                // Deleting the task also releases the capacity it reserved on the host
                host.delete(task);

                updateHost(host);

                if (newState == TaskState.COMPLETED) {
                    tasksCompleted++;
                    addCompletedTask(task);
                }
                if (newState == TaskState.TERMINATED) {
                    tasksTerminated++;
                    addTerminatedTask(task);
                }

                if (task.getState() == TaskState.COMPLETED || task.getState() == TaskState.TERMINATED) {
                    deleteTask(task);
                }

                scheduler.removeTask(task, isKnownHost ? host : null);

                // Try to reschedule if needed
                requestSchedulingCycle();
            }
        }
    };

    /**
     * Enqueue the specified [task] to be scheduled onto a host.
     */
    SchedulingRequest schedule(SimTask task) {
        return schedule(task, false);
    }

    SchedulingRequest schedule(SimTask task, boolean atFront) {
        LOGGER.debug("Enqueueing task {} to be assigned to host", task.getId());

        if (task.getNumFailures() >= maxNumFailures) {
            LOGGER.warn("task {} has been terminated because it failed {} times", task, task.getNumFailures());

            tasksTerminated++;
            task.terminate();

            this.addTerminatedTask(task);

            this.deleteTask(task);
            return null;
        }

        long now = clock.millis();
        SchedulingRequest request = new SchedulingRequest(task, now);

        // If the task has parents, put in blocked tasks
        if (task.hasParents()) {
            blockedTasks.put(task.getId(), request);
            return null;
        }

        // Add the request at the front or the back of the queue
        if (atFront) taskQueue.addFirst(request);
        else taskQueue.add(request);

        requestSchedulingCycle();
        return request;
    }

    void addCompletedTask(SimTask completedTask) {
        int parentId = completedTask.getId();

        if (!completedTask.hasChildren()) {
            return;
        }

        for (int childTaskId : completedTask.getChildren()) {
            SchedulingRequest childRequest = blockedTasks.get(childTaskId);
            if (childRequest != null) {
                SimTask childTask = childRequest.getTask();
                childTask.removeFromParents(parentId);

                // If the child task has no more parents, it can be scheduled
                if (!childTask.hasParents()) {
                    taskQueue.add(childRequest);
                    blockedTasks.remove(childTaskId);
                }
            }
        }
    }

    void addTerminatedTask(SimTask task) {

        if (!task.hasChildren()) {
            return;
        }

        for (int childTaskId : task.getChildren()) {
            SchedulingRequest request = blockedTasks.get(childTaskId);
            if (request != null) {
                SimTask childTask = request.getTask();

                tasksTerminated++;
                childTask.terminate();

                this.addTerminatedTask(childTask);

                this.deleteTask(childTask);

                blockedTasks.remove(childTask.getId());
            }
        }
    }

    /**
     * Unregister a {@link SimTask} that has deleted itself.
     */
    void unregisterTask(SimTask task) {
        taskById.remove(task.getId());
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
        for (Iterator<SchedulingRequest> iterator = taskQueue.iterator();
                iterator.hasNext();
                iterator = taskQueue.iterator()) {

            final SchedulingResult result = scheduler.select(iterator);
            if (result.getResultType() == SchedulingResultType.EMPTY) {
                break;
            }

            final SchedulingRequest req = result.getReq();
            final SimTask task = req.getTask();

            if (result.getResultType() == SchedulingResultType.FAILURE) {
                LOGGER.trace("Task {} selected for scheduling but no capacity available for it at the moment", task);

                // Check if the task will every fit on any of the hosts.
                // If not, terminate the host
                if (task.getMemorySize() > this.maxMemory || task.getCpuCoreCount() > this.maxCores) {
                    terminateOversizedTask(task, req);
                    continue;
                } else {
                    break;
                }
            }

            deployTask(task, result.getHost(), req);
        }
    }

    /**
     * Terminate a task that exceeds the capacity of every host, and remove it from the queue.
     */
    private void terminateOversizedTask(SimTask task, SchedulingRequest req) {
        // Remove the incoming image
        taskQueue.remove(req);
        tasksTerminated++;

        LOGGER.warn("Failed to spawn {}: does not fit", task);

        task.terminate();

        this.addTerminatedTask(task);

        this.deleteTask(task);
    }

    /**
     * Deploy the given task onto the host selected for it.
     */
    private void deployTask(SimTask task, SimHost host, SchedulingRequest req) {
        LOGGER.info("Assigned task {} to host {}", task, host);

        try {
            task.onScheduled(host, req.getSubmitTime());

            host.spawn(task);

            attemptsSuccess++;

            activeTasks.add(task);

            updateHost(host);
        } catch (Exception cause) {
            LOGGER.error("Failed to deploy VM", cause);
            scheduler.removeTask(task, host);
            attemptsFailure++;
        }
    }
}
