/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.simulator.compute.workload.trace;

import org.opendc.simulator.engine.graph.FlowEdge;

public record TraceFragment(long duration, double cpuUsage, int cpuCoreCount, double gpuUsage, int gpuCoreCount, double gpuMemoryUsage ) {

    public TraceFragment(long start, long duration, double cpuUsage, int cpuCoreCount) {
        this(duration, cpuUsage, cpuCoreCount, 0.0, 0, 0.0);
    }
    public TraceFragment(long duration, double cpuUsage, int cpuCoreCount) {
        this(duration, cpuUsage, cpuCoreCount, 0.0, 0, 0.0);
    }
    public TraceFragment(long start, long duration, double cpuUsage, int cpuCoreCount, double gpuUsage, int gpuCoreCount) {
        this(duration, cpuUsage, cpuCoreCount, gpuUsage, gpuCoreCount, 0.0);
    }

    /**
     * Returns the resource usage for the specified resource type.
     *
     * @param resourceType the type of resource
     * @return the usage value for the specified resource type
     */
    public double getResourceUsage(FlowEdge.ResourceType resourceType) throws IllegalArgumentException {
        return switch (resourceType) {
            case CPU -> cpuUsage;
            case GPU -> gpuUsage;
//            case GPU_MEMORY -> gpuMemoryUsage;
            default -> throw new IllegalArgumentException("Invalid resource type: " + resourceType);
        };
    }

    /**
     * Returns the core count for the specified resource type.
     *
     * @param resourceType the type of resource
     * @return the core count for the specified resource type
     */
    public int getCoreCount(FlowEdge.ResourceType resourceType) throws IllegalArgumentException {
        return switch (resourceType) {
            case CPU -> cpuCoreCount;
            case GPU -> gpuCoreCount;
            default -> throw new IllegalArgumentException("Invalid resource type: " + resourceType);
        };
    }
}
