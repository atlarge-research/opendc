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

package org.opendc.web.server.repository

import org.opendc.web.server.model.UserAccounting
import java.time.LocalDate
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.persistence.EntityManager

/**
 * A repository to manage [UserAccounting] entities.
 */
@ApplicationScoped
class UserAccountingRepository @Inject constructor(private val em: EntityManager) {
    /**
     * Find the [UserAccounting] object for the specified [userId].
     *
     * @param userId The unique identifier of the user.
     * @return The [UserAccounting] object or `null` if it does not exist.
     */
    fun findForUser(userId: String): UserAccounting? {
        return em.find(UserAccounting::class.java, userId)
    }

    /**
     * Save the specified [UserAccounting] object to the database.
     */
    fun save(accounting: UserAccounting) {
        em.persist(accounting)
    }

    /**
     * Atomically consume the budget for the specified [UserAccounting] object.
     *
     * @param accounting The [UserAccounting] object to update atomically.
     * @param seconds The number of seconds to consume from the user.
     * @return `true` when the update succeeded`, `false` when there was a conflict.
     */
    fun consumeBudget(accounting: UserAccounting, seconds: Int): Boolean {
        val count = em.createNamedQuery("UserAccounting.consumeBudget")
            .setParameter("userId", accounting.userId)
            .setParameter("periodEnd", accounting.periodEnd)
            .setParameter("seconds", seconds)
            .executeUpdate()
        em.refresh(accounting)
        return count > 0
    }

    /**
     * Atomically reset the budget for the specified [UserAccounting] object.
     *
     * @param accounting The [UserAccounting] object to update atomically.
     * @param periodEnd The new end period for the budget.
     * @param seconds The number of seconds that have already been consumed.
     * @return `true` when the update succeeded`, `false` when there was a conflict.
     */
    fun resetBudget(accounting: UserAccounting, periodEnd: LocalDate, seconds: Int): Boolean {
        val count = em.createNamedQuery("UserAccounting.resetBudget")
            .setParameter("userId", accounting.userId)
            .setParameter("oldPeriodEnd", accounting.periodEnd)
            .setParameter("periodEnd", periodEnd)
            .setParameter("seconds", seconds)
            .executeUpdate()
        em.refresh(accounting)
        return count > 0
    }
}
