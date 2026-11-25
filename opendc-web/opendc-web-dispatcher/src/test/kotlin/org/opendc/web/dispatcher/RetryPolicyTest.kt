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

package org.opendc.web.dispatcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Retrying costs a slot, so what matters is that a failure which cannot improve is never retried,
 * that escalation addresses the resource that actually ran out, and that it terminates.
 */
class RetryPolicyTest {
    private val slot = ExecutionSlot(cores = 4, memoryMb = 4096.0, timeCap = TimeCap.Unlimited)
    private val policy =
        DispatchPolicy(
            jvmBaselineMb = 512.0,
            offHeapPerUnitMb = 0.0,
            heapHeadroom = 1.0,
            startupSeconds = 30.0,
            timeSafetyFactor = 3.0,
            maxAttempts = 3,
            growthFactor = 2.0,
            maxMemoryRequestMb = 32_768.0,
        )

    private fun units(
        count: Int = 4,
        peakMemoryMb: Double = 512.0,
    ) = (0 until count).map { PlannedUnit(it, 0, 100.0, peakMemoryMb) }

    private fun grant(
        parallelism: Int = 4,
        heapMb: Int = 2048,
        timeLimitSeconds: Int = 330,
    ) = Grant(parallelism, heapMb, heapMb + 512, timeLimitSeconds)

    @ParameterizedTest
    @EnumSource(value = ExitReason::class, names = ["SIMULATION_ERROR", "INVALID_SPEC", "CANCELLED", "OK", "REJECTED"])
    fun `never retries a failure that a different grant cannot change`(reason: ExitReason) {
        assertEquals(emptyList<PlannedBag>(), retryBags(units(), grant(), attempt = 1, reason, slot, policy))
    }

    @ParameterizedTest
    @EnumSource(value = ExitReason::class, names = ["OOM", "TIMEOUT", "WALLTIME", "UNKNOWN"])
    fun `stops retrying once the attempt cap is reached, whatever the reason`(reason: ExitReason) {
        assertEquals(emptyList<PlannedBag>(), retryBags(units(), grant(), policy.maxAttempts, reason, slot, policy))
    }

    // Re-packing units by the estimate that packed them the first time would hand the platform the
    // same bag again. Units that ran out of memory have to come back bigger, not only fewer at once.
    @Test
    fun `answers an out-of-memory kill by giving every unit more memory than it had`() {
        val estimated = units(count = 3, peakMemoryMb = 100.0)
        val failed = grant(parallelism = 3, heapMb = 900)

        val retries = retryBags(estimated, failed, attempt = 1, ExitReason.OOM, slot, policy)

        assertEquals(3, retries.sumOf { it.units.size }, "growing must not drop work")
        for (unit in retries.flatMap { it.units }) {
            assertTrue(unit.peakMemoryMb > 300.0, "each unit had a 300 MB share of the heap that ran out, and got ${unit.peakMemoryMb}")
        }
        for (bag in retries) {
            assertTrue(
                bag.grant.heapMb / bag.grant.parallelism > failed.heapMb / failed.parallelism,
                "each unit's share of the heap must grow",
            )
        }
    }

    @Test
    fun `gives up rather than asking for a grant nothing will place`() {
        val huge = units(count = 1, peakMemoryMb = 20_000.0)

        assertEquals(
            emptyList<PlannedBag>(),
            retryBags(huge, grant(parallelism = 1, heapMb = 20_000), attempt = 1, ExitReason.OOM, slot, policy),
        )
    }

    @Test
    fun `grows memory only as far as the platform can give one execution`() {
        val capped = slot.copy(memoryCap = MemoryCap.Limited(4096.0))
        val unit = units(count = 1, peakMemoryMb = 1500.0)

        val first = retryBags(unit, grant(parallelism = 1, heapMb = 1500), attempt = 1, ExitReason.OOM, capped, policy).single()
        val second = retryBags(first.units, first.grant, attempt = 2, ExitReason.OOM, capped, policy)

        assertTrue(first.grant.memoryRequestMb <= 4096, "the first retry still fits: ${first.grant.memoryRequestMb} MB")
        assertEquals(emptyList<PlannedBag>(), second, "twice that again is more than any execution here can have")
    }

    @Test
    fun `answers an overrun with more time, since splitting a bag does not shorten it`() {
        val failed = grant()

        val retry = retryBags(units(), failed, attempt = 1, ExitReason.TIMEOUT, slot, policy).single()

        assertEquals(4, retry.units.size, "every unit still runs at once, so the bag stays whole")
        assertTrue(retry.grant.timeLimitSeconds > failed.timeLimitSeconds, "an overrun needs longer, not smaller")
    }

    @Test
    fun `grows a limit up to the platform's cap and then gives up`() {
        val capped = slot.copy(timeCap = TimeCap.Limited(500))

        val first = retryBags(units(), grant(timeLimitSeconds = 330), attempt = 1, ExitReason.WALLTIME, capped, policy).single()
        val second = retryBags(units(), first.grant, attempt = 2, ExitReason.WALLTIME, capped, policy)

        assertEquals(500, first.grant.timeLimitSeconds, "the limit grows only as far as the cap")
        assertEquals(emptyList<PlannedBag>(), second, "at the cap, more time is not on offer")
    }

    @Test
    fun `retries a platform that gave no verdict without ever shortening the limit`() {
        val failed = grant(timeLimitSeconds = 5000)

        val retries = retryBags(units(), failed, attempt = 1, ExitReason.UNKNOWN, slot, policy)

        assertEquals(4, retries.sumOf { it.units.size })
        assertTrue(retries.all { it.grant.timeLimitSeconds >= failed.timeLimitSeconds })
    }
}
