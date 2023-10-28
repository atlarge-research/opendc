/*
 * Copyright (c) 2023 AtLarge Research
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

import java.time.Instant;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.opendc.web.proto.JobState;

/**
 * A simulation job to be simulated by a runner.
 */
@Schema(name = "Runner.Job")
public record Job(
        long id,
        Scenario scenario,
        JobState state,
        Instant createdAt,
        Instant updatedAt,
        int runtime,
        Map<String, ?> results) {
    /**
     * A request to update the state of a job.
     *
     * @param state The next state of the job.
     * @param runtime The runtime of the job (in seconds).
     * @param results The results of the job.
     */
    @Schema(name = "Runner.Job.Update")
    public record Update(JobState state, int runtime, Map<String, ?> results) {}
}
