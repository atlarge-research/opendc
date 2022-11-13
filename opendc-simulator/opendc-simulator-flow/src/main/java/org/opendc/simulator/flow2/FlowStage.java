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

package org.opendc.simulator.flow2;

import java.time.InstantSource;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FlowStage} represents a node in a {@link FlowGraph}.
 */
public final class FlowStage {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowStage.class);

    /**
     * States of the flow stage.
     */
    private static final int STAGE_PENDING = 0; // Stage is pending to be started

    private static final int STAGE_ACTIVE = 1; // Stage is actively running
    private static final int STAGE_CLOSED = 2; // Stage is closed
    private static final int STAGE_STATE = 0b11; // Mask for accessing the state of the flow stage

    /**
     * Flags of the flow connection
     */
    private static final int STAGE_INVALIDATE = 1 << 2; // The stage is invalidated

    private static final int STAGE_CLOSE = 1 << 3; // The stage should be closed
    private static final int STAGE_UPDATE_ACTIVE = 1 << 4; // An update for the connection is active
    private static final int STAGE_UPDATE_PENDING = 1 << 5; // An (immediate) update of the connection is pending

    /**
     * The flags representing the state and pending actions for the stage.
     */
    private int flags = STAGE_PENDING;

    /**
     * The deadline of the stage after which an update should run.
     */
    long deadline = Long.MAX_VALUE;

    /**
     * The index of the timer in the {@link FlowTimerQueue}.
     */
    int timerIndex = -1;

    final InstantSource clock;
    private final FlowStageLogic logic;
    final FlowGraphInternal parentGraph;
    private final FlowEngine engine;

    private final Map<String, InPort> inlets = new HashMap<>();
    private final Map<String, OutPort> outlets = new HashMap<>();
    private int nextInlet = 0;
    private int nextOutlet = 0;

    /**
     * Construct a new {@link FlowStage} instance.
     *
     * @param parentGraph The {@link FlowGraph} this stage belongs to.
     * @param logic The logic of the stage.
     */
    FlowStage(FlowGraphInternal parentGraph, FlowStageLogic logic) {
        this.parentGraph = parentGraph;
        this.logic = logic;
        this.engine = parentGraph.getEngine();
        this.clock = engine.getClock();
    }

    /**
     * Return the {@link FlowGraph} to which this stage belongs.
     */
    public FlowGraph getGraph() {
        return parentGraph;
    }

    /**
     * Return the {@link Inlet} (an in-going edge) with the specified <code>name</code> for this {@link FlowStage}.
     * If an inlet with that name does not exist, a new one is allocated for the stage.
     *
     * @param name The name of the inlet.
     * @return The {@link InPort} representing an {@link Inlet} with the specified <code>name</code>.
     */
    public InPort getInlet(String name) {
        return inlets.computeIfAbsent(name, (key) -> new InPort(this, key, nextInlet++));
    }

    /**
     * Return the {@link Outlet} (an out-going edge) with the specified <code>name</code> for this {@link FlowStage}.
     * If an outlet with that name does not exist, a new one is allocated for the stage.
     *
     * @param name The name of the outlet.
     * @return The {@link OutPort} representing an {@link Outlet} with the specified <code>name</code>.
     */
    public OutPort getOutlet(String name) {
        return outlets.computeIfAbsent(name, (key) -> new OutPort(this, key, nextOutlet++));
    }

    /**
     * Return the current deadline of the {@link FlowStage}'s timer (in milliseconds after epoch).
     */
    public long getDeadline() {
        return deadline;
    }

    /**
     * Set the deadline of the {@link FlowStage}'s timer.
     *
     * @param deadline The new deadline (in milliseconds after epoch) when the stage should be interrupted.
     */
    public void setDeadline(long deadline) {
        this.deadline = deadline;

        if ((flags & STAGE_UPDATE_ACTIVE) == 0) {
            // Update the timer queue with the new deadline
            engine.scheduleDelayed(this);
        }
    }

    /**
     * Invalidate the {@link FlowStage} forcing the stage to update.
     */
    public void invalidate() {
        int flags = this.flags;

        if ((flags & STAGE_UPDATE_ACTIVE) == 0) {
            scheduleImmediate(clock.millis(), flags | STAGE_INVALIDATE);
        }
    }

    /**
     * Synchronously update the {@link FlowStage} at the current timestamp.
     */
    public void sync() {
        this.flags |= STAGE_INVALIDATE;
        onUpdate(clock.millis());
        engine.scheduleDelayed(this);
    }

    /**
     * Close the {@link FlowStage} and disconnect all inlets and outlets.
     */
    public void close() {
        int flags = this.flags;

        if ((flags & STAGE_STATE) == STAGE_CLOSED) {
            return;
        }

        // Toggle the close bit. In case no update is active, schedule a new update.
        if ((flags & STAGE_UPDATE_ACTIVE) != 0) {
            this.flags = flags | STAGE_CLOSE;
        } else {
            scheduleImmediate(clock.millis(), flags | STAGE_CLOSE);
        }
    }

    /**
     * Update the state of the flow stage.
     *
     * @param now The current virtual timestamp.
     */
    void onUpdate(long now) {
        int flags = this.flags;
        int state = flags & STAGE_STATE;

        if (state == STAGE_ACTIVE) {
            doUpdate(now, flags);
        } else if (state == STAGE_PENDING) {
            doStart(now, flags);
        }
    }

    /**
     * Invalidate the {@link FlowStage} forcing the stage to update.
     *
     * <p>
     * This method is similar to {@link #invalidate()}, but allows the user to manually pass the current timestamp to
     * prevent having to re-query the clock. This method should not be called during an update.
     */
    void invalidate(long now) {
        scheduleImmediate(now, flags | STAGE_INVALIDATE);
    }

    /**
     * Schedule an immediate update for this stage.
     */
    private void scheduleImmediate(long now, int flags) {
        // In case an immediate update is already scheduled, no need to do anything
        if ((flags & STAGE_UPDATE_PENDING) != 0) {
            this.flags = flags;
            return;
        }

        // Mark the stage that there is an update pending
        this.flags = flags | STAGE_UPDATE_PENDING;

        engine.scheduleImmediate(now, this);
    }

    /**
     * Start the stage.
     */
    private void doStart(long now, int flags) {
        // Update state before calling into the outside world, so it observes a consistent state
        flags = flags | STAGE_ACTIVE | STAGE_UPDATE_ACTIVE;

        doUpdate(now, flags);
    }

    /**
     * Update the state of the stage.
     */
    private void doUpdate(long now, int flags) {
        long deadline = this.deadline;
        long newDeadline = deadline;

        // Update the stage if:
        //  (1) the timer of the stage has expired.
        //  (2) one of the input ports is pushed,
        //  (3) one of the output ports is pulled,
        if ((flags & STAGE_INVALIDATE) != 0 || deadline == now) {
            // Update state before calling into the outside world, so it observes a consistent state
            this.flags = (flags & ~STAGE_INVALIDATE) | STAGE_UPDATE_ACTIVE;

            try {
                newDeadline = logic.onUpdate(this, now);

                // IMPORTANT: Re-fetch the flags after the callback might have changed those
                flags = this.flags;
            } catch (Exception e) {
                doFail(e);
            }
        }

        // Check whether the stage is marked as closing.
        if ((flags & STAGE_CLOSE) != 0) {
            doClose(flags, null);

            // IMPORTANT: Re-fetch the flags after the callback might have changed those
            flags = this.flags;
        }

        // Indicate that no update is active anymore and flush the flags
        this.flags = flags & ~(STAGE_UPDATE_ACTIVE | STAGE_UPDATE_PENDING);
        this.deadline = newDeadline;

        // Update the timer queue with the new deadline
        engine.scheduleDelayedInContext(this);
    }

    /**
     * This method is invoked when an uncaught exception is caught by the engine. When this happens, the
     * {@link FlowStageLogic} "fails" and disconnects all its inputs and outputs.
     */
    void doFail(Throwable cause) {
        LOGGER.warn("Uncaught exception (closing stage)", cause);

        doClose(flags, cause);
    }

    /**
     * This method is invoked when the {@link FlowStageLogic} exits successfully or due to failure.
     */
    private void doClose(int flags, Throwable cause) {
        // Mark the stage as closed
        this.flags = flags & ~(STAGE_STATE | STAGE_INVALIDATE | STAGE_CLOSE) | STAGE_CLOSED;

        // Remove stage from parent graph
        parentGraph.detach(this);

        // Remove stage from the timer queue
        setDeadline(Long.MAX_VALUE);

        // Cancel all input ports
        for (InPort port : inlets.values()) {
            if (port != null) {
                port.cancel(cause);
            }
        }

        // Cancel all output ports
        for (OutPort port : outlets.values()) {
            if (port != null) {
                port.fail(cause);
            }
        }
    }
}
