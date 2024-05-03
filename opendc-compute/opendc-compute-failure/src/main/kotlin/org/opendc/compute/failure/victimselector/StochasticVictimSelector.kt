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

package org.opendc.compute.failure.victimselector

import org.opendc.compute.simulator.SimHost
import java.util.SplittableRandom
import java.util.random.RandomGenerator
import kotlin.math.max
import kotlin.math.min

/**
 * A [VictimSelector] that stochastically selects a set of hosts to be failed.
 */
public class StochasticVictimSelector(
    private val random: RandomGenerator = SplittableRandom(0),
) : VictimSelector {
    override fun select(numberOfHosts: Int): List<SimHost> {
        error("select with only int cannot be used in this type of VictimSelector")
    }

    override fun select(
        hosts: Set<SimHost>,
        numberOfHosts: Int,
    ): List<SimHost> {
        val result = ArrayList<SimHost>(numberOfHosts)

        val random = random
        var samplesNeeded = numberOfHosts
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

    override fun select(failureIntensity: Double): List<SimHost> {
        error("select with only int cannot be used in this type of VictimSelector")
    }

    override fun select(
        hosts: Set<SimHost>,
        failureIntensity: Double,
    ): List<SimHost> {
        // clamp value between 0.0 and 1.0
        val intensity = min(1.0, max(0.0, failureIntensity))
        val numberOfHosts = (hosts.size * intensity).toInt()

        return hosts.asSequence().shuffled().take(numberOfHosts).toList()
    }

    override fun toString(): String = "StochasticVictimSelector"
}
