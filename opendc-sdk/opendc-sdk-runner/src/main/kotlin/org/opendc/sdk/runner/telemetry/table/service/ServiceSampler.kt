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

package org.opendc.sdk.runner.telemetry.table.service

import org.opendc.compute.simulator.service.ComputeService
import java.time.Duration
import java.time.Instant

public class ServiceSampler(
    private val service: ComputeService,
    private val startTime: Duration = Duration.ofMillis(0),
) {
    public fun sample(now: Instant): ServiceSample {
        val timestamp = now
        val timestampAbsolute = now + startTime

        return ServiceSample(
            timestamp = timestamp,
            timestampAbsolute = timestampAbsolute,
            hostsUp = service.hostsAvailable,
            hostsDown = service.hostsUnavailable,
            tasksTotal = service.tasksTotal,
            tasksPending = service.tasksPending,
            tasksCompleted = service.tasksCompleted,
            tasksActive = service.tasksActive,
            tasksTerminated = service.tasksTerminated,
            attemptsSuccess = service.attemptsSuccess.toInt(),
            attemptsFailure = service.attemptsFailure.toInt(),
        )
    }
}
