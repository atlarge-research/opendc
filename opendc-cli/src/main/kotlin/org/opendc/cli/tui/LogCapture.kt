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

package org.opendc.cli.tui

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.ConsoleAppender
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.status.StatusLogger
import org.opendc.cli.config.CliConfig
import org.opendc.cli.config.Logging

/**
 * Redirects log4j into an in-memory buffer for the lifetime of the dashboard, then restores the
 * original configuration on [close]. Written to be independent of which `log4j2.xml` happens to win
 * on the classpath — some attach their console to `org.opendc` (with `additivity=false`) rather than
 * to root.
 *
 * While attached it:
 *  - silences log4j's [StatusLogger], which prints internal warnings (e.g. malformed message
 *    placeholders) straight to stderr, bypassing appenders — that would corrupt the animation;
 *  - detaches every console appender from every logger, so nothing writes to the screen;
 *  - attaches a bounded [RingBufferAppender] to the root logger and forces `org.opendc` to be
 *    additive and INFO, so its records stream into the ring regardless of the original layout.
 */
internal class LogCapture(private val logging: Logging = CliConfig.DEFAULTS.logging) : AutoCloseable {
    private val context = LogManager.getContext(false) as LoggerContext
    private val layout =
        PatternLayout.newBuilder()
            .withPattern(PATTERN)
            .withConfiguration(context.configuration)
            .build()
    private val appender = RingBufferAppender(layout, logging.ringCapacity)
    private val captureLevel: Level = Level.toLevel(logging.captureLevel, Level.WARN)

    private var previousLevel: Level? = null
    private var previousStatusLevel: Level? = null
    private var previousAdditive: Boolean? = null
    private var detached: List<Pair<LoggerConfig, Appender>> = emptyList()

    fun attach() {
        val config = context.configuration

        val statusListener = StatusLogger.getLogger().fallbackListener
        previousStatusLevel = statusListener.statusLevel
        statusListener.setLevel(Level.OFF)

        detached =
            config.loggers.values.flatMap { logger ->
                logger.appenders.values.filterIsInstance<ConsoleAppender>().map { logger to it }
            }
        detached.forEach { (logger, console) -> logger.removeAppender(console.name) }

        val capture = config.getLoggerConfig(CAPTURE_LOGGER)
        previousLevel = capture.level
        if (capture.name == CAPTURE_LOGGER) {
            previousAdditive = capture.isAdditive
            capture.isAdditive = true
        }
        config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).addAppender(appender, null, null)
        context.updateLoggers()
        Configurator.setLevel(CAPTURE_LOGGER, captureLevel)
    }

    /** The most recent buffered log lines, oldest first. */
    fun snapshot(): List<String> = appender.snapshot()

    override fun close() {
        val config = context.configuration
        config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).removeAppender(appender.name)
        detached.forEach { (logger, console) -> logger.addAppender(console, null, null) }
        previousAdditive?.let { config.getLoggerConfig(CAPTURE_LOGGER).isAdditive = it }
        context.updateLoggers()
        Configurator.setLevel(CAPTURE_LOGGER, previousLevel ?: Level.WARN)
        previousStatusLevel?.let { StatusLogger.getLogger().fallbackListener.setLevel(it) }
        appender.stop()
    }

    private companion object {
        /** The logger namespace whose records the dashboard captures, and the layout it formats them with. */
        const val CAPTURE_LOGGER = "org.opendc"
        const val PATTERN = "%-5level %logger{1} %msg"
    }
}
