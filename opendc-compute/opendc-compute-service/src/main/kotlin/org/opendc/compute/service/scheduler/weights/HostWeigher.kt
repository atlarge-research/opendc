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

package org.opendc.compute.service.scheduler.weights

import org.opendc.compute.api.Server
import org.opendc.compute.service.HostView
import org.opendc.compute.service.scheduler.FilterScheduler

/**
 * An interface used by the [FilterScheduler] to weigh the pool of host for a scheduling request.
 */
public interface HostWeigher {
    /**
     * The multiplier for the weigher.
     */
    public val multiplier: Double

    /**
     * Obtain the weight of the specified [host] when scheduling the specified [server].
     */
    public fun getWeight(host: HostView, server: Server): Double

    /**
     * Obtain the weights for [hosts] when scheduling the specified [server].
     */
    public fun getWeights(hosts: List<HostView>, server: Server): Result {
        val weights = DoubleArray(hosts.size)
        var min = Double.MAX_VALUE
        var max = Double.MIN_VALUE

        for ((i, host) in hosts.withIndex()) {
            val weight = getWeight(host, server)
            weights[i] = weight
            min = kotlin.math.min(min, weight)
            max = kotlin.math.max(max, weight)
        }

        return Result(weights, min, max, multiplier)
    }

    /**
     * A result returned by the weigher.
     *
     * @param weights The weights returned by the weigher.
     * @param min The minimum weight returned.
     * @param max The maximum weight returned.
     * @param multiplier The weight multiplier to use.
     */
    public class Result(
        public val weights: DoubleArray,
        public val min: Double,
        public val max: Double,
        public val multiplier: Double
    )
}
