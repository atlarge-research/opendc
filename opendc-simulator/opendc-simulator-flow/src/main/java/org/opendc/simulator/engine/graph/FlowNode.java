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
import java.util.List;
import java.util.Map;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.engine.FlowEventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FlowNode} represents a node in the {@link FlowEngine}.
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

    public Boolean getInCycleQueue() {
        return inCycleQueue;
    }

    public void setInCycleQueue(Boolean inCycleQueue) {
        this.inCycleQueue = inCycleQueue;
    }

    public InstantSource getClock() {
        return clock;
    }

    public void setClock(InstantSource clock) {
        this.clock = clock;
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
     * The index of the timer in the {@link FlowEventQueue}.
     */
    private int timerIndex = -1;

    private Boolean inCycleQueue = false;

    protected InstantSource clock;
    protected FlowEngine engine;

    /**
     * Construct a new {@link FlowNode} instance.
     *
     * @param engine The {@link FlowEngine} driving the simulation.
     */
    public FlowNode(FlowEngine engine) {
        this.engine = engine;
        this.clock = engine.getClock();

        this.invalidate();
    }

    public abstract Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges();

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

        if (this.nodeState == NodeState.CLOSING || this.nodeState == NodeState.CLOSED || this.nodeState == NodeState.INVALIDATED) {
            return;
        }

        this.nodeState = NodeState.INVALIDATED;
        engine.scheduleImmediate(now, this);
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
        if (this.nodeState == NodeState.CLOSED) {
            this.deadline = Long.MAX_VALUE;
            return;
        }

        this.nodeState = NodeState.UPDATING;

        long newDeadline = this.deadline;

        try {
            newDeadline = this.onUpdate(now);
        } catch (Exception e) {
            doFail(e);
        }

        if (this.nodeState == NodeState.CLOSING) {
            closeNode();
            return;
        }

        // Check whether the stage is marked as closing.
        if ((this.nodeState == NodeState.INVALIDATED) || (this.nodeState == NodeState.CLOSED)) {
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
            return;
        }

        // Mark the stage as closed
        this.nodeState = NodeState.CLOSED;

        // Get Connected Edges
        Map<FlowEdge.NodeType, List<FlowEdge>> connectedEdges = getConnectedEdges();

        // Remove connected edges
        List<FlowEdge> consumerEdges = connectedEdges.get(FlowEdge.NodeType.CONSUMING);
        if (consumerEdges != null) {
            for (FlowEdge edge : consumerEdges) {
                edge.close(FlowEdge.NodeType.CONSUMING);
            }
        }

        // Remove connected edges
        List<FlowEdge> supplierEdges = connectedEdges.get(FlowEdge.NodeType.SUPPLYING);

        if (supplierEdges != null) {
            for (FlowEdge edge : supplierEdges) {
                edge.close(FlowEdge.NodeType.SUPPLYING);
            }
        }

        // Remove stage from the timer queue
        this.deadline = Long.MAX_VALUE;
        this.engine.scheduleDelayedInContext(this);
    }
}
