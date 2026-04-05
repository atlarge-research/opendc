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

package org.opendc.web.runner.internal

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import java.time.Instant

/**
 * A log collector that captures WARN and ERROR level messages during simulation execution.
 */
internal class ReportCollector : AbstractAppender(
    "ReportCollector",
    null,
    null,
    true,
    Property.EMPTY_ARRAY,
) {
    private val logs = mutableListOf<LogEntry>()

    init {
        start()
    }

    override fun append(event: LogEvent) {
        if (event.level == Level.WARN || event.level == Level.ERROR) {
            logs.add(
                LogEntry(
                    timestamp = Instant.ofEpochMilli(event.instant.epochMillisecond),
                    level = event.level.name(),
                    logger = event.loggerName,
                    message = event.message.formattedMessage,
                ),
            )
        }
    }

    /**
     * Attach this collector to the root logger.
     */
    fun attach() {
        val rootLogger = LogManager.getRootLogger() as Logger
        rootLogger.addAppender(this)
    }

    /**
     * Detach this collector from the root logger.
     */
    fun detach() {
        val rootLogger = LogManager.getRootLogger() as Logger
        rootLogger.removeAppender(this)
    }

    /**
     * Collect all captured logs and return them as a map structure.
     *
     * @param runtimeSeconds The runtime of the job in seconds (optional).
     * @param waitTimeSeconds The time the job spent waiting in the queue in seconds (optional).
     * @param createdAt The time the job was created (optional).
     * @param startedAt The time the job started running (optional).
     */
    fun collect(
        runtimeSeconds: Int? = null,
        waitTimeSeconds: Int? = null,
        createdAt: Instant? = null,
        startedAt: Instant? = null,
    ): Map<String, Any> {
        val logEntries =
            logs.map {
                mapOf(
                    "timestamp" to it.timestamp.toString(),
                    "level" to it.level,
                    "logger" to it.logger,
                    "message" to it.message,
                )
            }

        val summary =
            buildMap<String, Any> {
                put("totalWarnings", logs.count { it.level == "WARN" })
                put("totalErrors", logs.count { it.level == "ERROR" })
                if (runtimeSeconds != null) {
                    put("runtimeSeconds", runtimeSeconds)
                }
                if (waitTimeSeconds != null) {
                    put("waitTimeSeconds", waitTimeSeconds)
                }
            }

        return buildMap {
            if (createdAt != null) {
                put("createdAt", createdAt.toString())
            }
            if (startedAt != null) {
                put("startedAt", startedAt.toString())
            }
            put("logs", logEntries)
            put("summary", summary)
        }
    }

    /**
     * Clear all collected logs.
     */
    fun clear() {
        logs.clear()
    }

    /**
     * Represents a single log entry.
     */
    private data class LogEntry(
        val timestamp: Instant,
        val level: String,
        val logger: String,
        val message: String,
    )
}
