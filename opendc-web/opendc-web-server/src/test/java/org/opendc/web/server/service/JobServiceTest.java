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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import io.quarkus.test.junit.QuarkusTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendc.web.proto.JobState;
import org.opendc.web.server.model.Job;

/**
 * Test suite for the {@link JobService}.
 */
//@QuarkusTest
public class JobServiceTest {
    /**
     * The {@link JobService} instance under test.
     */
    private JobService service;

    /**
     * The mock {@link UserAccountingService}.
     */
    private UserAccountingService mockAccountingService;

    @BeforeEach
    public void setUp() {
        mockAccountingService = Mockito.mock(UserAccountingService.class);
        service = new JobService(mockAccountingService);
    }

//    @Test
    public void testUpdateInvalidTransition() {
        Job job = new Job(null, "test", Instant.now(), 1);
        job.state = JobState.RUNNING;

        assertThrows(IllegalArgumentException.class, () -> service.updateJob(job, JobState.CLAIMED, 0, null));

        Mockito.verifyNoInteractions(mockAccountingService);
    }

//    @Test
    public void testUpdateNoBudget() {
        Job job = Mockito.spy(new Job(null, "test", Instant.now(), 1));
        job.state = JobState.RUNNING;

        Mockito.when(mockAccountingService.consumeSimulationBudget(any(), anyInt()))
                .thenReturn(true);
        Mockito.doReturn(true).when(job).updateAtomically(any(), any(), anyInt(), any());

        service.updateJob(job, JobState.RUNNING, 0, null);

        Mockito.verify(job).updateAtomically(eq(JobState.FAILED), any(), anyInt(), any());
    }

//    @Test
    public void testUpdateNoBudgetWhenFinishing() {
        Job job = Mockito.spy(new Job(null, "test", Instant.now(), 1));
        job.state = JobState.RUNNING;

        Mockito.when(mockAccountingService.consumeSimulationBudget(any(), anyInt()))
                .thenReturn(true);
        Mockito.doReturn(true).when(job).updateAtomically(any(), any(), anyInt(), any());

        service.updateJob(job, JobState.FINISHED, 0, null);

        Mockito.verify(job).updateAtomically(eq(JobState.FINISHED), any(), anyInt(), any());
    }

//    @Test
    public void testUpdateSuccess() {
        Job job = Mockito.spy(new Job(null, "test", Instant.now(), 1));
        job.state = JobState.RUNNING;

        Mockito.when(mockAccountingService.consumeSimulationBudget(any(), anyInt()))
                .thenReturn(false);
        Mockito.doReturn(true).when(job).updateAtomically(any(), any(), anyInt(), any());

        service.updateJob(job, JobState.FINISHED, 0, null);

        Mockito.verify(job).updateAtomically(eq(JobState.FINISHED), any(), anyInt(), any());
    }

//    @Test
    public void testUpdateConflict() {
        Job job = Mockito.spy(new Job(null, "test", Instant.now(), 1));
        job.state = JobState.RUNNING;

        Mockito.when(mockAccountingService.consumeSimulationBudget(any(), anyInt()))
                .thenReturn(false);
        Mockito.doReturn(false).when(job).updateAtomically(any(), any(), anyInt(), any());

        assertThrows(IllegalStateException.class, () -> service.updateJob(job, JobState.FINISHED, 0, null));

        Mockito.verify(job).updateAtomically(eq(JobState.FINISHED), any(), anyInt(), any());
    }
}
