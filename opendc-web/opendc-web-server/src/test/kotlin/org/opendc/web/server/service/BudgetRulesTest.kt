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

package org.opendc.web.server.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.server.model.BudgetPeriod
import org.opendc.web.server.model.PlanTier
import org.opendc.web.server.model.SimulationCap
import java.time.Instant

/** What a run costs, and whom: the rules every charge and every refusal follow. */
class BudgetRulesTest {
    private val start = Instant.parse("2026-10-01T10:00:00Z")
    private val tenSeconds = PlatformSpan.Ran(start, start.plusSeconds(10))

    @Test
    fun `charges the cores held for as long as the platform ran the work`() {
        for (reason in listOf(ExitReason.OK, ExitReason.SIMULATION_ERROR, ExitReason.INVALID_SPEC, ExitReason.CANCELLED)) {
            assertEquals(40.0, chargedSeconds(reason, tenSeconds, parallelism = 4), 1e-9, "$reason")
        }
    }

    // Running out of memory or time, or being lost by the platform, is how the work was sized or where
    // it ran, which the user did not choose.
    @Test
    fun `charges nothing for failures that are the platform's or the sizing's`() {
        for (reason in listOf(ExitReason.OOM, ExitReason.TIMEOUT, ExitReason.WALLTIME, ExitReason.UNKNOWN, ExitReason.REJECTED)) {
            assertEquals(0.0, chargedSeconds(reason, tenSeconds, parallelism = 4), "$reason")
        }
    }

    @Test
    fun `charges nothing for work that never started`() {
        assertEquals(0.0, chargedSeconds(ExitReason.OK, PlatformSpan.NotStarted, parallelism = 8))
    }

    @Test
    fun `gives each plan its caps, and enterprise none`() {
        assertEquals(SimulationCap.Limited(3600.0), planCap(PlanTier.FREE, BudgetPeriod.SESSION))
        assertEquals(SimulationCap.Limited(144_000.0), planCap(PlanTier.EDUCATION, BudgetPeriod.WEEK))
        assertEquals(SimulationCap.Unlimited, planCap(PlanTier.ENTERPRISE, BudgetPeriod.WEEK))
    }
}
