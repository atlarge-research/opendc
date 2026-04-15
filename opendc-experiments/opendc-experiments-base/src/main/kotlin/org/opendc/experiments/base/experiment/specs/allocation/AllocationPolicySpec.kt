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

package org.opendc.experiments.base.experiment.specs.allocation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.scheduler.ComputeSchedulerEnum
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.createPrefabComputeScheduler
import org.opendc.compute.simulator.scheduler.portfolio.CombinedDORUtility
import org.opendc.compute.simulator.scheduler.portfolio.DisasterRecoveryRiskUtility
import org.opendc.compute.simulator.scheduler.portfolio.OperationalRiskUtility
import org.opendc.compute.simulator.scheduler.portfolio.PortfolioScheduler
import org.opendc.compute.simulator.scheduler.portfolio.UtilityFunction
import org.opendc.compute.simulator.scheduler.timeshift.MemorizingTimeshift
import org.opendc.compute.simulator.scheduler.timeshift.TaskStopper
import org.opendc.compute.simulator.scheduler.timeshift.TimeshiftScheduler
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

/**
 * specification describing how tasks are allocated
 */
@Serializable
public sealed interface AllocationPolicySpec

@Serializable
@SerialName("prefab")
public data class PrefabAllocationPolicySpec(
    val policyName: ComputeSchedulerEnum = ComputeSchedulerEnum.Mem,
    val cpuAllocationRatio: Double = 1.0,
    val ramAllocationRatio: Double = 1.0,
    val gpuAllocationRatio: Double = 1.0,
) : AllocationPolicySpec {
    public val name: String = policyName.toString()
}

@Serializable
@SerialName("filter")
public data class FilterAllocationPolicySpec(
    val filters: List<HostFilterSpec> = listOf(ComputeFilterSpec()),
    val weighers: List<HostWeigherSpec> = emptyList(),
    val subsetSize: Int = 1,
) : AllocationPolicySpec

@Serializable
public sealed interface UtilityFunctionSpec

@Serializable
@SerialName("operational-risk")
public data class OperationalRiskSpec(
    val forwardLookMs: Long = 3600000L,
) : UtilityFunctionSpec

@Serializable
@SerialName("disaster-recovery-risk")
public data class DisasterRecoveryRiskSpec(val forwardLookMs: Long = 3600000L) : UtilityFunctionSpec

@Serializable
@SerialName("combined-dor")
public data class CombinedDORSpec(
    val operationalWeight: Double = 1.0,
    val disasterRecoveryWeight: Double = 1.0,
    val forwardLookMs: Long = 3600000L,
) : UtilityFunctionSpec

@Serializable
@SerialName("portfolio")
public data class PortfolioAllocationPolicySpec(
    val policies: List<AllocationPolicySpec> =
        listOf(
            PrefabAllocationPolicySpec(ComputeSchedulerEnum.Mem),
            PrefabAllocationPolicySpec(ComputeSchedulerEnum.CoreMem),
            PrefabAllocationPolicySpec(ComputeSchedulerEnum.ActiveServers),
            PrefabAllocationPolicySpec(ComputeSchedulerEnum.Random),
            PrefabAllocationPolicySpec(ComputeSchedulerEnum.ProvisionedCores),
        ),
    val utilityFunction: UtilityFunctionSpec = OperationalRiskSpec(),
    val cpuAllocationRatio: Double = 1.0,
    val ramAllocationRatio: Double = 1.0,
    val gpuAllocationRatio: Double = 1.0,
) : AllocationPolicySpec

@Serializable
@SerialName("timeshift")
public data class TimeShiftAllocationPolicySpec(
    val filters: List<HostFilterSpec> = listOf(ComputeFilterSpec()),
    val weighers: List<HostWeigherSpec> = emptyList(),
    val windowSize: Int = 168,
    val subsetSize: Int = 1,
    val forecast: Boolean = true,
    val shortForecastThreshold: Double = 0.2,
    val longForecastThreshold: Double = 0.35,
    val forecastSize: Int = 24,
    val taskStopper: TaskStopperSpec? = null,
    val memorize: Boolean = true,
    val cpuAllocationRatio: Double = 1.0,
    val ramAllocationRatio: Double = 1.0,
    val gpuAllocationRatio: Double = 1.0,
) : AllocationPolicySpec

public fun createComputeScheduler(
    spec: AllocationPolicySpec,
    seeder: RandomGenerator,
    clock: InstantSource,
    numHosts: Int = 1000,
): ComputeScheduler {
    return when (spec) {
        is PrefabAllocationPolicySpec ->
            createPrefabComputeScheduler(
                spec.policyName,
                seeder,
                clock,
                numHosts,
                spec.cpuAllocationRatio,
                spec.ramAllocationRatio,
                spec.gpuAllocationRatio,
            )
        is FilterAllocationPolicySpec -> {
            val filters = spec.filters.map { createHostFilter(it) }
            val weighers = spec.weighers.map { createHostWeigher(it) }
            FilterScheduler(filters, weighers, spec.subsetSize, seeder, numHosts)
        }
        is PortfolioAllocationPolicySpec -> {
            val resolvedPolicies =
                spec.policies.map { policy ->
                    when (policy) {
                        is PrefabAllocationPolicySpec ->
                            policy.copy(
                                cpuAllocationRatio = policy.cpuAllocationRatio.takeUnless { it == 1.0 } ?: spec.cpuAllocationRatio,
                                ramAllocationRatio = policy.ramAllocationRatio.takeUnless { it == 1.0 } ?: spec.ramAllocationRatio,
                                gpuAllocationRatio = policy.gpuAllocationRatio.takeUnless { it == 1.0 } ?: spec.gpuAllocationRatio,
                            )
                        else -> policy
                    }
                }
            val subPolicies = resolvedPolicies.map { createComputeScheduler(it, seeder, clock, numHosts) }
            val utilityFunction = createUtilityFunction(spec.utilityFunction)
            val orUtility: OperationalRiskUtility?
            val drrUtility: DisasterRecoveryRiskUtility?
            when (spec.utilityFunction) {
                is OperationalRiskSpec -> {
                    orUtility = utilityFunction as OperationalRiskUtility
                    drrUtility = DisasterRecoveryRiskUtility()
                }
                is DisasterRecoveryRiskSpec -> {
                    orUtility = OperationalRiskUtility(spec.utilityFunction.forwardLookMs)
                    drrUtility = utilityFunction as DisasterRecoveryRiskUtility
                }
                is CombinedDORSpec -> {
                    orUtility = OperationalRiskUtility(spec.utilityFunction.forwardLookMs)
                    drrUtility = DisasterRecoveryRiskUtility()
                }
            }
            PortfolioScheduler(subPolicies, utilityFunction, clock, orUtility = orUtility, drrUtility = drrUtility)
        }
        is TimeShiftAllocationPolicySpec -> {
            val filters = spec.filters.map { createHostFilter(it) }
            val weighers = spec.weighers.map { createHostWeigher(it) }
            if (spec.memorize) {
                MemorizingTimeshift(
                    filters,
                    spec.windowSize,
                    clock,
                    spec.forecast,
                    spec.shortForecastThreshold,
                    spec.longForecastThreshold,
                    spec.forecastSize,
                )
            } else {
                TimeshiftScheduler(
                    filters, weighers, spec.windowSize, clock, spec.subsetSize, spec.forecast,
                    spec.shortForecastThreshold, spec.longForecastThreshold, spec.forecastSize, seeder,
                )
            }
        }
    }
}

@Serializable
@SerialName("taskstopper")
public data class TaskStopperSpec(
    val windowSize: Int = 168,
    val forecast: Boolean = true,
    val forecastThreshold: Double = 0.6,
    val forecastSize: Int = 24,
)

public fun createTaskStopper(
    spec: TaskStopperSpec?,
    context: CoroutineContext,
    clock: InstantSource,
): TaskStopper? {
    val taskStopper =
        if (spec != null) {
            TaskStopper(
                clock,
                context,
                spec.forecast,
                spec.forecastThreshold,
                spec.forecastSize,
                spec.windowSize,
            )
        } else {
            null
        }

    return taskStopper
}

public fun createUtilityFunction(spec: UtilityFunctionSpec): UtilityFunction {
    return when (spec) {
        is OperationalRiskSpec -> OperationalRiskUtility(spec.forwardLookMs)
        is DisasterRecoveryRiskSpec -> DisasterRecoveryRiskUtility()
        is CombinedDORSpec -> CombinedDORUtility(spec.operationalWeight, spec.disasterRecoveryWeight, spec.forwardLookMs)
    }
}
