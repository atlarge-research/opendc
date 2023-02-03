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

package org.opendc.web.server.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityExistsException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opendc.web.server.model.UserAccounting;

/**
 * Service to track the simulation budget of users.
 */
@ApplicationScoped
public final class UserAccountingService {
    /**
     * The default simulation budget for new users.
     */
    private final Duration simulationBudget;

    /**
     * Construct a {@link UserAccountingService} instance.
     *
     * @param simulationBudget The default simulation budget for new users.
     */
    public UserAccountingService(
            @ConfigProperty(name = "opendc.accounting.simulation-budget", defaultValue = "2000m")
                    Duration simulationBudget) {
        this.simulationBudget = simulationBudget;
    }

    /**
     * Return the {@link org.opendc.web.proto.user.UserAccounting} object for the user with the
     * specified <code>userId</code>. If the object does not exist in the database, a default value is constructed.
     */
    public org.opendc.web.proto.user.UserAccounting getAccounting(String userId) {
        UserAccounting accounting = UserAccounting.findByUser(userId);
        if (accounting != null) {
            return new org.opendc.web.proto.user.UserAccounting(
                    accounting.periodEnd, accounting.simulationTime, accounting.simulationTimeBudget);
        }

        return new org.opendc.web.proto.user.UserAccounting(
                getNextAccountingPeriod(LocalDate.now()), 0, (int) simulationBudget.toSeconds());
    }

    /**
     * Determine whether the user with <code>userId</code> has any remaining simulation budget.
     *
     * @param userId The unique identifier of the user.
     * @return <code>true</code> when the user still has budget left, <code>false</code> otherwise.
     */
    public boolean hasSimulationBudget(String userId) {
        UserAccounting accounting = UserAccounting.findByUser(userId);
        if (accounting == null) {
            return true;
        }
        return accounting.hasSimulationBudget();
    }

    /**
     * Consume <code>seconds</code> from the simulation budget of the user with <code>userId</code>.
     *
     * @param userId The unique identifier of the user.
     * @param seconds The seconds to consume from the simulation budget.
     * @return <code>true</code> if the user has consumed his full budget or <code>false</code> if there is still budget
     *         remaining.
     */
    public boolean consumeSimulationBudget(String userId, int seconds) {
        LocalDate today = LocalDate.now();
        LocalDate nextAccountingPeriod = getNextAccountingPeriod(today);

        // We need to be careful to prevent conflicts in case of concurrency
        // 1. First, we try to create the accounting object if it does not exist yet. This may fail if another instance
        //    creates the object concurrently.
        // 2. Second, we check if the budget needs to be reset and try this atomically.
        // 3. Finally, we atomically consume the budget from the object
        // This is repeated three times in case there is a conflict
        for (int i = 0; i < 3; i++) {
            UserAccounting accounting = UserAccounting.findByUser(userId);

            if (accounting == null) {
                try {
                    UserAccounting newAccounting = UserAccounting.create(
                            userId, nextAccountingPeriod, (int) simulationBudget.toSeconds(), seconds);
                    return !newAccounting.hasSimulationBudget();
                } catch (EntityExistsException e) {
                    // Conflict due to concurrency; retry
                }
            } else {
                boolean success;

                if (!today.isBefore(accounting.periodEnd)) {
                    success = accounting.resetBudget(nextAccountingPeriod, seconds);
                } else {
                    success = accounting.consumeBudget(seconds);
                }

                if (success) {
                    return !accounting.hasSimulationBudget();
                }
            }
        }

        throw new IllegalStateException("Failed to allocate consume budget due to conflict");
    }

    /**
     * Helper method to find next accounting period.
     */
    private static LocalDate getNextAccountingPeriod(LocalDate today) {
        return today.with(TemporalAdjusters.firstDayOfNextMonth());
    }
}
