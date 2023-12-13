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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import io.quarkus.panache.mock.PanacheMock;
import java.time.Duration;
import java.time.LocalDate;
import javax.persistence.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.opendc.web.server.model.UserAccounting;

/**
 * Test suite for the {@link UserAccountingService}.
 */
// @QuarkusTest
public class UserAccountingServiceTest {
    /**
     * The {@link UserAccountingService} instance under test.
     */
    private UserAccountingService service;

    /**
     * The user id to test with
     */
    private final String userId = "test";

    @BeforeEach
    public void setUp() {
        PanacheMock.mock(UserAccounting.class);
        service = new UserAccountingService(Duration.ofHours(1));
    }

    //    @Test
    public void testGetUserDoesNotExist() {
        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(null);

        var accounting = service.getAccounting(userId);

        assertTrue(accounting.getPeriodEnd().isAfter(LocalDate.now()));
        assertEquals(0, accounting.getSimulationTime());
    }

    //    @Test
    public void testGetUserDoesExist() {
        var now = LocalDate.now();
        var periodEnd = now.plusMonths(1);

        var mockAccounting = new UserAccounting(userId, periodEnd, 3600);
        mockAccounting.simulationTime = 32;

        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(mockAccounting);

        var accounting = service.getAccounting(userId);

        assertAll(
                () -> assertEquals(periodEnd, accounting.getPeriodEnd()),
                () -> assertEquals(32, accounting.getSimulationTime()),
                () -> assertEquals(3600, accounting.getSimulationTimeBudget()));
    }

    //    @Test
    public void testHasBudgetUserDoesNotExist() {
        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(null);

        assertTrue(service.hasSimulationBudget(userId));
    }

    //    @Test
    public void testHasBudget() {
        var periodEnd = LocalDate.now().plusMonths(2);

        var mockAccounting = new UserAccounting(userId, periodEnd, 3600);
        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(mockAccounting);

        assertTrue(service.hasSimulationBudget(userId));
    }

    //    @Test
    public void testHasBudgetExceededButPeriodExpired() {
        var periodEnd = LocalDate.now().minusMonths(2);

        var mockAccounting = new UserAccounting(userId, periodEnd, 3600);
        mockAccounting.simulationTime = 3900;
        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(mockAccounting);

        assertTrue(service.hasSimulationBudget(userId));
    }

    //    @Test
    public void testHasBudgetPeriodExpired() {
        var periodEnd = LocalDate.now().minusMonths(2);

        var mockAccounting = new UserAccounting(userId, periodEnd, 3600);
        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(mockAccounting);

        assertTrue(service.hasSimulationBudget(userId));
    }

    //    @Test
    public void testHasBudgetExceeded() {
        var periodEnd = LocalDate.now().plusMonths(1);

        var mockAccounting = new UserAccounting(userId, periodEnd, 3600);
        mockAccounting.simulationTime = 3900;
        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(mockAccounting);

        assertFalse(service.hasSimulationBudget(userId));
    }

    //    @Test
    public void testConsumeBudgetNewUser() {
        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(null);
        Mockito.when(UserAccounting.create(anyString(), any(), anyInt(), anyInt()))
                .thenAnswer((i) -> {
                    var accounting = new UserAccounting(i.getArgument(0), i.getArgument(1), i.getArgument(2));
                    accounting.simulationTime = i.getArgument(3);
                    return accounting;
                });

        assertFalse(service.consumeSimulationBudget(userId, 10));
    }

    //    @Test
    public void testConsumeBudgetNewUserExceeded() {
        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(null);
        Mockito.when(UserAccounting.create(anyString(), any(), anyInt(), anyInt()))
                .thenAnswer((i) -> {
                    var accounting = new UserAccounting(i.getArgument(0), i.getArgument(1), i.getArgument(2));
                    accounting.simulationTime = i.getArgument(3);
                    return accounting;
                });

        assertTrue(service.consumeSimulationBudget(userId, 4000));
    }

    //    @Test
    public void testConsumeBudgetNewUserConflict() {
        var periodEnd = LocalDate.now().plusMonths(1);
        var accountingMock = Mockito.spy(new UserAccounting(userId, periodEnd, 3600));

        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(null).thenReturn(accountingMock);
        Mockito.when(UserAccounting.create(anyString(), any(), anyInt(), anyInt()))
                .thenThrow(new EntityExistsException());
        Mockito.when(accountingMock.consumeBudget(anyInt())).thenAnswer((i) -> {
            accountingMock.simulationTime += i.<Integer>getArgument(0);
            return true;
        });

        assertFalse(service.consumeSimulationBudget(userId, 10));
    }

    //    @Test
    public void testConsumeBudgetResetSuccess() {
        var periodEnd = LocalDate.now().minusMonths(2);
        var accountingMock = Mockito.spy(new UserAccounting(userId, periodEnd, 3600));
        accountingMock.simulationTime = 3900;

        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(accountingMock);
        Mockito.when(accountingMock.resetBudget(any(), anyInt())).thenAnswer((i) -> {
            accountingMock.periodEnd = i.getArgument(0);
            accountingMock.simulationTime += i.<Integer>getArgument(1);
            return true;
        });

        assertTrue(service.consumeSimulationBudget(userId, 4000));
    }

    //    @Test
    public void testInfiniteConflict() {
        var periodEnd = LocalDate.now().plusMonths(1);
        var accountingMock = Mockito.spy(new UserAccounting(userId, periodEnd, 3600));

        Mockito.when(UserAccounting.findByUser(userId)).thenReturn(accountingMock);
        Mockito.when(accountingMock.consumeBudget(anyInt())).thenAnswer((i) -> {
            accountingMock.simulationTime += i.<Integer>getArgument(0);
            return false;
        });

        assertThrows(IllegalStateException.class, () -> service.consumeSimulationBudget(userId, 10));
    }
}
