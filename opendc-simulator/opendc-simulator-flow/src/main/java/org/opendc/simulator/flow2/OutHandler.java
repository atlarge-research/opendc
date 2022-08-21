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

/**
 * Collection of callbacks for the output port (a {@link OutPort}) of a {@link FlowStageLogic}.
 */
public interface OutHandler {
    /**
     * This method is invoked when another {@link FlowStageLogic} changes the capacity of the outlet.
     *
     * @param port     The output port of which the capacity was changed.
     * @param capacity The new capacity of the outlet.
     */
    void onPull(OutPort port, float capacity);

    /**
     * This method is invoked when the output port no longer accepts any flow.
     * <p>
     * After this callback no other callbacks will be called for this port.
     *
     * @param port  The outlet that no longer accepts any flow.
     * @param cause The cause of the output port no longer accepting any flow or <code>null</code> if the port closed
     *              successfully.
     */
    void onDownstreamFinish(OutPort port, Throwable cause);
}
