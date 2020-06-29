/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.odcsim.engine.omega.logging

import com.atlarge.odcsim.SimulationContext
import org.slf4j.Logger
import org.slf4j.Marker

/**
 * A [Logger] implementation that is not aware of the calling location.
 *
 * @param ctx The owning [SimulationContext] of this logger.
 * @param delegate The [Logger] to delegate the messages to.
 */
internal class LocationIgnorantLoggerImpl(
    ctx: SimulationContext,
    private val delegate: Logger
) : LoggerImpl(ctx), Logger by delegate {
    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(marker, format, arg1, arg2) }
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(format, arg1, arg2) }
    }

    override fun warn(msg: String?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(msg) }
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(marker, format, arg) }
    }

    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(marker, format, arguments) }
    }

    override fun warn(format: String?, arg: Any?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(format, arg) }
    }

    override fun warn(marker: Marker?, msg: String?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(marker, msg) }
    }

    override fun warn(msg: String?, t: Throwable?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(msg, t) }
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(format, *arguments) }
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        if (!isWarnEnabled) {
            return
        }

        withMdc { delegate.warn(marker, msg, t) }
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(marker, format, *arguments) }
    }

    override fun info(format: String?, arg: Any?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(format, arg) }
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(marker, msg, t) }
    }

    override fun info(msg: String?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(msg) }
    }

    override fun info(format: String?, vararg arguments: Any?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(format, *arguments) }
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(format, arg1, arg2) }
    }

    override fun info(marker: Marker?, msg: String?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(marker, msg) }
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(marker, format, arg) }
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(marker, format, arg1, arg2) }
    }

    override fun info(msg: String?, t: Throwable?) {
        if (!isInfoEnabled) {
            return
        }

        withMdc { delegate.info(msg, t) }
    }

    override fun error(msg: String?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(msg) }
    }

    override fun error(marker: Marker?, msg: String?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(marker, msg) }
    }

    override fun error(format: String?, vararg arguments: Any?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(format, *arguments) }
    }

    override fun error(format: String?, arg: Any?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(format, arg) }
    }

    override fun error(msg: String?, t: Throwable?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(msg, t) }
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(marker, format, arg1, arg2) }
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(marker, format, *arguments) }
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(marker, msg, t) }
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(format, arg1, arg2) }
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        if (!isErrorEnabled) {
            return
        }

        withMdc { delegate.error(marker, format, arg) }
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(format, *arguments) }
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(format, arg1, arg2) }
    }

    override fun debug(msg: String?, t: Throwable?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(msg, t) }
    }

    override fun debug(format: String?, arg: Any?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(format, arg) }
    }

    override fun debug(marker: Marker?, msg: String?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(marker, msg) }
    }

    override fun debug(msg: String?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(msg) }
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(marker, msg, t) }
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(marker, format, arg1, arg2) }
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(marker, format, arg) }
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (!isDebugEnabled) {
            return
        }

        withMdc { delegate.debug(marker, format, *arguments) }
    }

    override fun trace(format: String?, arg: Any?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(format, arg) }
    }

    override fun trace(marker: Marker?, msg: String?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(marker, msg) }
    }

    override fun trace(msg: String?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(msg) }
    }

    override fun trace(msg: String?, t: Throwable?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(msg, t) }
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(format, arg1, arg2) }
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(marker, format, arg1, arg2) }
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(marker, format, arg) }
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(marker, format, *argArray) }
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(marker, msg, t) }
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        if (!isTraceEnabled) {
            return
        }

        withMdc { delegate.trace(format, *arguments) }
    }
}
