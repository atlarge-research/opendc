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

package org.opendc.compute.service.telemetry;

/**
 * Statistics about the scheduling component of the [ComputeService].
 *
 * @param hostsAvailable The number of hosts currently available for scheduling.
 * @param hostsUnavailable The number of hosts unavailable for scheduling.
 * @param attemptsSuccess Scheduling attempts that resulted into an allocation onto a host.
 * @param attemptsFailure The number of failed scheduling attempt due to insufficient capacity at the moment.
 * @param attemptsError The number of scheduling attempts that failed due to system error.
 * @param serversTotal The number of servers registered with the service.
 * @param serversPending The number of servers that are pending to be scheduled.
 * @param serversActive The number of servers that are currently managed by the service and running.
 */
public record SchedulerStats(
        int hostsAvailable,
        int hostsUnavailable,
        long attemptsSuccess,
        long attemptsFailure,
        long attemptsError,
        int serversTotal,
        int serversPending,
        int serversActive) {}
