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

package org.opendc.compute.service.driver.telemetry;

import java.time.Duration;
import java.time.Instant;

/**
 * System-level statistics of a host.
 *
 * @param uptime The cumulative uptime of the host since last boot (in ms).
 * @param downtime The cumulative downtime of the host since last boot (in ms).
 * @param bootTime The time at which the server started.
 * @param powerUsage Instantaneous power usage of the system (in W).
 * @param energyUsage The cumulative energy usage of the system (in J).
 * @param guestsTerminated The number of guests that are in a terminated state.
 * @param guestsRunning The number of guests that are in a running state.
 * @param guestsError The number of guests that are in an error state.
 * @param guestsInvalid The number of guests that are in an unknown state.
 */
public record HostSystemStats(
        Duration uptime,
        Duration downtime,
        Instant bootTime,
        double powerUsage,
        double energyUsage,
        int guestsTerminated,
        int guestsRunning,
        int guestsError,
        int guestsInvalid) {}
