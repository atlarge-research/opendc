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
 * A port that outputs a flow.
 * <p>
 * Output ports are represented as out-going edges in the flow graph.
 */
public final class OutPort implements Outlet {
    private final int id;

    private float capacity;
    private float demand;

    private boolean mask;

    InPort input;
    private OutHandler handler = OutHandlers.noop();
    private final String name;
    private final FlowStage stage;
    private final InstantSource clock;

    OutPort(FlowStage stage, String name, int id) {
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
     * Return the identifier of the {@link OutPort} with respect to its stage.
     */
    public int getId() {
        return id;
    }

    /**
     * Return the capacity of the output port.
     */
    public float getCapacity() {
        return capacity;
    }

    /**
     * Return the current demand of flow of the output port.
     */
    public float getDemand() {
        return demand;
    }

    /**
     * Return the current rate of flow of the input port.
     */
    public float getRate() {
        InPort input = this.input;
        if (input != null) {
            return input.getRate();
        }

        return 0.f;
    }

    /**
     * Return the current {@link OutHandler} of the output port.
     */
    public OutHandler getHandler() {
        return handler;
    }

    /**
     * Set the {@link OutHandler} of the output port.
     */
    public void setHandler(OutHandler handler) {
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
     * Push the given flow rate over output port.
     *
     * @param rate The rate of the flow to push.
     */
    public void push(float rate) {
        demand = rate;
        InPort input = this.input;

        if (input != null) {
            input.push(rate);
        }
    }

    /**
     * Signal to the downstream port that the output has completed successfully and disconnect the port from its input.
     * <p>
     * The output port can still be used and re-connected to another input.
     */
    public void complete() {
        fail(null);
    }

    /**
     * Signal a failure to the downstream port and disconnect the port from its input.
     * <p>
     * The output can still be used and re-connected to another input.
     */
    public void fail(Throwable cause) {
        capacity = 0.f;

        InPort input = this.input;
        if (input != null) {
            this.input = null;
            input.output = null;
            input.finish(cause);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutPort port = (OutPort) o;
        return stage.equals(port.stage) && name.equals(port.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stage.parentGraph, name);
    }

    /**
     * This method is invoked when the outlet is connected to an inlet.
     */
    void connect() {
        input.push(demand);
    }

    /**
     * Pull from this outlet with a specified capacity.
     *
     * @param capacity The capacity of the inlet.
     */
    void pull(float capacity) {
        // No-op when outlet is not active or the rate is unchanged
        if (this.capacity == capacity) {
            return;
        }

        try {
            handler.onPull(this, capacity);
            this.capacity = capacity;

            if (!mask) {
                stage.invalidate(clock.millis());
            }
        } catch (Exception e) {
            stage.doFail(e);
        }
    }

    /**
     * This method is invoked by the connected {@link InPort} when downstream cancels the connection.
     */
    void cancel(Throwable cause) {
        try {
            handler.onDownstreamFinish(this, cause);
            this.capacity = 0.f;

            if (!mask) {
                stage.invalidate(clock.millis());
            }
        } catch (Exception e) {
            stage.doFail(e);
        }
    }
}
