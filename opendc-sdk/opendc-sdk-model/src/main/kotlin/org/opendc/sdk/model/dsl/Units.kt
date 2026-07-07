/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.dsl

import org.opendc.common.units.DataRate
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.Power
import org.opendc.common.units.TimeDelta

/** This number as a [Frequency] in gigahertz. */
public val Number.ghz: Frequency get() = Frequency.ofGHz(this)

/** This number as a [Frequency] in megahertz. */
public val Number.mhz: Frequency get() = Frequency.ofMHz(this)

/** This number as a [DataSize] in gibibytes. */
public val Number.gib: DataSize get() = DataSize.ofGiB(this)

/** This number as a [DataSize] in mebibytes. */
public val Number.mib: DataSize get() = DataSize.ofMiB(this)

/** This number as a [DataSize] in bytes. */
public val Number.bytes: DataSize get() = DataSize.ofBytes(this)

/** This number as a [Power] in watts. */
public val Number.watts: Power get() = Power.ofWatts(this)

/** This number as a [Power] in kilowatts. */
public val Number.kwatts: Power get() = Power.ofKWatts(this)

/** This number as a [DataRate] in gigabytes per second. */
public val Number.gbps: DataRate get() = DataRate.ofGBps(this)

/** This number as a [DataRate] in kibibits per second. */
public val Number.kibps: DataRate get() = DataRate.ofKibps(this)

/** This number as a [TimeDelta] in milliseconds. */
public val Number.ms: TimeDelta get() = TimeDelta.ofMillis(this)

/** This number as a [TimeDelta] in seconds. */
public val Number.seconds: TimeDelta get() = TimeDelta.ofSec(this)

/** This number as a [TimeDelta] in minutes. */
public val Number.minutes: TimeDelta get() = TimeDelta.ofMin(this)

/** This number as a [TimeDelta] in hours. */
public val Number.hours: TimeDelta get() = TimeDelta.ofHours(this)
