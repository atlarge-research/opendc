/*
 * Copyright (c) 2021 AtLarge Research
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

@file:JvmName("RiskEvaluationPeriods")
package org.opendc.telemetry.risk

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * An evaluation period of one or multiple minutes.
 */
public fun minutePeriod(minutes: Int = 1): RiskEvaluationPeriod = object : RiskEvaluationPeriod {
    override fun getNextPeriod(date: ZonedDateTime): ZonedDateTime {
        return date.truncatedTo(ChronoUnit.MINUTES).plusMinutes(minutes.toLong())
    }

    override fun toString(): String = "RiskEvaluationPeriod[every $minutes minutes]"
}

/**
 * An evaluation period of one or multiple hours.
 */
public fun hourPeriod(hours: Int = 1): RiskEvaluationPeriod = object : RiskEvaluationPeriod {
    override fun getNextPeriod(date: ZonedDateTime): ZonedDateTime {
        return date.truncatedTo(ChronoUnit.HOURS).plusHours(hours.toLong())
    }

    override fun toString(): String = "RiskEvaluationPeriod[every $hours hours]"
}

/**
 * An evaluation period of one or multiple days.
 */
public fun dayPeriod(days: Int = 1): RiskEvaluationPeriod = object : RiskEvaluationPeriod {
    override fun getNextPeriod(date: ZonedDateTime): ZonedDateTime {
        return date.truncatedTo(ChronoUnit.DAYS).plusDays(days.toLong())
    }

    override fun toString(): String = "RiskEvaluationPeriod[every $days days]"
}

/**
 * An evaluation period of a week.
 */
public fun weekPeriod(): RiskEvaluationPeriod = object : RiskEvaluationPeriod {
    override fun getNextPeriod(date: ZonedDateTime): ZonedDateTime {
        return date.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    }

    override fun toString(): String = "RiskEvaluationPeriod[every week]"
}

/**
 * An evaluation period of a month.
 */
public fun monthPeriod(): RiskEvaluationPeriod = object : RiskEvaluationPeriod {
    override fun getNextPeriod(date: ZonedDateTime): ZonedDateTime {
        return date.with(TemporalAdjusters.firstDayOfNextMonth())
    }

    override fun toString(): String = "RiskEvaluationPeriod[every month]"
}
