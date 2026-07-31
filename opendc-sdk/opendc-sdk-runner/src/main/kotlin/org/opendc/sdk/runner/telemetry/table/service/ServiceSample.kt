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

import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Instant

public data class ServiceSample(
    // TODO: Why is this not a Long?
    public val timestamp: Instant = Instant.MIN,
    public val timestampAbsolute: Instant = Instant.MIN,
    public val hostsUp: Int = -1,
    public val hostsDown: Int = -1,
    public val tasksTotal: Int = -1,
    public val tasksPending: Int = -1,
    public val tasksCompleted: Int = -1,
    public val tasksActive: Int = -1,
    public val tasksTerminated: Int = -1,
    public val attemptsSuccess: Int = -1,
    public val attemptsFailure: Int = -1,
) : Exportable
