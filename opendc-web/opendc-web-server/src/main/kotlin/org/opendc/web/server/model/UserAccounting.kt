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

package org.opendc.web.server.model

import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Table

/**
 * Entity to track the number of simulation minutes used by a user.
 */
@Entity
@Table(name = "user_accounting")
@NamedQueries(
    value = [
        NamedQuery(
            name = "UserAccounting.consumeBudget",
            query = """
                UPDATE UserAccounting a
                SET a.simulationTime = a.simulationTime + :seconds
                WHERE a.userId = :userId AND a.periodEnd = :periodEnd
            """
        ),
        NamedQuery(
            name = "UserAccounting.resetBudget",
            query = """
                UPDATE UserAccounting a
                SET a.periodEnd = :periodEnd, a.simulationTime = :seconds
                WHERE a.userId = :userId AND a.periodEnd = :oldPeriodEnd
            """
        )
    ]
)
class UserAccounting(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: String,

    /**
     * The end of the accounting period.
     */
    @Column(name = "period_end", nullable = false)
    var periodEnd: LocalDate,

    /**
     * The number of simulation seconds to be used per accounting period.
     */
    @Column(name = "simulation_time_budget", nullable = false)
    var simulationTimeBudget: Int
) {
    /**
     * The number of simulation seconds used in this period. This number should reset once the accounting period has
     * been reached.
     */
    @Column(name = "simulation_time", nullable = false)
    var simulationTime: Int = 0
}
