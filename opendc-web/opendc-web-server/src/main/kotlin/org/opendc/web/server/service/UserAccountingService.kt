/*
 * Copyright (c) 2022 AtLarge Research
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

import org.eclipse.microprofile.config.inject.ConfigProperty
import org.opendc.web.server.model.UserAccounting
import org.opendc.web.server.repository.UserAccountingRepository
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.persistence.EntityExistsException
import org.opendc.web.proto.user.UserAccounting as UserAccountingDto

/**
 * Service for tracking the simulation budget of users.
 *
 * @param repository The [UserAccountingRepository] used to communicate with the database.
 * @param simulationBudget The default simulation budget for new users.
 */
@ApplicationScoped
class UserAccountingService @Inject constructor(
    private val repository: UserAccountingRepository,
    @ConfigProperty(name = "opendc.accounting.simulation-budget", defaultValue = "2000m")
    private val simulationBudget: Duration
) {
    /**
     * Return the [UserAccountingDto] object for the user with the specified [userId]. If the object does not exist in the
     * database, a default value is constructed.
     */
    fun getAccounting(userId: String): UserAccountingDto {
        val accounting = repository.findForUser(userId)
        return if (accounting != null) {
            UserAccountingDto(accounting.periodEnd, accounting.simulationTime, accounting.simulationTimeBudget)
        } else {
            UserAccountingDto(getNextAccountingPeriod(), 0, simulationBudget.toSeconds().toInt())
        }
    }

    /**
     * Determine whether the user with [userId] has any remaining simulation budget.
     *
     * @param userId The unique identifier of the user.
     * @return `true` when the user still has budget left, `false` otherwise.
     */
    fun hasSimulationBudget(userId: String): Boolean {
        val accounting = repository.findForUser(userId) ?: return true
        val today = LocalDate.now()

        // The accounting period must be over or there must be budget remaining.
        return !today.isBefore(accounting.periodEnd) || accounting.simulationTimeBudget > accounting.simulationTime
    }

    /**
     * Consume [seconds] from the simulation budget of the user with [userId].
     *
     * @param userId The unique identifier of the user.
     * @param seconds The seconds to consume from the simulation budget.
     * @param `true` if the user has consumed his full budget or `false` if there is still budget remaining.
     */
    fun consumeSimulationBudget(userId: String, seconds: Int): Boolean {
        val today = LocalDate.now()
        val nextAccountingPeriod = getNextAccountingPeriod(today)
        val repository = repository

        // We need to be careful to prevent conflicts in case of concurrency
        // 1. First, we try to create the accounting object if it does not exist yet. This may fail if another instance
        //    creates the object concurrently.
        // 2. Second, we check if the budget needs to be reset and try this atomically.
        // 3. Finally, we atomically consume the budget from the object
        // This is repeated three times in case there is a conflict
        repeat(3) {
            val accounting = repository.findForUser(userId)

            if (accounting == null) {
                try {
                    val newAccounting = UserAccounting(userId, nextAccountingPeriod, simulationBudget.toSeconds().toInt())
                    newAccounting.simulationTime = seconds
                    repository.save(newAccounting)

                    return newAccounting.simulationTime >= newAccounting.simulationTimeBudget
                } catch (e: EntityExistsException) {
                    // Conflict due to concurrency; retry
                }
            } else {
                val success = if (!today.isBefore(accounting.periodEnd)) {
                    repository.resetBudget(accounting, nextAccountingPeriod, seconds)
                } else {
                    repository.consumeBudget(accounting, seconds)
                }

                if (success) {
                    return accounting.simulationTimeBudget <= accounting.simulationTime
                }
            }
        }

        throw IllegalStateException("Failed to allocate consume budget due to conflict")
    }

    /**
     * Helper method to find next accounting period.
     */
    private fun getNextAccountingPeriod(today: LocalDate = LocalDate.now()): LocalDate {
        return today.with(TemporalAdjusters.firstDayOfNextMonth())
    }
}
