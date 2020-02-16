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
import org.slf4j.helpers.MessageFormatter
import org.slf4j.spi.LocationAwareLogger

/**
 * An actor-specific [Logger] implementation that is aware of the calling location.
 *
 * @param ctx The owning [SimulationContext] of this logger.
 * @param delegate The [LocationAwareLogger] to delegate the messages to.
 */
internal class LocationAwareLoggerImpl(
    ctx: SimulationContext,
    private val delegate: LocationAwareLogger
) : LoggerImpl(ctx), Logger by delegate {
    /**
     * The fully qualified name of this class.
     */
    private val fqcn = LocationAwareLoggerImpl::class.java.name

    override fun trace(format: String?, arg: Any?) {
        if (!delegate.isTraceEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg).message
            delegate.log(null, fqcn, LocationAwareLogger.TRACE_INT, formattedMessage, arrayOf(arg), null)
        }
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isTraceEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(null, fqcn, LocationAwareLogger.TRACE_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun trace(format: String?, argArray: Array<Any?>) {
        if (!delegate.isTraceEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.arrayFormat(format, argArray).message
            delegate.log(null, fqcn, LocationAwareLogger.TRACE_INT, formattedMessage, argArray, null)
        }
    }

    override fun trace(msg: String?, t: Throwable?) {
        if (!delegate.isTraceEnabled) {
            return
        }

        withMdc {
            delegate.log(null, fqcn, LocationAwareLogger.TRACE_INT, msg, null, t)
        }
    }

    override fun trace(marker: Marker?, msg: String?) {
        if (!delegate.isTraceEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.TRACE_INT, msg, null, null)
        }
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        if (!delegate.isTraceEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg).message
            delegate.log(marker, fqcn, LocationAwareLogger.TRACE_INT, formattedMessage, arrayOf(arg), null)
        }
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isTraceEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(marker, fqcn, LocationAwareLogger.TRACE_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun trace(marker: Marker?, format: String?, argArray: Array<Any?>) {
        if (!delegate.isTraceEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.arrayFormat(format, argArray).message
            delegate.log(marker, fqcn, LocationAwareLogger.TRACE_INT, formattedMessage, argArray, null)
        }
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        if (!delegate.isTraceEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.TRACE_INT, msg, null, t)
        }
    }

    override fun debug(msg: String?) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            delegate.log(null, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, null)
        }
    }

    override fun debug(format: String?, arg: Any?) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg).message
            delegate.log(null, fqcn, LocationAwareLogger.DEBUG_INT, formattedMessage, arrayOf(arg), null)
        }
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(null, fqcn, LocationAwareLogger.DEBUG_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun debug(format: String?, argArray: Array<Any?>) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            val ft = MessageFormatter.arrayFormat(format, argArray)
            delegate.log(null, fqcn, LocationAwareLogger.DEBUG_INT, ft.message, ft.argArray, ft.throwable)
        }
    }

    override fun debug(msg: String?, t: Throwable?) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            delegate.log(null, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, t)
        }
    }

    override fun debug(marker: Marker?, msg: String?) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, null)
        }
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            val ft = MessageFormatter.format(format, arg)
            delegate.log(marker, fqcn, LocationAwareLogger.DEBUG_INT, ft.message, ft.argArray, ft.throwable)
        }
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(marker, fqcn, LocationAwareLogger.DEBUG_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun debug(marker: Marker?, format: String?, argArray: Array<Any?>) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            val ft = MessageFormatter.arrayFormat(format, argArray)
            delegate.log(marker, fqcn, LocationAwareLogger.DEBUG_INT, ft.message, argArray, ft.throwable)
        }
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        if (!delegate.isDebugEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, t)
        }
    }

    override fun info(msg: String?) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            delegate.log(null, fqcn, LocationAwareLogger.INFO_INT, msg, null, null)
        }
    }

    override fun info(format: String?, arg: Any?) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg).message
            delegate.log(null, fqcn, LocationAwareLogger.INFO_INT, formattedMessage, arrayOf(arg), null)
        }
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(null, fqcn, LocationAwareLogger.INFO_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun info(format: String?, argArray: Array<Any?>) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.arrayFormat(format, argArray).message
            delegate.log(null, fqcn, LocationAwareLogger.INFO_INT, formattedMessage, argArray, null)
        }
    }

    override fun info(msg: String?, t: Throwable?) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            delegate.log(null, fqcn, LocationAwareLogger.INFO_INT, msg, null, t)
        }
    }

    override fun info(marker: Marker?, msg: String?) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.INFO_INT, msg, null, null)
        }
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg).message
            delegate.log(marker, fqcn, LocationAwareLogger.INFO_INT, formattedMessage, arrayOf(arg), null)
        }
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(marker, fqcn, LocationAwareLogger.INFO_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun info(marker: Marker?, format: String?, argArray: Array<Any?>) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.arrayFormat(format, argArray).message
            delegate.log(marker, fqcn, LocationAwareLogger.INFO_INT, formattedMessage, argArray, null)
        }
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        if (!delegate.isInfoEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.INFO_INT, msg, null, t)
        }
    }

    override fun warn(msg: String?) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            delegate.log(null, fqcn, LocationAwareLogger.WARN_INT, msg, null, null)
        }
    }

    override fun warn(format: String?, arg: Any?) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg).message
            delegate.log(null, fqcn, LocationAwareLogger.WARN_INT, formattedMessage, arrayOf(arg), null)
        }
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(null, fqcn, LocationAwareLogger.WARN_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun warn(format: String?, argArray: Array<Any?>) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.arrayFormat(format, argArray).message
            delegate.log(null, fqcn, LocationAwareLogger.WARN_INT, formattedMessage, argArray, null)
        }
    }

    override fun warn(msg: String?, t: Throwable?) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            delegate.log(null, fqcn, LocationAwareLogger.WARN_INT, msg, null, t)
        }
    }

    override fun warn(marker: Marker?, msg: String?) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.WARN_INT, msg, null, null)
        }
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg).message
            delegate.log(marker, fqcn, LocationAwareLogger.WARN_INT, formattedMessage, arrayOf(arg), null)
        }
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(marker, fqcn, LocationAwareLogger.WARN_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun warn(marker: Marker?, format: String?, argArray: Array<Any?>) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.arrayFormat(format, argArray).message
            delegate.log(marker, fqcn, LocationAwareLogger.WARN_INT, formattedMessage, argArray, null)
        }
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        if (!delegate.isWarnEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.WARN_INT, msg, null, t)
        }
    }

    override fun error(msg: String?) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            delegate.log(null, fqcn, LocationAwareLogger.ERROR_INT, msg, null, null)
        }
    }

    override fun error(format: String?, arg: Any?) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg).message
            delegate.log(null, fqcn, LocationAwareLogger.ERROR_INT, formattedMessage, arrayOf(arg), null)
        }
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(null, fqcn, LocationAwareLogger.ERROR_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun error(format: String?, argArray: Array<Any?>) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.arrayFormat(format, argArray).message
            delegate.log(null, fqcn, LocationAwareLogger.ERROR_INT, formattedMessage, argArray, null)
        }
    }

    override fun error(msg: String?, t: Throwable?) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            delegate.log(null, fqcn, LocationAwareLogger.ERROR_INT, msg, null, t)
        }
    }

    override fun error(marker: Marker?, msg: String?) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.ERROR_INT, msg, null, null)
        }
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg).message
            delegate.log(marker, fqcn, LocationAwareLogger.ERROR_INT, formattedMessage, arrayOf(arg), null)
        }
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.format(format, arg1, arg2).message
            delegate.log(marker, fqcn, LocationAwareLogger.ERROR_INT, formattedMessage, arrayOf(arg1, arg2), null)
        }
    }

    override fun error(marker: Marker?, format: String?, argArray: Array<Any?>) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            val formattedMessage = MessageFormatter.arrayFormat(format, argArray).message
            delegate.log(marker, fqcn, LocationAwareLogger.ERROR_INT, formattedMessage, argArray, null)
        }
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        if (!delegate.isErrorEnabled) {
            return
        }

        withMdc {
            delegate.log(marker, fqcn, LocationAwareLogger.ERROR_INT, msg, null, t)
        }
    }
}
