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

package org.opendc.web.server.model

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** The windows simulation is metered over: one sitting, and a week. */
enum class BudgetPeriod(val length: Duration) {
    SESSION(Duration.ofHours(5)),
    WEEK(Duration.ofDays(7)),
}

enum class CapKind {
    LIMITED,
    UNLIMITED,
}

/**
 * How much simulation a window allows. Unlimited is a grant made on purpose, so it is its own
 * variant rather than a missing number.
 */
@Serializable
sealed interface SimulationCap {
    @Serializable
    @SerialName("limited")
    data class Limited(val seconds: Double) : SimulationCap

    @Serializable
    @SerialName("unlimited")
    data object Unlimited : SimulationCap
}

/**
 * One window of an account's simulation budget. What is reserved against it is not stored: it is
 * the quotes of the account's unfinished work, read when it is needed, so it cannot drift.
 */
@Entity
@Table(name = "budget_windows")
class BudgetWindow : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var user: UserAccount

    @Enumerated(EnumType.STRING)
    lateinit var period: BudgetPeriod

    var usedSeconds: Double = 0.0

    @Enumerated(EnumType.STRING)
    lateinit var capKind: CapKind

    /** The limit when [capKind] is [CapKind.LIMITED], which a check constraint ties it to. */
    var capSeconds: Double? = null

    lateinit var resetsAt: Instant

    var cap: SimulationCap
        get() =
            when (capKind) {
                CapKind.LIMITED -> SimulationCap.Limited(checkNotNull(capSeconds) { "a limited window has a limit" })
                CapKind.UNLIMITED -> SimulationCap.Unlimited
            }
        set(value) {
            when (value) {
                is SimulationCap.Limited -> {
                    capKind = CapKind.LIMITED
                    capSeconds = value.seconds
                }
                SimulationCap.Unlimited -> {
                    capKind = CapKind.UNLIMITED
                    capSeconds = null
                }
            }
        }

    /** What has been used in the window as it stands at [now]: nothing, once it has run out. */
    fun usedAt(now: Instant): Double = if (now.isBefore(resetsAt)) usedSeconds else 0.0

    companion object : PanacheCompanion<BudgetWindow> {
        fun findByUser(userId: Long): List<BudgetWindow> = list("user.id = ?1", userId).sortedBy { it.period }

        /** An account's windows, locked, so one account's submissions are admitted one at a time. */
        fun lockByUser(userId: Long): List<BudgetWindow> =
            find("user.id = ?1", userId).withLock(LockModeType.PESSIMISTIC_WRITE).list().sortedBy { it.period }
    }
}

@Entity
@Table(name = "invoices")
class Invoice : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var publicId: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var user: UserAccount

    lateinit var issuedAt: Instant

    lateinit var amountEur: BigDecimal

    var paid: Boolean = false

    companion object : PanacheCompanion<Invoice> {
        fun findByUser(userId: Long): List<Invoice> = list("user.id = ?1", userId).sortedByDescending { it.issuedAt }
    }
}
