/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.compute.api

/**
 * Flavors define the compute and memory capacity of [ServiceTask] instance. To put it simply, a flavor is an available
 * hardware configuration for a task. It defines the size of a virtual task that can be launched.
 */
public interface Flavor : Resource {
    /**
     * The number of (virtual) processing cores to use.
     */
    public val cpuCoreCount: Int

    /**
     * The amount of RAM available to the task (in MB).
     */
    public val memorySize: Long

    /**
     * The amount of gpu cores available to the task.
     */
    public val gpuCoreCount: Int

    /**
     * Set of Tasks that need to be finished before this can startAdd commentMore actions
     */
    public val dependencies: Set<Int>

    /**
     * Set of Tasks that need to be finished before this can startAdd commentMore actions
     */
    public val parents: Set<Int>

    /**
     * Set of Tasks that need to be finished before this can startAdd commentMore actions
     */
    public val children: Set<Int>
}
