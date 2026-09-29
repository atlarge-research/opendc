/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.simulator.compute.scheduler

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.simulator.compute.carbon.CarbonNode
import org.opendc.simulator.compute.scheduler.timeshift.CarbonTimeshifter
import org.opendc.simulator.compute.task.SimTask
import java.time.InstantSource

/**
 * Test suite for the [CarbonTimeshifter].
 */
internal class CarbonTimeshifterTest {
    private val clock = mockk<InstantSource>().also { every { it.millis() } returns 10 }

    private fun task(
        deferrable: Boolean = true,
        duration: Long = 10,
        deadline: Long = 50,
    ): SimTask {
        val task = mockk<SimTask>()
        every { task.deferrable } returns deferrable
        every { task.duration } returns duration
        every { task.deadline } returns deadline
        return task
    }

    /** A timeshifter comparing against the moving average, which saw the intensity rise from 100 to 200. */
    private fun highCarbon(): CarbonTimeshifter {
        val timeshifter = CarbonTimeshifter(clock, windowSize = 2, forecast = false)
        timeshifter.updateCarbonIntensity(100.0)
        timeshifter.updateCarbonIntensity(200.0)
        return timeshifter
    }

    @Test
    fun testDelaysDeferrableTaskWhileCarbonIsHigh() {
        assertTrue(highCarbon().shouldDelay(task()))
    }

    @Test
    fun testDoesNotDelayPastTheDeadline() {
        // Starting at 10 with a duration of 10, the task only just meets a deadline of 20
        assertFalse(highCarbon().shouldDelay(task(deadline = 20)))
    }

    @Test
    fun testDoesNotDelayTaskThatIsNotDeferrable() {
        assertFalse(highCarbon().shouldDelay(task(deferrable = false)))
    }

    @Test
    fun testShortAndLongTasksFollowTheirOwnRegime() {
        val timeshifter = CarbonTimeshifter(clock, windowSize = 2, forecast = false)
        timeshifter.updateCarbonIntensity(200.0)
        timeshifter.updateCarbonIntensity(100.0)

        // Below the average of 150 is low for long tasks; short tasks also need the intensity to be rising
        val hours = 60 * 60 * 1000L
        assertTrue(timeshifter.shouldDelay(task(duration = 1 * hours, deadline = 10 * hours)))
        assertFalse(timeshifter.shouldDelay(task(duration = 3 * hours, deadline = 10 * hours)))
    }

    @Test
    fun testForecastQuantiles() {
        val carbonNode = mockk<CarbonNode>()
        every { carbonNode.getForecast(10) } returns doubleArrayOf(100.0, 90.0, 80.0, 70.0, 60.0, 50.0, 40.0, 30.0, 20.0, 10.0)
        val timeshifter = CarbonTimeshifter(clock, forecastSize = 10, shortForecastThreshold = 0.2, longForecastThreshold = 0.35)
        timeshifter.setCarbonNode(carbonNode)

        // 40 is above the short quantile (30) but below the long quantile (50)
        timeshifter.updateCarbonIntensity(40.0)

        val hours = 60 * 60 * 1000L
        assertTrue(timeshifter.shouldDelay(task(duration = 1 * hours, deadline = 10 * hours)))
        assertFalse(timeshifter.shouldDelay(task(duration = 3 * hours, deadline = 10 * hours)))
    }
}
