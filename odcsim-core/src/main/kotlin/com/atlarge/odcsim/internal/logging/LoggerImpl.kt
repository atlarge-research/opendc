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

package com.atlarge.odcsim.internal.logging

import com.atlarge.odcsim.ActorContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.spi.LocationAwareLogger

/**
 * An actor-specific [Logger] implementation.
 *
 * @param ctx The owning [ActorContext] of this logger.
 */
abstract class LoggerImpl internal constructor(protected val ctx: ActorContext<*>) : Logger {
    /**
     * Configure [MDC] with actor-specific information.
     */
    protected inline fun withMdc(block: () -> Unit) {
        MDC.put(MDC_ACTOR_SYSTEM, ctx.system.name)
        MDC.put(MDC_ACTOR_TIME, String.format("%.2f", ctx.time))
        MDC.put(MDC_ACTOR_REF, ctx.self.path.toString())
        try {
            block()
        } finally {
            MDC.remove(MDC_ACTOR_SYSTEM)
            MDC.remove(MDC_ACTOR_TIME)
            MDC.remove(MDC_ACTOR_REF)
        }
    }

    /**
     * Mapped Diagnostic Context (MDC) attribute names.
     */
    companion object {
        val MDC_ACTOR_SYSTEM = "actor.system"
        val MDC_ACTOR_TIME = "actor.time"
        val MDC_ACTOR_REF = "actor.ref"

        /**
         * Create a [Logger] for the specified [ActorContext].
         *
         * @param ctx The actor context to create the logger for.
         */
        operator fun invoke(ctx: ActorContext<*>): Logger {
            val logger = LoggerFactory.getLogger(ctx.javaClass)
            return if (logger is LocationAwareLogger) {
                LocationAwareLoggerImpl(ctx, logger)
            } else {
                LocationIgnorantLoggerImpl(ctx, logger)
            }
        }
    }
}
