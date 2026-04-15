/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.web.proto.runner;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A report containing metadata, logs, and summary information about a simulation job.
 */
@Schema(name = "Runner.Report")
public record Report(String createdAt, String startedAt, List<LogEntry> logs, Summary summary, ErrorInfo error) {

    /**
     * A single log entry captured during simulation execution.
     */
    @Schema(name = "Runner.Report.LogEntry")
    public record LogEntry(String timestamp, String level, String logger, String message) {}

    /**
     * Aggregate summary of the simulation job.
     */
    @Schema(name = "Runner.Report.Summary")
    public record Summary(int totalWarnings, int totalErrors, Integer runtimeSeconds, Integer waitTimeSeconds) {}

    /**
     * Error information when a simulation job fails.
     */
    @Schema(name = "Runner.Report.ErrorInfo")
    public record ErrorInfo(String message, String type, String stackTrace) {}
}
