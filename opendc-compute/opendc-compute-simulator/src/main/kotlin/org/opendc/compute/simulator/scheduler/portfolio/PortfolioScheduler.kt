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

package org.opendc.compute.simulator.scheduler.portfolio

import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.scheduler.SchedulingRequest
import org.opendc.compute.simulator.scheduler.SchedulingResult
import org.opendc.compute.simulator.scheduler.SchedulingResultType
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask
import java.time.InstantSource

/**
 * A meta-scheduler that dynamically selects the best scheduling policy from a portfolio,
 * guided by a risk-aware [UtilityFunction].
 *
 * Based on "Portfolio Scheduling for Managing Operational and Disaster-Recovery Risks
 * in Virtualized Datacenters Hosting Business-Critical Workloads" (van Beek et al., ISPDC 2019).
 *
 * For each scheduling decision, the portfolio scheduler evaluates each candidate policy by
 * simulating its placement decision (via [ComputeScheduler.evaluatePlacement]) and computing
 * the resulting risk score. The policy with the lowest risk score is selected, and the actual
 * scheduling is delegated to it. All other policies are kept in sync via
 * [ComputeScheduler.notifyPlacement].
 *
 * @param policies The portfolio of candidate scheduling policies.
 * @param utilityFunction The utility function used to evaluate placements.
 */
public class PortfolioScheduler(
    private val policies: List<ComputeScheduler>,
    private val utilityFunction: UtilityFunction,
    private val clock: InstantSource? = null,
) : ComputeScheduler {
    private val hosts = mutableSetOf<HostView>()
    private var lastSelectedPolicyIndex: Int = 0
    private var nextDecisionId: Long = 0
    private val pendingRecords = mutableListOf<SchedulingDecisionRecord>()

    init {
        require(policies.isNotEmpty()) { "Portfolio must contain at least one policy" }
    }

    /**
     * Returns the index of the most recently selected policy.
     */
    public fun getLastSelectedPolicyIndex(): Int = lastSelectedPolicyIndex

    /**
     * Drains and returns all pending scheduling decision records.
     */
    public fun drainDecisionRecords(): List<SchedulingDecisionRecord> {
        val records = pendingRecords.toList()
        pendingRecords.clear()
        return records
    }

    override fun addHost(host: HostView) {
        hosts.add(host)
        policies.forEach { it.addHost(host) }
    }

    override fun removeHost(host: HostView) {
        hosts.remove(host)
        policies.forEach { it.removeHost(host) }
    }

    override fun failHost(host: HostView) {
        policies.forEach { it.failHost(host) }
    }

    override fun restartHost(host: HostView) {
        policies.forEach { it.restartHost(host) }
    }

    override fun updateHost(host: HostView) {
        policies.forEach { it.updateHost(host) }
    }

    override fun setHostEmpty(hostView: HostView) {
        policies.forEach { it.setHostEmpty(hostView) }
    }

    override fun select(iter: MutableIterator<SchedulingRequest>): SchedulingResult {
        // Find the first non-cancelled request
        var req: SchedulingRequest? = null
        while (iter.hasNext()) {
            val candidate = iter.next()
            if (candidate.isCancelled) {
                iter.remove()
                continue
            }
            req = candidate
            break
        }

        if (req == null) {
            return SchedulingResult(SchedulingResultType.EMPTY)
        }

        val task = req.task
        val decisionId = nextDecisionId++

        // Evaluate each policy: which host would it pick?
        data class PolicyEvaluation(val index: Int, val host: HostView?, val score: Double)

        val evaluations = mutableListOf<PolicyEvaluation>()
        var bestHost: HostView? = null
        var bestScore = Double.MAX_VALUE
        var bestPolicyIndex = 0

        for ((index, policy) in policies.withIndex()) {
            val candidateHost = policy.evaluatePlacement(task)
            val score =
                if (candidateHost != null) {
                    evaluateWithPlacement(candidateHost, task)
                } else {
                    Double.MAX_VALUE
                }
            evaluations.add(PolicyEvaluation(index, candidateHost, score))
            if (candidateHost != null && score < bestScore) {
                bestScore = score
                bestHost = candidateHost
                bestPolicyIndex = index
            }
        }

        if (bestHost == null) {
            // Record all evaluations even on failure
            for (eval in evaluations) {
                pendingRecords.add(
                    SchedulingDecisionRecord(
                        decisionId = decisionId,
                        timestampMs = clock?.instant()?.toEpochMilli() ?: 0,
                        taskId = task.id,
                        policyIndex = eval.index,
                        candidateHostName = eval.host?.host?.getName() ?: "",
                        score = eval.score,
                        selected = false,
                        winningScore = Double.MAX_VALUE,
                    ),
                )
            }
            return SchedulingResult(SchedulingResultType.FAILURE, null, req)
        }

        // Remove the request from the queue
        iter.remove()

        lastSelectedPolicyIndex = bestPolicyIndex

        // Record decision records for all policies
        for (eval in evaluations) {
            pendingRecords.add(
                SchedulingDecisionRecord(
                    decisionId = decisionId,
                    timestampMs = clock?.instant()?.toEpochMilli() ?: 0,
                    taskId = task.id,
                    policyIndex = eval.index,
                    candidateHostName = eval.host?.host?.getName() ?: "",
                    score = eval.score,
                    selected = eval.index == bestPolicyIndex,
                    winningScore = bestScore,
                ),
            )
        }

        // Notify ALL policies of the placement (including the winning one) for state sync
        for (policy in policies) {
            policy.notifyPlacement(bestHost, task)
        }

        return SchedulingResult(SchedulingResultType.SUCCESS, bestHost, req)
    }

    override fun evaluatePlacement(task: ServiceTask): HostView? {
        // Evaluate all policies and return the host with the best score
        var bestHost: HostView? = null
        var bestScore = Double.MAX_VALUE

        for (policy in policies) {
            val candidateHost = policy.evaluatePlacement(task) ?: continue
            val score = evaluateWithPlacement(candidateHost, task)
            if (score < bestScore) {
                bestScore = score
                bestHost = candidateHost
            }
        }
        return bestHost
    }

    override fun notifyPlacement(
        host: HostView,
        task: ServiceTask,
    ) {
        policies.forEach { it.notifyPlacement(host, task) }
    }

    override fun removeTask(
        task: ServiceTask,
        host: HostView?,
    ) {
        policies.forEach { it.removeTask(task, host) }
    }

    /**
     * Evaluate the utility function score assuming a task is placed on the given host.
     */
    private fun evaluateWithPlacement(
        host: HostView,
        task: ServiceTask,
    ): Double {
        return utilityFunction.evaluate(hosts, SimulatedPlacement(host, task))
    }
}
