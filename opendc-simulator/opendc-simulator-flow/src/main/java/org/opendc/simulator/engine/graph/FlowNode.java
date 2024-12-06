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

package org.opendc.simulator.engine.graph;

import java.time.InstantSource;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.engine.FlowTimerQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FlowNode} represents a node in a {@link FlowGraph}.
 */
public abstract class FlowNode {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowNode.class);

    protected enum NodeState {
        PENDING, // Stage is active, but is not running any updates
        UPDATING, // Stage is active, and running an update
        INVALIDATED, // Stage is deemed invalid, and should run an update
        CLOSING, // Stage is being closed, final updates can still be run
        CLOSED // Stage is closed and should not run any updates
    }

    protected NodeState nodeState = NodeState.PENDING;

    public NodeState getNodeState() {
        return nodeState;
    }

    public void setNodeState(NodeState nodeState) {
        this.nodeState = nodeState;
    }

    public int getTimerIndex() {
        return timerIndex;
    }

    public void setTimerIndex(int index) {
        this.timerIndex = index;
    }

    public InstantSource getClock() {
        return clock;
    }

    public void setClock(InstantSource clock) {
        this.clock = clock;
    }

    public FlowGraph getParentGraph() {
        return parentGraph;
    }

    public void setParentGraph(FlowGraph parentGraph) {
        this.parentGraph = parentGraph;
    }

    public FlowEngine getEngine() {
        return engine;
    }

    public void setEngine(FlowEngine engine) {
        this.engine = engine;
    }

    /**
     * Return the current deadline of the {@link FlowNode}'s timer (in milliseconds after epoch).
     */
    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    /**
     * The deadline of the stage after which an update should run.
     */
    private long deadline = Long.MAX_VALUE;

    /**
     * The index of the timer in the {@link FlowTimerQueue}.
     */
    private int timerIndex = -1;

    protected InstantSource clock;
    protected FlowGraph parentGraph;
    protected FlowEngine engine;

    /**
     * Return the {@link FlowGraph} to which this stage belongs.
     */
    public FlowGraph getGraph() {
        return parentGraph;
    }

    /**
     * Construct a new {@link FlowNode} instance.
     *
     * @param parentGraph The {@link FlowGraph} this stage belongs to.
     */
    public FlowNode(FlowGraph parentGraph) {
        this.parentGraph = parentGraph;
        this.engine = parentGraph.getEngine();
        this.clock = engine.getClock();

        this.parentGraph.addNode(this);
    }

    /**
     * Invalidate the {@link FlowNode} forcing the stage to update.
     *
     * <p>
     * This method is similar to {@link #invalidate()}, but allows the user to manually pass the current timestamp to
     * prevent having to re-query the clock. This method should not be called during an update.
     */
    public void invalidate(long now) {
        // If there is already an update running,
        // notify the update, that a next update should be run after
        if (this.nodeState == NodeState.UPDATING) {
            this.nodeState = NodeState.INVALIDATED;
        } else {
            engine.scheduleImmediate(now, this);
        }
    }

    /**
     * Invalidate the {@link FlowNode} forcing the stage to update.
     */
    public void invalidate() {
        invalidate(clock.millis());
    }

    /**
     * Update the state of the stage.
     */
    public void update(long now) {
        this.nodeState = NodeState.UPDATING;

        long newDeadline = this.deadline;

        try {
            newDeadline = this.onUpdate(now);
        } catch (Exception e) {
            doFail(e);
        }

        // Check whether the stage is marked as closing.
        if (this.nodeState == NodeState.INVALIDATED) {
            newDeadline = now;
        }
        if (this.nodeState == NodeState.CLOSING) {
            closeNode();
            return;
        }

        this.deadline = newDeadline;

        // Update the timer queue with the new deadline
        engine.scheduleDelayedInContext(this);

        this.nodeState = NodeState.PENDING;
    }

    /**
     * This method is invoked when the one of the stage's InPorts or OutPorts is invalidated.
     *
     * @param now The virtual timestamp in milliseconds after epoch at which the update is occurring.
     * @return The next deadline for the stage.
     */
    public abstract long onUpdate(long now);

    /**
     * This method is invoked when an uncaught exception is caught by the engine. When this happens, the
     */
    void doFail(Throwable cause) {
        LOGGER.warn("Uncaught exception (closing stage)", cause);

        closeNode();
    }

    /**
     * This method is invoked when the {@link FlowNode} exits successfully or due to failure.
     */
    public void closeNode() {
        if (this.nodeState == NodeState.CLOSED) {
            //            LOGGER.warn("Flowstage:doClose() => Tried closing a stage that was already closed");
            return;
        }

        // If this stage is running an update, notify it that is should close after.
        if (this.nodeState == NodeState.UPDATING) {
            //            LOGGER.warn("Flowstage:doClose() => Tried closing a stage, but update was active");
            this.nodeState = NodeState.CLOSING;
            return;
        }

        // Mark the stage as closed
        this.nodeState = NodeState.CLOSED;

        // Remove stage from parent graph
        this.parentGraph.removeNode(this);

        // Remove stage from the timer queue
        this.deadline = Long.MAX_VALUE;
        this.engine.scheduleDelayedInContext(this);
    }
}
