/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.simulator.compute.virtualization.OverheadModels;

import org.opendc.simulator.compute.virtualization.VirtualizationOverheadModel;

/**
 * A VirtualizationOverheadModel that applies a constant percentage overhead to the current GPU demand.
 * This model is useful for scenarios where a fixed overhead is expected regardless of the number of consumers.
 */
public class ConstantVirtualizationOverhead implements VirtualizationOverheadModel {

    private double percentageOverhead = 0.0;

    public double getPercentageOverhead() {
        return percentageOverhead;
    }

    /**
     * Creates a new instance of ConstantVirtualizationOverhead with the specified percentage overhead.
     *
     * @param percentageOverhead The percentage overhead to apply to the current GPU demand.
     *                           If set to -1.0, a default value of 0.05 (5%) is used.
     */
    public ConstantVirtualizationOverhead(double percentageOverhead) {

        this.percentageOverhead = (percentageOverhead == -1.0) ? 0.05 : percentageOverhead;
    }

    @Override
    public double getSupply(double currentGpuDemand, int consumerCount) {
        return currentGpuDemand * (1 - percentageOverhead);
    }
}
