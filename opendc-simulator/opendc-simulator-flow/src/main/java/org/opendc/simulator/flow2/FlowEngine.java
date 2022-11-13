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

import java.time.Clock;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import kotlin.coroutines.CoroutineContext;
import org.opendc.common.Dispatcher;

/**
 * A {@link FlowEngine} simulates a generic flow network.
 * <p>
 * The engine centralizes the scheduling logic of state updates of flow connections, allowing update propagation
 * to happen more efficiently. and overall, reducing the work necessary to transition into a steady state.
 */
public final class FlowEngine implements Runnable {
    /**
     * The queue of {@link FlowStage} updates that are scheduled for immediate execution.
     */
    private final FlowStageQueue queue = new FlowStageQueue(256);

    /**
     * A priority queue containing the {@link FlowStage} updates to be scheduled in the future.
     */
    private final FlowTimerQueue timerQueue = new FlowTimerQueue(256);

    /**
     * The stack of engine invocations to occur in the future.
     */
    private final InvocationStack futureInvocations = new InvocationStack(256);

    /**
     * A flag to indicate that the engine is active.
     */
    private boolean active;

    private final Dispatcher dispatcher;
    private final InstantSource clock;

    /**
     * Create a new {@link FlowEngine} instance using the specified {@link CoroutineContext} and {@link InstantSource}.
     */
    public static FlowEngine create(Dispatcher dispatcher) {
        return new FlowEngine(dispatcher);
    }

    FlowEngine(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.clock = dispatcher.getTimeSource();
    }

    /**
     * Obtain the (virtual) {@link Clock} driving the simulation.
     */
    public InstantSource getClock() {
        return clock;
    }

    /**
     * Return a new {@link FlowGraph} that can be used to build a flow network.
     */
    public FlowGraph newGraph() {
        return new RootGraph(this);
    }

    /**
     * Enqueue the specified {@link FlowStage} to be updated immediately during the active engine cycle.
     * <p>
     * This method should be used when the state of a flow context is invalidated/interrupted and needs to be
     * re-computed.
     */
    void scheduleImmediate(long now, FlowStage ctx) {
        scheduleImmediateInContext(ctx);

        // In-case the engine is already running in the call-stack, return immediately. The changes will be picked
        // up by the active engine.
        if (active) {
            return;
        }

        trySchedule(futureInvocations, now, now);
    }

    /**
     * Enqueue the specified {@link FlowStage} to be updated immediately during the active engine cycle.
     * <p>
     * This method should be used when the state of a flow context is invalidated/interrupted and needs to be
     * re-computed.
     * <p>
     * This method should only be invoked while inside an engine cycle.
     */
    void scheduleImmediateInContext(FlowStage ctx) {
        queue.add(ctx);
    }

    /**
     * Enqueue the specified {@link FlowStage} to be updated at its updated deadline.
     */
    void scheduleDelayed(FlowStage ctx) {
        scheduleDelayedInContext(ctx);

        // In-case the engine is already running in the call-stack, return immediately. The changes will be picked
        // up by the active engine.
        if (active) {
            return;
        }

        long deadline = timerQueue.peekDeadline();
        if (deadline != Long.MAX_VALUE) {
            trySchedule(futureInvocations, clock.millis(), deadline);
        }
    }

    /**
     * Enqueue the specified {@link FlowStage} to be updated at its updated deadline.
     * <p>
     * This method should only be invoked while inside an engine cycle.
     */
    void scheduleDelayedInContext(FlowStage ctx) {
        FlowTimerQueue timerQueue = this.timerQueue;
        timerQueue.enqueue(ctx);
    }

    /**
     * Run all the enqueued actions for the specified timestamp (<code>now</code>).
     */
    private void doRunEngine(long now) {
        final FlowStageQueue queue = this.queue;
        final FlowTimerQueue timerQueue = this.timerQueue;

        try {
            // Mark the engine as active to prevent concurrent calls to this method
            active = true;

            // Execute all scheduled updates at current timestamp
            while (true) {
                final FlowStage ctx = timerQueue.poll(now);
                if (ctx == null) {
                    break;
                }

                ctx.onUpdate(now);
            }

            // Execute all immediate updates
            while (true) {
                final FlowStage ctx = queue.poll();
                if (ctx == null) {
                    break;
                }

                ctx.onUpdate(now);
            }
        } finally {
            active = false;
        }

        // Schedule an engine invocation for the next update to occur.
        long headDeadline = timerQueue.peekDeadline();
        if (headDeadline != Long.MAX_VALUE && headDeadline >= now) {
            trySchedule(futureInvocations, now, headDeadline);
        }
    }

    @Override
    public void run() {
        doRunEngine(futureInvocations.poll());
    }

    /**
     * Try to schedule an engine invocation at the specified [target].
     *
     * @param scheduled The queue of scheduled invocations.
     * @param now The current virtual timestamp.
     * @param target The virtual timestamp at which the engine invocation should happen.
     */
    private void trySchedule(InvocationStack scheduled, long now, long target) {
        // Only schedule a new scheduler invocation in case the target is earlier than all other pending
        // scheduler invocations
        if (scheduled.tryAdd(target)) {
            dispatcher.schedule(target - now, this);
        }
    }

    /**
     * Internal implementation of a root {@link FlowGraph}.
     */
    private static final class RootGraph implements FlowGraphInternal {
        private final FlowEngine engine;
        private final List<FlowStage> stages = new ArrayList<>();

        public RootGraph(FlowEngine engine) {
            this.engine = engine;
        }

        @Override
        public FlowEngine getEngine() {
            return engine;
        }

        @Override
        public FlowStage newStage(FlowStageLogic logic) {
            final FlowEngine engine = this.engine;
            final FlowStage stage = new FlowStage(this, logic);
            stages.add(stage);
            long now = engine.getClock().millis();
            stage.invalidate(now);
            return stage;
        }

        @Override
        public void connect(Outlet outlet, Inlet inlet) {
            FlowGraphInternal.connect(this, outlet, inlet);
        }

        @Override
        public void disconnect(Outlet outlet) {
            FlowGraphInternal.disconnect(this, outlet);
        }

        @Override
        public void disconnect(Inlet inlet) {
            FlowGraphInternal.disconnect(this, inlet);
        }

        /**
         * Internal method to remove the specified {@link FlowStage} from the graph.
         */
        @Override
        public void detach(FlowStage stage) {
            stages.remove(stage);
        }
    }
}
