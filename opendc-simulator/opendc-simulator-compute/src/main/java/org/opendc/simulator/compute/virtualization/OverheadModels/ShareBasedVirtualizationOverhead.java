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
 * A VirtualizationOverheadModel that divides the current GPU demand by the number of consumers.
 * This model assumes that the supply is shared among all consumers, effectively reducing the
 * supply available to each consumer based on the number of consumers.
 */
public class ShareBasedVirtualizationOverhead implements VirtualizationOverheadModel {
    @Override
    public double getSupply(double currentGpuDemand, int consumerCount) {
        // Supply is divided by the number of consumers to account for sharing
        return currentGpuDemand / (consumerCount == 0 ? 1 : consumerCount);
    }
}
