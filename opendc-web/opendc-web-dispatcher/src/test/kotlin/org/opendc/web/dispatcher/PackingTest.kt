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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Packing decides what a dispatcher asks a cluster for, so the properties worth pinning are the ones
 * that cost real money or kill real runs: never asking for more memory than a slot holds, never
 * dropping work, and never giving a bag less time than the work inside it needs.
 */
class PackingTest {
    private val slot = ExecutionSlot(cores = 4, memoryMb = 4096.0, timeCap = TimeCap.Unlimited)
    private val policy =
        DispatchPolicy(
            jvmBaselineMb = JVM_BASELINE_MB,
            offHeapPerUnitMb = 0.0,
            heapHeadroom = 1.0,
            startupSeconds = STARTUP_SECONDS,
            timeSafetyFactor = TIME_SAFETY_FACTOR,
            maxAttempts = 3,
            growthFactor = 2.0,
            maxMemoryRequestMb = 32_768.0,
        )

    private fun unit(
        scenarioIndex: Int,
        seed: Long = 0,
        cpuSeconds: Double = 45.0,
        peakMemoryMb: Double = 256.0,
    ) = PlannedUnit(scenarioIndex, seed, cpuSeconds, peakMemoryMb)

    private fun bags(units: List<PlannedUnit>) = planBags(units, slot, policy)

    @Test
    fun `packs nothing into nothing rather than an empty bag`() {
        assertEquals(emptyList<PlannedBag>(), bags(emptyList()))
    }

    @Test
    fun `keeps every unit exactly once, so no work is dropped or run twice`() {
        val units = (0 until 40).map { unit(scenarioIndex = it, peakMemoryMb = 200.0 + it * 40.0) }

        val packed = bags(units).flatMap { it.units }

        assertEquals(units.size, packed.size)
        assertEquals(units.toSet(), packed.toSet())
    }

    @Test
    fun `gives a bag at least as long as its slowest unit needs, however skewed the bag`() {
        // One long unit among short ones. Treating a bag's duration as its summed work divided by
        // its parallelism puts the limit below what the long unit alone requires.
        val units =
            listOf(
                unit(0, cpuSeconds = 400.0, peakMemoryMb = 64.0),
                unit(1, cpuSeconds = 10.0, peakMemoryMb = 64.0),
                unit(2, cpuSeconds = 10.0, peakMemoryMb = 64.0),
                unit(3, cpuSeconds = 10.0, peakMemoryMb = 64.0),
            )

        for (bag in bags(units)) {
            val slowest = bag.units.maxOf { it.cpuSeconds }
            assertTrue(
                bag.grant.timeLimitSeconds >= slowest,
                "bag was given ${bag.grant.timeLimitSeconds}s for a unit needing ${slowest}s",
            )
            assertEquals(slowest, bag.makespanSeconds)
        }
    }

    // A SLURM partition kills anything past its walltime however long it was asked to run, so a
    // limit past the cap only promises time the platform will not give.
    @Test
    fun `never gives a bag more time than the platform allows`() {
        val capped = slot.copy(timeCap = TimeCap.Limited(900))

        val bag = planBags(listOf(unit(0, cpuSeconds = 10_000.0)), capped, policy).single()

        assertEquals(900, bag.grant.timeLimitSeconds)
    }

    @Test
    fun `runs every unit in a bag at once, so no unit waits on another`() {
        val units = (0 until 30).map { unit(scenarioIndex = it, peakMemoryMb = 100.0 + it * 100.0) }

        for (bag in bags(units)) {
            assertEquals(bag.units.size, bag.grant.parallelism)
        }
    }

    @Test
    fun `never requests more memory than the slot holds, for any mix of unit sizes`() {
        val units = (0 until 30).map { unit(scenarioIndex = it, peakMemoryMb = 100.0 + it * 100.0) }

        for (bag in bags(units)) {
            val largest = bag.units.maxOf { it.peakMemoryMb }
            // A unit too large to ever fit is the documented exception: it gets an honest oversized
            // request rather than being silently dropped or split.
            if (JVM_BASELINE_MB + largest <= slot.memoryMb) {
                assertTrue(
                    bag.grant.memoryRequestMb <= slot.memoryMb,
                    "bag asked for ${bag.grant.memoryRequestMb} MB against a ${slot.memoryMb} MB slot",
                )
            }
        }
    }

    @Test
    fun `sizes a bag from its hungriest unit, not its average`() {
        val first = bags(listOf(unit(0, peakMemoryMb = 3000.0), unit(1, peakMemoryMb = 100.0))).first()

        assertEquals(1, first.grant.parallelism, "3000 MB leaves no room for a second concurrent unit")
        assertEquals((JVM_BASELINE_MB + 3000.0).toInt(), first.grant.memoryRequestMb)
        assertEquals(3000, first.grant.heapMb)
    }

    @Test
    fun `runs small units concurrently up to the core count, not beyond it`() {
        val units = (0 until 8).map { unit(scenarioIndex = it, peakMemoryMb = 64.0) }

        val parallelism = bags(units).first().grant.parallelism

        assertEquals(slot.cores, parallelism, "64 MB units fit far more than 4, but a slot has 4 cores")
    }

    @Test
    fun `gives a unit larger than the whole slot its own bag rather than dropping it`() {
        val oversized = bags(listOf(unit(0, peakMemoryMb = 10_000.0)))

        assertEquals(1, oversized.size)
        assertEquals(1, oversized.first().grant.parallelism)
        assertEquals(1, oversized.first().units.size)
    }

    // A request no node can satisfy is never placed, so a platform smaller than the policy's ceiling
    // has to rule the unit out before it is queued, not after it has waited for room.
    @Test
    fun `rules out a unit larger than the platform can give one execution, under the policy's ceiling too`() {
        val large = unit(0, peakMemoryMb = 10_000.0)
        val capped = slot.copy(memoryCap = MemoryCap.Limited(8192.0))

        val unfit = unfit(listOf(large), capped, policy).single()

        assertEquals(Unfit.TooLarge(large, 10_000.0 + JVM_BASELINE_MB, 8192.0), unfit)
        assertEquals(emptyList<Unfit>(), unfit(listOf(large), slot, policy), "with no cap of its own, the unit runs alone")
    }

    @Test
    fun `rules out a unit larger than the policy's ceiling on a platform that could give more`() {
        val huge = unit(0, peakMemoryMb = 40_000.0)

        val unfit = unfit(listOf(huge), slot.copy(memoryCap = MemoryCap.Limited(65_536.0)), policy).single()

        assertEquals(policy.maxMemoryRequestMb, (unfit as Unfit.TooLarge).capMb)
    }

    @Test
    fun `refuses an estimate of no memory instead of packing a slot into infinite parts`() {
        assertThrows(IllegalArgumentException::class.java) { unit(0, peakMemoryMb = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { unit(0, peakMemoryMb = -1.0) }
    }

    @Test
    fun `is deterministic, so the same submission always plans the same bags`() {
        val units = (0 until 25).map { unit(scenarioIndex = it % 5, seed = it.toLong(), peakMemoryMb = 256.0) }

        assertEquals(bags(units), bags(units.shuffled()))
    }

    // A launcher spends seconds starting a JVM and loading its inputs before the first run begins.
    // A limit that is only a multiple of the work kills every short bag on its first attempt.
    @Test
    fun `leaves room for the launcher to start, not only for the work`() {
        val bag = bags(listOf(unit(0, cpuSeconds = 1.0))).single()

        assertTrue(
            bag.grant.timeLimitSeconds >= STARTUP_SECONDS,
            "a one-second bag was given ${bag.grant.timeLimitSeconds}s, less than it takes to start",
        )
    }

    private companion object {
        const val JVM_BASELINE_MB = 512.0
        const val STARTUP_SECONDS = 30.0
        const val TIME_SAFETY_FACTOR = 3.0
    }
}
