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
 * Collection of callbacks for the input port (a {@link InPort}) of a {@link FlowStageLogic}.
 */
public interface InHandler {
    /**
     * Return the actual flow rate over the input port.
     *
     * @param port The input port to which the flow was pushed.
     * @return The actual flow rate over the port.
     */
    default float getRate(InPort port) {
        return Math.min(port.getDemand(), port.getCapacity());
    }

    /**
     * This method is invoked when another {@link FlowStageLogic} changes the rate of flow to the specified inlet.
     *
     * @param port   The input port to which the flow was pushed.
     * @param demand The rate of flow the output attempted to push to the port.
     */
    void onPush(InPort port, float demand);

    /**
     * This method is invoked when the input port is finished.
     *
     * @param port  The input port that has finished.
     * @param cause The cause of the input port being finished or <code>null</code> if the port completed successfully.
     */
    void onUpstreamFinish(InPort port, Throwable cause);
}
