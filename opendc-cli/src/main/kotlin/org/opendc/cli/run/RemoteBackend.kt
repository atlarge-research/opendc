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

package org.opendc.cli.run

/**
 * Runs an experiment on a remote OpenDC server (SaaS mode). NOT YET IMPLEMENTED — this is the
 * documented seam that makes the shape of remote execution explicit.
 *
 * The intent is that the `run` command and its dashboard behave identically to local mode, because a
 * remote session exposes the same [SimulationSession]. The planned flow:
 *
 *  1. POST the `SdkJson`-serialized experiment to `{apiUrl}/experiments` → `{ "jobId": "..." }`.
 *  2. A `RemoteProgressSource` polls `GET {apiUrl}/jobs/{jobId}/status` on the dashboard's cadence,
 *     decoding
 *         { "state": "PENDING|RUNNING|DONE|FAILED", "completedTasks": <long>, "totalTasks": <long> }
 *     and mapping `completedTasks`/`totalTasks` to a `ProgressSnapshot`. Because the reporters depend
 *     only on [org.opendc.cli.progress.ProgressSource], this poller is a drop-in replacement for the
 *     local [org.opendc.cli.progress.ExperimentProgress].
 *  3. On `state == DONE`, GET `{apiUrl}/jobs/{jobId}/result` → a payload mapped to the SAME
 *     [org.opendc.cli.render.RunSummaryView] / [org.opendc.cli.render.OutputView] that [LocalBackend]
 *     produces, so the summary and output rendering are identical.
 *
 * Those server endpoints do not exist yet (the web server must first adopt the opendc-sdk runtime), so
 * [prepare] fails fast. It throws a plain [NotImplementedError] to stay free of the CLI framework; the
 * command layer translates it into a clean, user-facing error.
 */
internal class RemoteBackend(private val apiUrl: String) : SimulationBackend {
    override fun prepare(request: RunRequest): SimulationSession =
        throw NotImplementedError("Remote execution via --api-url ($apiUrl) is not implemented yet.")
}
