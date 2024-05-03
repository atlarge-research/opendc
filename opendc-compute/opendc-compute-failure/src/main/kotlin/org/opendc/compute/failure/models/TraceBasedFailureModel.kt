/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.compute.failure.models

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opendc.compute.service.ComputeService
import org.opendc.trace.Trace
import org.opendc.trace.conv.FAILURE_DURATION
import org.opendc.trace.conv.FAILURE_INTENSITY
import org.opendc.trace.conv.FAILURE_INTERVAL
import org.opendc.trace.conv.TABLE_FAILURES
import java.io.File
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

/**
 * A definition of a Failure
 *
 * @property failureInterval The time between this and the previous failure in ms
 * @property failureDuration The Duration of the failure in ms
 * @property failureIntensity The ratio of hosts affected by the failure
 * @constructor Create empty Failure
 */
public data class Failure(
    val failureInterval: Long,
    val failureDuration: Long,
    val failureIntensity: Double,
) {
    init {
        require(failureInterval >= 0.0) { "A failure cannot start at a negative time" }
        require(failureDuration >= 0.0) { "A failure can not have a duration of 0 or less" }
        require(failureIntensity > 0.0 && failureInterval <= 1.0) { "The intensity of a failure has to be in the range (0.0, 1.0]" }
    }
}

/**
 * A [FailureModel] based on a provided parquet file
 * The file provides a list of [Failure] objects
 *
 *
 * @param context
 * @param clock
 * @param service
 * @param random
 * @param pathToTrace The path to the parquet file as a [String]
 */
public class TraceBasedFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: RandomGenerator,
    pathToTrace: String,
) : FailureModel(context, clock, service, random) {
    private val failureList = loadTrace(pathToTrace).iterator()

    override suspend fun runInjector() {
        while (failureList.hasNext()) {
            val failure = failureList.next()

            delay(failure.failureInterval - clock.millis())

            val victims = victimSelector.select(hosts, failure.failureIntensity)
            scope.launch {
                fault.apply(victims, failure.failureDuration)
            }
        }
    }

    /**
     * Load a list [Failure] objects from the provided [pathToFile]
     *
     * @param pathToFile
     */
    private fun loadTrace(pathToFile: String): List<Failure> {
        val trace = Trace.open(File(pathToFile), "failure")

        val reader = checkNotNull(trace.getTable(TABLE_FAILURES)).newReader()

        val failureStartTimeCol = reader.resolve(FAILURE_INTERVAL)
        val failureDurationCol = reader.resolve(FAILURE_DURATION)
        val failureIntensityCol = reader.resolve(FAILURE_INTENSITY)

        val entries = mutableListOf<Failure>()

        try {
            while (reader.nextRow()) {
                val failureStartTime = reader.getLong(failureStartTimeCol)
                val failureDuration = reader.getLong(failureDurationCol)
                val failureIntensity = reader.getDouble(failureIntensityCol)

                entries.add(Failure(failureStartTime, failureDuration, failureIntensity))
            }

            return entries
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            reader.close()
        }
    }
}
