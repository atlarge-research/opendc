/*
 * Copyright (c) 2023 AtLarge Research
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

package org.opendc.web.server.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Entity to track the number of simulation minutes used by a user.
 */
@Entity
@Table
@NamedQueries({
    @NamedQuery(
            name = "UserAccounting.consumeBudget",
            query =
                    """
                UPDATE UserAccounting a
                SET a.simulationTime = a.simulationTime + :seconds
                WHERE a.userId = :userId AND a.periodEnd = :periodEnd
            """),
    @NamedQuery(
            name = "UserAccounting.resetBudget",
            query =
                    """
                UPDATE UserAccounting a
                SET a.periodEnd = :periodEnd, a.simulationTime = :seconds
                WHERE a.userId = :userId AND a.periodEnd = :oldPeriodEnd
            """)
})
public class UserAccounting extends PanacheEntityBase {
    /**
     * User to which this object belongs.
     */
    @Id
    @Column(name = "user_id", nullable = false)
    public String userId;

    /**
     * The end of the accounting period.
     */
    @Column(name = "period_end", nullable = false)
    public LocalDate periodEnd;

    /**
     * The number of simulation seconds to be used per accounting period.
     */
    @Column(name = "simulation_time_budget", nullable = false)
    public int simulationTimeBudget;

    /**
     * The number of simulation seconds used in this period. This number should reset once the accounting period has
     * been reached.
     */
    @Column(name = "simulation_time", nullable = false)
    public int simulationTime = 0;

    /**
     * Construct a new {@link UserAccounting} object.
     *
     * @param userId The identifier of the user that this object belongs to.
     * @param periodEnd The end of the accounting period.
     * @param simulationTimeBudget The number of simulation seconds available per accounting period.
     */
    public UserAccounting(String userId, LocalDate periodEnd, int simulationTimeBudget) {
        this.userId = userId;
        this.periodEnd = periodEnd;
        this.simulationTimeBudget = simulationTimeBudget;
    }

    /**
     * JPA constructor.
     */
    protected UserAccounting() {}

    /**
     * Return the {@link UserAccounting} object associated with the specified user id.
     */
    public static UserAccounting findByUser(String userId) {
        return findById(userId);
    }

    /**
     * Create a new {@link UserAccounting} object and persist it to the database.
     *
     * @param userId The identifier of the user that this object belongs to.
     * @param periodEnd The end of the accounting period.
     * @param simulationTimeBudget The number of simulation seconds available per accounting period.
     * @param simulationTime The initial simulation time that has been consumed.
     */
    public static UserAccounting create(
            String userId, LocalDate periodEnd, int simulationTimeBudget, int simulationTime) {
        UserAccounting newAccounting = new UserAccounting(userId, periodEnd, simulationTimeBudget);
        newAccounting.simulationTime = simulationTime;
        newAccounting.persistAndFlush();
        return newAccounting;
    }

    /**
     * Atomically consume the budget for this {@link UserAccounting} object.
     *
     * @param seconds The number of seconds to consume from the user.
     * @return <code>true</code> when the update succeeded, <code>false</code> when there was a conflict.
     */
    public boolean consumeBudget(int seconds) {
        long count = update(
                "#UserAccounting.consumeBudget",
                Parameters.with("userId", userId).and("periodEnd", periodEnd).and("seconds", seconds));
        return count > 0;
    }

    /**
     * Atomically reset the budget for this {@link UserAccounting} object.
     *
     * @param periodEnd The new end period for the budget.
     * @param seconds The number of seconds that have already been consumed.
     * @return <code>true</code> when the update succeeded`, <code>false</code> when there was a conflict.
     */
    public boolean resetBudget(LocalDate periodEnd, int seconds) {
        long count = update(
                "#UserAccounting.resetBudget",
                Parameters.with("userId", userId)
                        .and("oldPeriodEnd", this.periodEnd)
                        .and("periodEnd", periodEnd)
                        .and("seconds", seconds));
        return count > 0;
    }

    /**
     * Determine whether the user has any remaining simulation budget.
     *
     * @return <code>true</code> when the user still has budget left, <code>false</code> otherwise.
     */
    public boolean hasSimulationBudget() {
        var today = LocalDate.now();

        // The accounting period must be over or there must be budget remaining.
        return !today.isBefore(periodEnd) || simulationTimeBudget > simulationTime;
    }
}
