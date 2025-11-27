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

import java.util.Set;
import java.util.HashSet;
import org.opendc.compute.api.TaskState;

public class DagNode {

    private ServiceTask task;

    // Parents and children stored as DagNode references.
    // These reflect the edges in the DAG.
    private final Set<DagNode> parents = new HashSet<>();
    private final Set<DagNode> children = new HashSet<>();

    public DagNode(ServiceTask task) {
        this.task = task;
    }

    // -------- Basic Accessors --------

    public ServiceTask getTask() {
        return task;
    }

    public Set<DagNode> getParents() {
        return parents;
    }

    public Set<DagNode> getChildren() {
        return children;
    }

    public int getTaskId() {
        return task.getId();
    }

    public void setTask(ServiceTask task) {
    this.task = task;
    }


    // -------- DAG Operations --------

    /** Add a parent edge (bidirectional). */
    public void addParent(DagNode parentNode) {
        if (parentNode == null) return;
        parents.add(parentNode);
        parentNode.children.add(this);
    }

    /** Add a child edge (bidirectional). */
    public void addChild(DagNode childNode) {
        if (childNode == null) return;
        children.add(childNode);
        childNode.parents.add(this);
    }

    /** True if all parent tasks have finished execution. */
    public boolean areParentsCompleted() {
        for (DagNode p : parents) {
            if (p.getTask().getState() != TaskState.COMPLETED) {   
                return false;
            }
        }
        return true;
    }

    /** Whether the node has no parent dependencies. */
    public boolean isRoot() {
        return parents.isEmpty();
    }

    /** Whether no tasks depend on this task. */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public String toString() {
        return "DagNode(taskId=" + task.getId() + ")";
    }
}
