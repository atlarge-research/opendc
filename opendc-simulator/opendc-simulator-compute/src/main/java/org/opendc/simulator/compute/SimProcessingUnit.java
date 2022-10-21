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

package org.opendc.simulator.compute;

import org.opendc.simulator.compute.model.ProcessingUnit;
import org.opendc.simulator.flow2.sink.FlowSink;

/**
 * A simulated processing unit.
 */
public interface SimProcessingUnit extends FlowSink {
    /**
     * Return the base clock frequency of the processing unit (in MHz).
     */
    double getFrequency();

    /**
     * Adjust the base clock frequency of the processing unit.
     *
     * <p>
     * The CPU may or may not round the new frequency to one of its pre-defined frequency steps.
     *
     * @param frequency The new frequency to set the clock of the processing unit to.
     * @throws UnsupportedOperationException if the base clock cannot be adjusted.
     */
    void setFrequency(double frequency);

    /**
     * The demand on the processing unit.
     */
    double getDemand();

    /**
     * The speed of the processing unit.
     */
    double getSpeed();

    /**
     *  The model representing the static properties of the processing unit.
     */
    ProcessingUnit getModel();
}
