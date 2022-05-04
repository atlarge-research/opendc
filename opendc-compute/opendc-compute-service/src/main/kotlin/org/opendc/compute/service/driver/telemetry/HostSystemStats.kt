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

package org.opendc.compute.service.driver.telemetry

import java.time.Duration
import java.time.Instant

/**
 * System-level statistics of a host.

 * @property uptime The cumulative uptime of the host since last boot (in ms).
 * @property downtime The cumulative downtime of the host since last boot (in ms).
 * @property bootTime The time at which the server started.
 * @property powerUsage Instantaneous power usage of the system (in W).
 * @property energyUsage The cumulative energy usage of the system (in J).
 * @property guestsTerminated The number of guests that are in a terminated state.
 * @property guestsRunning The number of guests that are in a running state.
 * @property guestsError The number of guests that are in an error state.
 * @property guestsInvalid The number of guests that are in an unknown state.
 */
public data class HostSystemStats(
    val uptime: Duration,
    val downtime: Duration,
    val bootTime: Instant?,
    val powerUsage: Double,
    val energyUsage: Double,
    val guestsTerminated: Int,
    val guestsRunning: Int,
    val guestsError: Int,
    val guestsInvalid: Int,
)
