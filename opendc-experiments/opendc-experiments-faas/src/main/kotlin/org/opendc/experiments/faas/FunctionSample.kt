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

package org.opendc.experiments.faas

/**
 * A sample of a single function.
 *
 * @param timestamp The timestamp of the function.
 * @param duration The average execution time of the function.
 * @param invocations The number of invocations.
 * @param provisionedCpu The provisioned CPU for this function in MHz.
 * @param provisionedMem The amount of memory provisioned for this function in MB.
 * @param cpuUsage The actual CPU usage in MHz.
 * @param memUsage The actual memory usage in MB.
 */
public data class FunctionSample(
    val timestamp: Long,
    val duration: Long,
    val invocations: Int,
    val provisionedCpu: Int,
    val provisionedMem: Int,
    val cpuUsage: Double,
    val memUsage: Double
)
