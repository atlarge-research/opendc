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

import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.server.model.BudgetPeriod
import org.opendc.web.server.model.BudgetWindow
import org.opendc.web.server.model.PlanTier
import org.opendc.web.server.model.RunUnit
import org.opendc.web.server.model.SimulationCap
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.rest.DocumentIssue
import org.opendc.web.server.rest.conflict
import java.time.Duration
import java.time.Instant

private const val SECONDS_PER_HOUR = 3600.0

private const val MILLIS_PER_SECOND = 1000.0

// Placeholder plan caps until the product settles real numbers; a window can be raised by hand.
private const val FREE_SESSION_HOURS = 1.0
private const val FREE_WEEK_HOURS = 10.0
private const val EDUCATION_SESSION_HOURS = 4.0
private const val EDUCATION_WEEK_HOURS = 40.0

/** What each plan may simulate per window. */
fun planCap(
    tier: PlanTier,
    period: BudgetPeriod,
): SimulationCap =
    when (tier) {
        PlanTier.FREE -> hours(period, session = FREE_SESSION_HOURS, week = FREE_WEEK_HOURS)
        PlanTier.EDUCATION -> hours(period, session = EDUCATION_SESSION_HOURS, week = EDUCATION_WEEK_HOURS)
        PlanTier.ENTERPRISE -> SimulationCap.Unlimited
    }

private fun hours(
    period: BudgetPeriod,
    session: Double,
    week: Double,
): SimulationCap =
    SimulationCap.Limited(
        SECONDS_PER_HOUR *
            when (period) {
                BudgetPeriod.SESSION -> session
                BudgetPeriod.WEEK -> week
            },
    )

/** Gives a new account its windows. An account with none, like the local one, is not metered. */
fun openWindows(
    user: UserAccount,
    now: Instant,
) {
    for (period in BudgetPeriod.entries) {
        val window = BudgetWindow()
        window.user = user
        window.period = period
        window.cap = planCap(user.planTier, period)
        window.resetsAt = now.plus(period.length)
        window.persist()
    }
}

/**
 * Refuses work quoted at [seconds] that would take [payer] past a limit, counting what their
 * unfinished work already holds in reserve. Admissions of one account take turns on its windows,
 * so two submits cannot both squeeze under the same limit.
 */
fun admit(
    payer: UserAccount,
    seconds: Double,
    now: Instant,
) {
    val windows = BudgetWindow.lockByUser(payer.id)
    if (windows.isEmpty()) {
        return
    }
    val reserved = RunUnit.reservedBy(payer.id)
    val short =
        windows.mapNotNull { window ->
            val cap = window.cap
            val left = if (cap is SimulationCap.Limited) cap.seconds - window.usedAt(now) - reserved else Double.POSITIVE_INFINITY
            if (seconds <= left) {
                null
            } else {
                val until = if (now.isBefore(window.resetsAt)) window.resetsAt else now.plus(window.period.length)
                DocumentIssue(
                    "budget.${window.period.name.lowercase()}",
                    "needs ${seconds.toLong()} s, ${left.coerceAtLeast(0.0).toLong()} s left until $until",
                )
            }
        }
    if (short.isNotEmpty()) {
        throw conflict("Not enough simulation budget left", short)
    }
}

/** Charges [payer] for [seconds] of simulation, starting a window afresh once it has run out. */
fun charge(
    payer: UserAccount,
    seconds: Double,
    now: Instant,
) {
    if (seconds <= 0.0) {
        return
    }
    for (window in BudgetWindow.lockByUser(payer.id)) {
        if (!now.isBefore(window.resetsAt)) {
            window.usedSeconds = 0.0
            window.resetsAt = now.plus(window.period.length)
        }
        window.usedSeconds += seconds
    }
}

/**
 * What an execution that ended for [reason] costs: the cores it held for as long as the platform
 * ran it. A failure shaped by how it was sized or by the platform is ours, not the user's, so it
 * costs nothing; so does work that never started.
 */
fun chargedSeconds(
    reason: ExitReason,
    span: PlatformSpan,
    parallelism: Int,
): Double {
    val billable =
        when (reason) {
            ExitReason.OK, ExitReason.SIMULATION_ERROR, ExitReason.INVALID_SPEC, ExitReason.CANCELLED -> true
            ExitReason.OOM, ExitReason.TIMEOUT, ExitReason.WALLTIME, ExitReason.UNKNOWN, ExitReason.REJECTED -> false
        }
    return when (span) {
        PlatformSpan.NotStarted -> 0.0
        is PlatformSpan.Ran -> if (billable) coreSeconds(span.startedAt, span.endedAt, parallelism) else 0.0
    }
}

/** What holding [cores] from [from] to [to] costs, which is what every charge is made of. */
fun coreSeconds(
    from: Instant,
    to: Instant,
    cores: Int,
): Double = Duration.between(from, to).toMillis() / MILLIS_PER_SECOND * cores
