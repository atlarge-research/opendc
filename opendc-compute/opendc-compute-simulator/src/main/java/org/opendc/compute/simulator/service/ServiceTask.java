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
import java.util.HashMap;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;
import org.opendc.compute.api.TaskState;
import org.opendc.compute.simulator.TaskWatcher;
import org.opendc.compute.simulator.host.SimHost;
import org.opendc.compute.simulator.scheduler.SchedulingRequest;
import org.opendc.simulator.compute.workload.Workload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is essentially a WorkflowServiceTask
 */
public class ServiceTask extends ServiceTaskBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTask.class);
    
    // Workflow DAG - specific fields
    private final Set<ServiceTask> wfParents = new HashSet<>();
    private final Set<ServiceTask> wfChildren = new HashSet<>();
    // a field which tracks the dependecy depth of all its children
    // a workflow root identfying field so that we can quickly check if they are part of same dependecy chain somehow so that we can quickly assess the depth and select the required one
    
    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constructor
    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    
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
        // base class constructor
        super(id, name, submissionTime, duration, cpuCoreCount, cpuCapacity,
              totalCPULoad, memorySize, gpuCoreCount, gpuCapacity, gpuMemorySize,
              workload, deferrable, deadline, parents, children);
        
        // DAG fields are initialized inline already above
    }
    
    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    /// Overridden Methods - preserve original behavior + add workflow dag logic
    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    
    // @Override
    // void setState(TaskState newState) {
    //     // Execute base class behavior first
    //     super.setState(newState);
        
    //     // Add DAG-specific logic
    //     if (newState == TaskState.COMPLETED) {
    //         propagateCompletionToChildren();
    //     }
    // }
    
    // @Override
    // public ServiceTask copy() {
    //     // Create a new ServiceTask with base class data
    //     ServiceTask copy = new ServiceTask(
    //         this.getId(),
    //         this.getName(),
    //         this.getSubmittedAt(),
    //         this.getDuration(),
    //         this.getCpuCoreCount(),
    //         this.getCpuCapacity(),
    //         this.getTotalCPULoad(),
    //         this.getMemorySize(),
    //         this.getGpuCoreCount(),
    //         this.getGpuCapacity(),
    //         this.getGpuMemorySize(),
    //         this.getWorkload(),
    //         this.getDeferrable(),
    //         this.getDeadline(),
    //         this.getParents() == null ? null : new ArrayList<>(this.getParents()),
    //         this.getChildren() == null ? null : Set.copyOf(this.getChildren())
    //     );
        
    //     // to copy dag relationship or not?
    //     // completedParentsDepth is initialized to 0 by default
        
    //     return copy;
    // }
    
    // @Override
    // public boolean equals(Object o) {
    //     // Use base class equality (based on service and id)
    //     return super.equals(o);
    // }
    
    // @Override
    // public int hashCode() {
    //     // Use base class hash code
    //     return super.hashCode();
    // }
    
    @Override
    public String toString() {
        return super.toString() + 
               "[wfParents=" + wfParents.size() + 
               ",wfChildren=" + wfChildren.size() 
               //",depth=" + completedParentsDepth + "]"
               ;
    }


    // have to override fns where servicetask needs to be passed to
    // external interfaces instead of serviceTaskBase
    // @Override
    public void setState(TaskState newState) {

        if (this.getState() == newState) {
            return;
        }

        for (TaskWatcher watcher : this.getWatchers()) {
            watcher.onStateChanged(this, newState);   // to pass ServiceTask as this here
        }

        if (newState == TaskState.FAILED) {
            this.setNumFailures(this.getNumFailures() + 1);
        } else if (newState == TaskState.PAUSED) {
            this.setNumPauses(this.getNumPauses() + 1);
        }

        if (newState == TaskState.COMPLETED
                || newState == TaskState.FAILED
                || newState == TaskState.TERMINATED) {
            this.setFinishedAt(this.getService().getClock().millis());
        }

        this.setStateOrdinal(newState.ordinal());
    }

    // @Override
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
                LOGGER.info("User requested to start task {}", this.getId());
                this.setState(TaskState.PROVISIONING);
                assert this.getRequest() == null : "Scheduling request already active";
                this.setRequest(this.getService().schedule(this));
                break;

            case PAUSED:
                LOGGER.info("User requested to start task after pause {}", this.getId());
                this.setState(TaskState.PROVISIONING);
                this.setRequest(this.getService().schedule(this, false));
                break;

            case FAILED:
                LOGGER.info("User requested to start task after failure {}", this.getId());
                this.setState(TaskState.PROVISIONING);
                this.setRequest(this.getService().schedule(this, false));
                break;
        }
    }

    // @Override
    public void delete() {
        cancelProvisioningRequest();
        final SimHost host = this.getHost();
        if (host != null) {
            host.delete(this);
        }

        this.getService().delete(this);
        this.setWorkload(null);
        this.setState(TaskState.DELETED);
    }

    
    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    /// DAG-Specific Methods
    /// //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Connects parent-child dependencies between tasks in the list.
     * establishing the workflow DAG by linking tasks based on their ID references.
     * Doesnt handle if parent/child not in the list, i.e. they never arrrived 
     * 
     * @param tasks The list of tasks to connect
     * @return The same list with dependencies established
     */
    // public static List<ServiceTask> connectDependencies(List<ServiceTask> tasks) {

    //     Map<Integer, ServiceTask> taskMap = new HashMap<>();
    //     for (ServiceTask task : tasks) {
    //         taskMap.put(task.getId(), task);
    //     }
        
    //     // Connect parents and children
    //     for (ServiceTask task : tasks) {
    //         // Connect parents
    //         if (task.getParents() != null) {
    //             for (Integer parentId : task.getParents()) {
    //                 ServiceTask parentTask = taskMap.get(parentId);
    //                 if (parentTask != null) {
    //                     task.addWfParent(parentTask);
    //                 } else {
    //                     LOGGER.warn("Parent task with ID {} not found for task {}", 
    //                                parentId, task.getId());
    //                 }
    //             }
    //         }
            
    //         // Connect children
    //         if (task.getChildren() != null) {
    //             for (Integer childId : task.getChildren()) {
    //                 ServiceTask childTask = taskMap.get(childId);
    //                 if (childTask != null) {
    //                     task.addWfChild(childTask);
    //                 } else {
    //                     LOGGER.warn("Child task with ID {} not found for task {}", 
    //                                childId, task.getId());
    //                 }
    //             }
    //         }
    //     }
    //     return tasks;
    // }

    
    // public void addWfParent(ServiceTask parent) {
    //     this.wfParents.add(parent);
    // }
    
    // public void addWfChild(ServiceTask child) {
    //     this.wfChildren.add(child);
    // }
    
    public Set<ServiceTask> getWfParents() {
        return wfParents;
    }
    
    public Set<ServiceTask> getWfChildren() {
        return wfChildren;
    }

    /**
     * bidirectionally connect this task and a parent task.
     * @param parent The parent task to connect to
     */
    public void connectToParent(ServiceTask parent) {
        this.wfParents.add(parent);
        parent.wfChildren.add(this);
    }
    
    /**
     * bidirectionally connect this task and a child task.
     * @param child The child task to connect to
     */
    public void connectToChild(ServiceTask child) {
        this.wfChildren.add(child);
        child.wfParents.add(this);
    }

    /**
     * Prints this task and recursively prints all its children in a tree structure.
     * @param indent The indentation level for pretty printing
     * @param visited Set of already visited task IDs to handle cycles/shared children
     */
    public void printTaskTree(String indent, Set<Integer> visited) {
        // Print current task
        System.out.println(indent + "├─ Task[id=" + getId() + 
                        ", state=" + getState() + 
                        ", parents=" + (getParents() != null ? getParents().size() : 0) +
                        ", children=" + (getChildren() != null ? getChildren().size() : 0) + "]");
        visited.add(getId());
        
        // print children recursively
        if (hasChildren()) {
            String childIndent = indent + "│  ";
            int childCount = 0;
            
            for (ServiceTask child : wfChildren) {
                childCount++;
                boolean isLast = (childCount == wfChildren.size());
                
                if (visited.contains(child.getId())) {
                    // already printed 
                    System.out.println(childIndent + "├─ Task[id=" + child.getId() + 
                                    "] (already printed above)");
                } else {
                    // recursively print child
                    child.printTaskTree(childIndent, visited);
                }
            }
        }
    }

    /**
     * Prints this task and all its descendants starting from this task(for root case).
     */
    public void printTaskTree() {
        System.out.println("\n=== Task DAG starting from root ===");
        printTaskTree("", new HashSet<>());
        System.out.println("===================================\n");
    }
    
    // /**
    //  * Add a parent dependency to this task.
    //  * This creates a bidirectional relationship.
    //  */
    // public void addDagParent(ServiceTask parent) {
    //     if (parent == null) {
    //         LOGGER.warn("Attempted to add null parent to task {}", getId());
    //         return;
    //     }
    //     wfParents.add(parent);
    //     parent.wfChildren.add(this);
    // }
    
    // /**
    //  * Add a child dependency to this task.
    //  * This creates a bidirectional relationship.
    //  */
    // public void addDagChild(ServiceTask child) {
    //     if (child == null) {
    //         LOGGER.warn("Attempted to add null child to task {}", getId());
    //         return;
    //     }
    //     wfChildren.add(child);
    //     child.wfParents.add(this);
    // }
    
    // /**
    //  * Remove a parent dependency from this task.
    //  */
    // public void removeDagParent(ServiceTask parent) {
    //     if (parent != null) {
    //         wfParents.remove(parent);
    //         parent.wfChildren.remove(this);
    //     }
    // }
    
    // /**
    //  * Remove a child dependency from this task.
    //  */
    // public void removeDagChild(ServiceTask child) {
    //     if (child != null) {
    //         wfChildren.remove(child);
    //         child.wfParents.remove(this);
    //     }
    // }
    
    // /**
    //  * Check if all parent tasks have completed execution.
    //  */
    // public boolean areParentsCompleted() {
    //     for (ServiceTask parent : wfParents) {
    //         if (parent.getState() != TaskState.COMPLETED) {
    //             return false;
    //         }
    //     }
    //     return true;
    // }
    
    /**
     * Check if this task has no parent dependencies (is a root node).
     */
    public boolean isRootTask() {
        return wfParents.isEmpty();
    }
    
    // /**
    //  * Check if this task has no child dependencies (is a leaf node).
    //  */
    // public boolean isLeafTask() {
    //     return wfChildren.isEmpty();
    // }
    
    // /**
    //  * Propagate completion metrics to all child tasks.
    //  * Called automatically when this task completes.
    //  */
    // private void propagateCompletionToChildren() {
    //     for (ServiceTask child : wfChildren) {
    //         child.incrementCompletedParentsDepth();
    //     }
    // }
    
    // /**
    //  * Increment the completed parents depth metric.
    //  * This is called by parent tasks when they complete.
    //  */
    // private void incrementCompletedParentsDepth() {
    //     this.completedParentsDepth++;
    // }
    
    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    /// Getters for DAG Fields
    /// //////////////////////////////////////////////////////////////////////////////////////////////////
    
    // public Set<ServiceTask> getDagParents() {
    //     return Collections.unmodifiableSet(wfParents);
    // }
    
    // public Set<ServiceTask> getDagChildren() {
    //     return Collections.unmodifiableSet(wfChildren);
    // }
    
    // public int getCompletedParentsDepth() {
    //     return completedParentsDepth;
    // }
    
    // /**
    //  * Get the number of parent tasks that have completed.
    //  */
    // public int getNumCompletedParents() {
    //     int count = 0;
    //     for (ServiceTask parent : wfParents) {
    //         if (parent.getState() == TaskState.COMPLETED) {
    //             count++;
    //         }
    //     }
    //     return count;
    // }
    
    // /**
    //  * Get the total number of parent dependencies.
    //  */
    // public int getDagParentCount() {
    //     return wfParents.size();
    // }
    
    // /**
    //  * Get the total number of child dependencies.
    //  */
    // public int getDagChildCount() {
    //     return wfChildren.size();
    // }
}
