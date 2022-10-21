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
 * Test suite for the [PowerSaveScalingGovernor]
 */
internal class PowerSaveScalingGovernorTest {
    @Test
    fun testSetStartLimitWithoutPStates() {
        val cpuCapacity = 4100.0
        val minSpeed = cpuCapacity / 2
        val policy = mockk<ScalingPolicy>(relaxUnitFun = true)
        val logic = ScalingGovernors.powerSave().newGovernor(policy)

        every { policy.max } returns cpuCapacity
        every { policy.min } returns minSpeed

        logic.onStart()

        logic.onLimit(0.0)
        verify(exactly = 1) { policy.target = minSpeed }

        logic.onLimit(1.0)
        verify(exactly = 1) { policy.target = minSpeed }
    }

    @Test
    fun testSetStartLimitWithPStates() {
        val cpuCapacity = 4100.0
        val firstPState = 1000.0
        val policy = mockk<ScalingPolicy>(relaxUnitFun = true)
        val logic = ScalingGovernors.powerSave().newGovernor(policy)

        every { policy.max } returns cpuCapacity
        every { policy.min } returns firstPState

        logic.onStart()
        verify(exactly = 1) { policy.target = firstPState }

        logic.onLimit(0.0)
        verify(exactly = 1) { policy.target = firstPState }

        logic.onLimit(1.0)
        verify(exactly = 1) { policy.target = firstPState }
    }
}
