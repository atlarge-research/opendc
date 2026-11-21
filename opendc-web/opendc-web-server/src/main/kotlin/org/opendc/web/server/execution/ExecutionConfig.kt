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

package org.opendc.web.server.execution

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import org.opendc.web.dispatcher.DispatchPolicy
import org.opendc.web.dispatcher.estimate.EstimatorCoefficients
import java.time.Duration

/** The longest a presigned object-storage URL can last, and so the longest a launcher's URLs may. */
const val MAX_URL_LIFETIME = "P7D"

/** How work is estimated, shaped and escalated, and where it runs. */
@ConfigMapping(prefix = "opendc.execution")
interface ExecutionConfig {
    fun dispatcher(): DispatcherKind

    /** Where a launcher posts its progress: absolute, and reachable from wherever the launcher runs. */
    fun telemetryUrl(): String

    /** How long a launcher's manifest, input and output URLs stay valid: the longest queue wait plus the run. */
    @WithDefault(MAX_URL_LIFETIME)
    fun urlLifetime(): Duration

    /** How long to leave the platform alone after it could not take an execution. */
    @WithDefault("PT30S")
    fun launchRetryDelay(): Duration

    fun estimator(): EstimatorSettings

    fun packing(): PackingSettings

    fun retry(): RetrySettings

    /**
     * The terms of the resource model, each described on [EstimatorCoefficients]. The defaults are
     * fitted to launcher runs over the sample traces on one ordinary machine; the two multipliers
     * correct for another: raise them where estimates come out short.
     */
    interface EstimatorSettings {
        @WithDefault("80.0")
        fun baseMemoryMb(): Double

        @WithDefault("0.03")
        fun memoryPerHostMb(): Double

        @WithDefault("80.0")
        fun memoryPerMillionFragmentsMb(): Double

        @WithDefault("2500.0")
        fun memoryPerMillionTasksMb(): Double

        @WithDefault("2.0")
        fun baseSeconds(): Double

        @WithDefault("1.2")
        fun loadSecondsPerMillionFragments(): Double

        @WithDefault("1.6")
        fun simulateSecondsPerMillionFragments(): Double

        @WithDefault("1800.0")
        fun secondsPerMillionTasks(): Double

        @WithDefault("0.005")
        fun exportCostPerHost(): Double

        @WithDefault("0.25")
        fun failureOverhead(): Double

        @WithDefault("0.15")
        fun checkpointOverhead(): Double

        @WithDefault("1.0")
        fun runtimeMultiplier(): Double

        @WithDefault("1.0")
        fun memoryMultiplier(): Double
    }

    /** Each setting is described on [DispatchPolicy]. */
    interface PackingSettings {
        @WithDefault("256.0")
        fun jvmBaselineMb(): Double

        @WithDefault("0.0")
        fun offHeapPerUnitMb(): Double

        @WithDefault("1.0")
        fun heapHeadroom(): Double

        @WithDefault("30.0")
        fun startupSeconds(): Double

        @WithDefault("3.0")
        fun timeSafetyFactor(): Double
    }

    interface RetrySettings {
        @WithDefault("3")
        fun maxAttempts(): Int

        @WithDefault("2.0")
        fun growthFactor(): Double

        @WithDefault("32768.0")
        fun maxMemoryRequestMb(): Double
    }
}

fun ExecutionConfig.EstimatorSettings.toCoefficients(): EstimatorCoefficients =
    EstimatorCoefficients(
        baseMemoryMb = baseMemoryMb(),
        memoryPerHostMb = memoryPerHostMb(),
        memoryPerMillionFragmentsMb = memoryPerMillionFragmentsMb(),
        memoryPerMillionTasksMb = memoryPerMillionTasksMb(),
        baseSeconds = baseSeconds(),
        loadSecondsPerMillionFragments = loadSecondsPerMillionFragments(),
        simulateSecondsPerMillionFragments = simulateSecondsPerMillionFragments(),
        secondsPerMillionTasks = secondsPerMillionTasks(),
        exportCostPerHost = exportCostPerHost(),
        failureOverhead = failureOverhead(),
        checkpointOverhead = checkpointOverhead(),
    )

fun ExecutionConfig.toPolicy(): DispatchPolicy =
    DispatchPolicy(
        jvmBaselineMb = packing().jvmBaselineMb(),
        offHeapPerUnitMb = packing().offHeapPerUnitMb(),
        heapHeadroom = packing().heapHeadroom(),
        startupSeconds = packing().startupSeconds(),
        timeSafetyFactor = packing().timeSafetyFactor(),
        maxAttempts = retry().maxAttempts(),
        growthFactor = retry().growthFactor(),
        maxMemoryRequestMb = retry().maxMemoryRequestMb(),
    )
