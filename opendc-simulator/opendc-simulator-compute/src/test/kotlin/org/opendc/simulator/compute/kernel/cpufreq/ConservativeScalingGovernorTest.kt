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

package org.opendc.simulator.compute.kernel.cpufreq

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * Test suite for the conservative [ScalingGovernor].
 */
internal class ConservativeScalingGovernorTest {
    @Test
    fun testSetStartLimitWithoutPStates() {
        val cpuCapacity = 4100.0
        val minSpeed = cpuCapacity / 2
        val defaultThreshold = 0.8
        val defaultStepSize = 0.05 * cpuCapacity
        val governor = ScalingGovernors.conservative(defaultThreshold)

        val policy = mockk<ScalingPolicy>(relaxUnitFun = true)
        every { policy.max } returns cpuCapacity
        every { policy.min } returns minSpeed

        var target = 0.0
        every { policy.target } answers { target }
        every { policy.target = any() } propertyType Double::class answers { target = value }

        val logic = governor.newGovernor(policy)
        logic.onStart()
        logic.onLimit(0.5)

        /* Upwards scaling */
        logic.onLimit(defaultThreshold + 0.2)

        /* Downwards scaling */
        logic.onLimit(defaultThreshold + 0.1)

        verify(exactly = 2) { policy.target = minSpeed }
        verify(exactly = 1) { policy.target = minSpeed + defaultStepSize }
    }

    @Test
    fun testSetStartLimitWithPStatesAndParams() {
        val firstPState = 1000.0
        val cpuCapacity = 4100.0
        val minSpeed = firstPState
        val threshold = 0.5
        val stepSize = 0.02 * cpuCapacity
        val governor = ScalingGovernors.conservative(threshold, stepSize)

        val policy = mockk<ScalingPolicy>(relaxUnitFun = true)
        every { policy.max } returns cpuCapacity
        every { policy.min } returns firstPState

        var target = 0.0
        every { policy.target } answers { target }
        every { policy.target = any() } propertyType Double::class answers { target = value }

        val logic = governor.newGovernor(policy)
        logic.onStart()
        logic.onLimit(0.5)

        /* Upwards scaling */
        logic.onLimit(threshold + 0.2)

        /* Downwards scaling */
        logic.onLimit(threshold + 0.1)

        verify(exactly = 2) { policy.target = minSpeed }
        verify(exactly = 1) { policy.target = minSpeed + stepSize }
    }
}
