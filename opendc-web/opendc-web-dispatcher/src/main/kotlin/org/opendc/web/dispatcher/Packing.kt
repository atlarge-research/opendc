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

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** One `(scenario, seed)` run with what it is expected to cost. */
data class PlannedUnit(
    val scenarioIndex: Int,
    val seed: Long,
    val cpuSeconds: Double,
    val peakMemoryMb: Double,
) {
    init {
        require(peakMemoryMb > 0.0) { "unit $scenarioIndex/$seed was estimated at $peakMemoryMb MB" }
        require(cpuSeconds >= 0.0) { "unit $scenarioIndex/$seed was estimated at $cpuSeconds s" }
    }
}

/**
 * What a platform is asked to give one execution. Memory is in whole megabytes, as platforms count it.
 *
 * @property parallelism How many units run at once, which is also how many cores are asked for.
 * @property heapMb What the units need between them, which is the process heap.
 * @property memoryRequestMb What the platform must reserve: the heap and the process around it.
 */
data class Grant(
    val parallelism: Int,
    val heapMb: Int,
    val memoryRequestMb: Int,
    val timeLimitSeconds: Int,
)

/**
 * One packed execution.
 *
 * @property makespanSeconds Expected wall clock, which elapsed time is later compared against.
 */
data class PlannedBag(
    val units: List<PlannedUnit>,
    val grant: Grant,
    val makespanSeconds: Double,
)

/**
 * What a bag is shaped against, and how far a failing one may be escalated before its work is given
 * up on.
 *
 * @property jvmBaselineMb The launcher JVM's own footprint, paid once however many units share it.
 * @property offHeapPerUnitMb What each unit holds outside the heap, such as parquet buffers.
 * @property heapHeadroom How much heap a unit is given per megabyte of its estimated peak.
 * @property startupSeconds Starting a launcher and fetching its inputs, likewise paid once.
 * @property timeSafetyFactor How far past its estimate the work in a bag may run before it is killed.
 * @property maxAttempts Including the first, so two means one retry.
 * @property growthFactor What a unit's memory or a bag's time is multiplied by when it was not enough.
 * @property maxMemoryRequestMb The largest grant worth asking for, on a platform that can give more.
 */
data class DispatchPolicy(
    val jvmBaselineMb: Double,
    val offHeapPerUnitMb: Double,
    val heapHeadroom: Double,
    val startupSeconds: Double,
    val timeSafetyFactor: Double,
    val maxAttempts: Int,
    val growthFactor: Double,
    val maxMemoryRequestMb: Double,
)

/** What a bag of [size] units, the largest of which peaks at [headPeakMb], asks the platform to reserve. */
fun requestMb(
    size: Int,
    headPeakMb: Double,
    policy: DispatchPolicy,
): Double = policy.jvmBaselineMb + policy.offHeapPerUnitMb * size + policy.heapHeadroom * size * headPeakMb

/** The largest grant worth asking [slot]'s platform for: the policy's ceiling, or the platform's where that is lower. */
private fun DispatchPolicy.requestCapMb(slot: ExecutionSlot): Double = slot.memoryCap.clamp(maxMemoryRequestMb)

/**
 * Packs units into bags, memory first.
 *
 * Units are ordered by peak memory and each bag is filled from the top, so a bag is
 * memory-homogeneous and its first unit decides both how many fit and what to ask for.
 *
 * A bag runs every unit it holds at once, so its parallelism is its size and its expected duration
 * is `max(unit)` rather than `sum / n`. A unit too large for the slot gets a bag of its own with an
 * oversized request.
 */
fun planBags(
    units: List<PlannedUnit>,
    slot: ExecutionSlot,
    policy: DispatchPolicy,
): List<PlannedBag> {
    val ordered =
        units.sortedWith(
            compareByDescending<PlannedUnit> { it.peakMemoryMb }.thenBy { it.scenarioIndex }.thenBy { it.seed },
        )
    val bags = mutableListOf<PlannedBag>()
    var index = 0

    while (index < ordered.size) {
        val head = ordered[index]
        val perUnit = policy.offHeapPerUnitMb + policy.heapHeadroom * head.peakMemoryMb
        val fits = floor((slot.memoryMb - policy.jvmBaselineMb) / perUnit).coerceIn(1.0, slot.cores.toDouble())
        val size = min(fits.toInt(), ordered.size - index)
        bags.add(ordered.subList(index, index + size).toList().toBag(slot, policy))
        index += size
    }
    return bags
}

/**
 * How a failed execution's units are tried again, or an empty list when nothing different is worth
 * trying: a failure no grant could change, or one that has used up its attempts.
 */
fun retryBags(
    units: List<PlannedUnit>,
    failed: Grant,
    attempt: Int,
    reason: ExitReason,
    slot: ExecutionSlot,
    policy: DispatchPolicy,
): List<PlannedBag> =
    if (!reason.isResourceShaped || attempt >= policy.maxAttempts) {
        emptyList()
    } else {
        escalatedBags(units, failed, reason, slot, policy)
    }

/**
 * The bags that answer the resource [reason] says ran out under [failed].
 *
 * Memory is answered per unit: each unit is grown to at least its share of the heap that was not
 * enough, so the estimate that packed it does not pack it the same way again, and the units are
 * re-packed around their new size. Time is answered with a longer limit, up to the slot's cap; a bag
 * already at the cap has nothing left to try. A death nobody explained is tried once more as it was.
 */
fun escalatedBags(
    units: List<PlannedUnit>,
    failed: Grant,
    reason: ExitReason,
    slot: ExecutionSlot,
    policy: DispatchPolicy,
): List<PlannedBag> =
    when (reason) {
        ExitReason.OOM -> {
            val share = failed.heapMb / (failed.parallelism * policy.heapHeadroom)
            val grown = units.map { it.copy(peakMemoryMb = policy.growthFactor * max(it.peakMemoryMb, share)) }
            if (grown.any { requestMb(1, it.peakMemoryMb, policy) > policy.requestCapMb(slot) }) {
                emptyList()
            } else {
                requeuedBags(grown, failed, slot, policy)
            }
        }

        ExitReason.TIMEOUT, ExitReason.WALLTIME -> {
            val longer = slot.timeCap.clamp(ceil(failed.timeLimitSeconds * policy.growthFactor).toInt())
            if (longer <= failed.timeLimitSeconds) {
                emptyList()
            } else {
                planBags(units, slot, policy).map { it.withLimitAtLeast(longer) }
            }
        }

        ExitReason.UNKNOWN -> requeuedBags(units, failed, slot, policy)

        ExitReason.OK, ExitReason.SIMULATION_ERROR, ExitReason.INVALID_SPEC, ExitReason.CANCELLED, ExitReason.REJECTED -> emptyList()
    }

/** [units] packed afresh, never given less time than [failed] had. */
fun requeuedBags(
    units: List<PlannedUnit>,
    failed: Grant,
    slot: ExecutionSlot,
    policy: DispatchPolicy,
): List<PlannedBag> = planBags(units, slot, policy).map { it.withLimitAtLeast(failed.timeLimitSeconds) }

/** A unit no bag can hold here, whatever else it is packed with. */
sealed interface Unfit {
    val unit: PlannedUnit

    /** Its estimate alone runs past the slot's time cap. */
    data class TooLong(
        override val unit: PlannedUnit,
        val seconds: Int,
        val capSeconds: Int,
    ) : Unfit

    /** It alone needs more memory than any grant worth asking for, or than the platform can give one execution. */
    data class TooLarge(
        override val unit: PlannedUnit,
        val memoryMb: Double,
        val capMb: Double,
    ) : Unfit
}

/**
 * The units that cannot run here at all.
 *
 * Judged on the estimate without the safety factor: a unit whose estimate fits but whose margin does
 * not is still run, with its limit clamped to the cap.
 */
fun unfit(
    units: List<PlannedUnit>,
    slot: ExecutionSlot,
    policy: DispatchPolicy,
): List<Unfit> =
    units.mapNotNull { unit ->
        val seconds = ceil(policy.startupSeconds + unit.cpuSeconds).toInt()
        val memory = requestMb(1, unit.peakMemoryMb, policy)
        val cap = slot.timeCap
        val memoryCap = policy.requestCapMb(slot)
        when {
            cap is TimeCap.Limited && seconds > cap.seconds -> Unfit.TooLong(unit, seconds, cap.seconds)
            memory > memoryCap -> Unfit.TooLarge(unit, memory, memoryCap)
            else -> null
        }
    }

private fun PlannedBag.withLimitAtLeast(seconds: Int): PlannedBag =
    copy(
        grant = grant.copy(timeLimitSeconds = max(grant.timeLimitSeconds, seconds)),
    )

private fun List<PlannedUnit>.toBag(
    slot: ExecutionSlot,
    policy: DispatchPolicy,
): PlannedBag {
    val head = maxOf { it.peakMemoryMb }
    val makespan = maxOf { it.cpuSeconds }
    // Starting a launcher costs the same whatever it then runs, so it is added rather than
    // multiplied: a factor over a short bag's estimate is all startup and no work.
    val limit = ceil(policy.startupSeconds + policy.timeSafetyFactor * makespan).toInt().coerceAtLeast(1)
    return PlannedBag(
        units = this,
        grant =
            Grant(
                parallelism = size,
                heapMb = ceil(policy.heapHeadroom * size * head).toInt().coerceAtLeast(1),
                memoryRequestMb = ceil(requestMb(size, head, policy)).toInt(),
                timeLimitSeconds = slot.timeCap.clamp(limit),
            ),
        makespanSeconds = makespan,
    )
}
