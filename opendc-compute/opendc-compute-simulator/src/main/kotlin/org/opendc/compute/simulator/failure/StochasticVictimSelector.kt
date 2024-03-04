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

package org.opendc.compute.simulator.failure

import org.apache.commons.math3.distribution.RealDistribution
import org.opendc.compute.simulator.SimHost
import java.util.ArrayList
import java.util.SplittableRandom
import java.util.random.RandomGenerator
import kotlin.math.roundToInt

/**
 * A [VictimSelector] that stochastically selects a set of hosts to be failed.
 */
public class StochasticVictimSelector(
    private val size: RealDistribution,
    private val random: RandomGenerator = SplittableRandom(0),
) : VictimSelector {
    override fun select(hosts: Set<SimHost>): List<SimHost> {
        val n = size.sample().roundToInt()
        val result = ArrayList<SimHost>(n)

        val random = random
        var samplesNeeded = n
        var remainingHosts = hosts.size
        val iterator = hosts.iterator()

        while (iterator.hasNext() && samplesNeeded > 0) {
            val host = iterator.next()

            if (random.nextInt(remainingHosts) < samplesNeeded) {
                result.add(host)
                samplesNeeded--
            }

            remainingHosts--
        }

        return result
    }

    override fun toString(): String = "StochasticVictimSelector[$size]"
}
