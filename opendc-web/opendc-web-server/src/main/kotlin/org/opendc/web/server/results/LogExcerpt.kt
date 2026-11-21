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

package org.opendc.web.server.results

/** A record starts with the launcher's ISO 8601 timestamp; anything else continues the one above it. */
private val RECORD_START = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}""")

/** One scenario's share of a launcher log, and how many of its earlier lines were left out. */
data class LogExcerpt(
    val lines: List<String>,
    val omitted: Int,
) {
    companion object {
        /**
         * The lines of a launcher log written while [scenarioIndex] ran, keeping the last [limit].
         *
         * The launcher tags every record a unit writes with `[scenario=<index> seed=<seed>]`. A stack
         * trace runs over several lines and only its first carries the tag, so an untagged line
         * belongs to the record above it, kept or dropped with it.
         */
        fun ofScenario(
            lines: Sequence<String>,
            scenarioIndex: Int,
            limit: Int,
        ): LogExcerpt {
            val tag = "[scenario=$scenarioIndex "
            val kept = ArrayDeque<String>()
            var omitted = 0
            var inside = false
            for (line in lines) {
                if (RECORD_START.containsMatchIn(line)) {
                    inside = tag in line
                }
                if (!inside) {
                    continue
                }
                kept.addLast(line)
                if (kept.size > limit) {
                    kept.removeFirst()
                    omitted++
                }
            }
            return LogExcerpt(kept.toList(), omitted)
        }
    }
}
