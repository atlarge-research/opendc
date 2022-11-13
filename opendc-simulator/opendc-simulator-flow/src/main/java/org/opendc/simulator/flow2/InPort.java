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
import java.util.Objects;

/**
 * A port that consumes a flow.
 * <p>
 * Input ports are represented as in-going edges in the flow graph.
 */
public final class InPort implements Inlet {
    private final int id;

    private float capacity;
    private float demand;

    private boolean mask;

    OutPort output;
    private InHandler handler = InHandlers.noop();
    private final InstantSource clock;
    private final String name;
    private final FlowStage stage;

    InPort(FlowStage stage, String name, int id) {
        this.name = name;
        this.id = id;
        this.stage = stage;
        this.clock = stage.clock;
    }

    @Override
    public FlowGraph getGraph() {
        return stage.parentGraph;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Return the identifier of the {@link InPort} with respect to its stage.
     */
    public int getId() {
        return id;
    }

    /**
     * Return the current capacity of the input port.
     */
    public float getCapacity() {
        return capacity;
    }

    /**
     * Return the current demand of flow of the input port.
     */
    public float getDemand() {
        return demand;
    }

    /**
     * Return the current rate of flow of the input port.
     */
    public float getRate() {
        return handler.getRate(this);
    }

    /**
     * Pull the flow with the specified <code>capacity</code> from the input port.
     *
     * @param capacity The maximum throughput that the stage can receive from the input port.
     */
    public void pull(float capacity) {
        this.capacity = capacity;

        OutPort output = this.output;
        if (output != null) {
            output.pull(capacity);
        }
    }

    /**
     * Return the current {@link InHandler} of the input port.
     */
    public InHandler getHandler() {
        return handler;
    }

    /**
     * Set the {@link InHandler} of the input port.
     */
    public void setHandler(InHandler handler) {
        this.handler = handler;
    }

    /**
     * Return the mask of this port.
     * <p>
     * Stages ignore events originating from masked ports.
     */
    public boolean getMask() {
        return mask;
    }

    /**
     * (Un)mask the port.
     */
    public void setMask(boolean mask) {
        this.mask = mask;
    }

    /**
     * Disconnect the input port from its (potentially) connected outlet.
     * <p>
     * The inlet can still be used and re-connected to another outlet.
     *
     * @param cause The cause for disconnecting the port or <code>null</code> when no more flow is needed.
     */
    public void cancel(Throwable cause) {
        demand = 0.f;

        OutPort output = this.output;
        if (output != null) {
            this.output = null;
            output.input = null;
            output.cancel(cause);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InPort port = (InPort) o;
        return stage.equals(port.stage) && name.equals(port.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stage.parentGraph, name);
    }

    /**
     * This method is invoked when the inlet is connected to an outlet.
     */
    void connect() {
        OutPort output = this.output;
        output.pull(capacity);
    }

    /**
     * Push a flow from an outlet to this inlet.
     *
     * @param demand The rate of flow to push.
     */
    void push(float demand) {
        // No-op when the rate is unchanged
        if (this.demand == demand) {
            return;
        }

        try {
            handler.onPush(this, demand);
            this.demand = demand;

            if (!mask) {
                stage.invalidate(clock.millis());
            }
        } catch (Exception e) {
            stage.doFail(e);
        }
    }

    /**
     * This method is invoked by the connected {@link OutPort} when it finishes.
     */
    void finish(Throwable cause) {
        try {
            long now = clock.millis();
            handler.onUpstreamFinish(this, cause);
            this.demand = 0.f;

            if (!mask) {
                stage.invalidate(now);
            }
        } catch (Exception e) {
            stage.doFail(e);
        }
    }
}
