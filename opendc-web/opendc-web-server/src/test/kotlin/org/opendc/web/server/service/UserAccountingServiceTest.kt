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

import io.mockk.every
import io.mockk.mockk
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.web.server.model.UserAccounting
import org.opendc.web.server.repository.UserAccountingRepository
import java.time.Duration
import java.time.LocalDate
import javax.persistence.EntityExistsException

/**
 * Test suite for the [UserAccountingService].
 */
@QuarkusTest
class UserAccountingServiceTest {
    /**
     * The [UserAccountingRepository] that is mocked.
     */
    private val repository: UserAccountingRepository = mockk()

    /**
     * The [UserAccountingService] instance under test.
     */
    private val service: UserAccountingService = UserAccountingService(repository, Duration.ofHours(1))

    @Test
    fun testGetUserDoesNotExist() {
        val userId = "test"

        every { repository.findForUser(userId) } returns null

        val accounting = service.getAccounting(userId)

        assertTrue(accounting.periodEnd.isAfter(LocalDate.now()))
        assertEquals(0, accounting.simulationTime)
    }

    @Test
    fun testGetUserDoesExist() {
        val userId = "test"

        val now = LocalDate.now()
        val periodEnd = now.plusMonths(1)

        every { repository.findForUser(userId) } returns UserAccounting(userId, periodEnd, 3600).also { it.simulationTime = 32 }

        val accounting = service.getAccounting(userId)

        assertAll(
            { assertEquals(periodEnd, accounting.periodEnd) },
            { assertEquals(32, accounting.simulationTime) },
            { assertEquals(3600, accounting.simulationTimeBudget) }
        )
    }

    @Test
    fun testHasBudgetUserDoesNotExist() {
        val userId = "test"

        every { repository.findForUser(userId) } returns null

        assertTrue(service.hasSimulationBudget(userId))
    }

    @Test
    fun testHasBudget() {
        val userId = "test"
        val periodEnd = LocalDate.now().plusMonths(2)

        every { repository.findForUser(userId) } returns UserAccounting(userId, periodEnd, 3600)

        assertTrue(service.hasSimulationBudget(userId))
    }

    @Test
    fun testHasBudgetExceededButPeriodExpired() {
        val userId = "test"
        val periodEnd = LocalDate.now().minusMonths(2)

        every { repository.findForUser(userId) } returns UserAccounting(userId, periodEnd, 3600).also { it.simulationTime = 3900 }

        assertTrue(service.hasSimulationBudget(userId))
    }

    @Test
    fun testHasBudgetPeriodExpired() {
        val userId = "test"
        val periodEnd = LocalDate.now().minusMonths(2)

        every { repository.findForUser(userId) } returns UserAccounting(userId, periodEnd, 3600)

        assertTrue(service.hasSimulationBudget(userId))
    }

    @Test
    fun testHasBudgetExceeded() {
        val userId = "test"
        val periodEnd = LocalDate.now().plusMonths(1)

        every { repository.findForUser(userId) } returns UserAccounting(userId, periodEnd, 3600).also { it.simulationTime = 3900 }

        assertFalse(service.hasSimulationBudget(userId))
    }

    @Test
    fun testConsumeBudgetNewUser() {
        val userId = "test"

        every { repository.findForUser(userId) } returns null
        every { repository.save(any()) } returns Unit

        assertFalse(service.consumeSimulationBudget(userId, 10))
    }

    @Test
    fun testConsumeBudgetNewUserExceeded() {
        val userId = "test"

        every { repository.findForUser(userId) } returns null
        every { repository.save(any()) } returns Unit

        assertTrue(service.consumeSimulationBudget(userId, 4000))
    }

    @Test
    fun testConsumeBudgetNewUserConflict() {
        val userId = "test"

        val periodEnd = LocalDate.now().plusMonths(1)

        every { repository.findForUser(userId) } returns null andThen UserAccounting(userId, periodEnd, 3600)
        every { repository.save(any()) } throws EntityExistsException()
        every { repository.consumeBudget(any(), any()) } answers {
            val accounting = it.invocation.args[0] as UserAccounting
            accounting.simulationTime -= it.invocation.args[1] as Int
            true
        }

        assertFalse(service.consumeSimulationBudget(userId, 10))
    }

    @Test
    fun testConsumeBudgetResetSuccess() {
        val userId = "test"

        val periodEnd = LocalDate.now().minusMonths(2)

        every { repository.findForUser(userId) } returns UserAccounting(userId, periodEnd, 3600).also { it.simulationTime = 3900 }
        every { repository.resetBudget(any(), any(), any()) } answers {
            val accounting = it.invocation.args[0] as UserAccounting
            accounting.periodEnd = it.invocation.args[1] as LocalDate
            accounting.simulationTime = it.invocation.args[2] as Int
            true
        }

        assertTrue(service.consumeSimulationBudget(userId, 4000))
    }

    @Test
    fun testInfiniteConflict() {
        val userId = "test"

        val periodEnd = LocalDate.now().plusMonths(1)

        every { repository.findForUser(userId) } returns UserAccounting(userId, periodEnd, 3600)
        every { repository.consumeBudget(any(), any()) } answers {
            val accounting = it.invocation.args[0] as UserAccounting
            accounting.simulationTime -= it.invocation.args[1] as Int
            false
        }

        assertThrows<IllegalStateException> { service.consumeSimulationBudget(userId, 10) }
    }
}
