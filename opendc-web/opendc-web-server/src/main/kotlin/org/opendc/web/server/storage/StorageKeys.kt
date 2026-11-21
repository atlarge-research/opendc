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

package org.opendc.web.server.storage

import java.util.UUID

/**
 * Where the trace identified by [tracePublicId] keeps its tables. Keys derive from identity rather
 * than contents, because a browser is handed one before it has sent a byte.
 */
fun traceKey(tracePublicId: UUID): String = "traces/$tracePublicId"

/** Where one [table] of that trace lives. */
fun traceKey(
    tracePublicId: UUID,
    table: String,
): String = "${traceKey(tracePublicId)}/$table.parquet"

/** Everything an experiment keeps in the store, so deleting it is one prefix to remove. */
fun experimentKey(experimentPublicId: UUID): String = "experiments/$experimentPublicId"

/**
 * Where the runs of an experiment publish their parquet, as `<prefix>/<scenario>/seed=<seed>/`. The
 * layout under it is what an archive mirrors, so it matches a local run's output tree.
 */
fun resultKey(experimentPublicId: UUID): String = "${experimentKey(experimentPublicId)}/results"

/** Where one run publishes its files. */
fun runKey(
    experimentPublicId: UUID,
    scenarioIndex: Int,
    seed: Long,
): String = "${resultKey(experimentPublicId)}/$scenarioIndex/seed=$seed"

/** What one execution keeps beside the results: its manifest, its log and its units' outcomes. */
fun executionKey(
    experimentPublicId: UUID,
    executionPublicId: UUID,
): String = "${experimentKey(experimentPublicId)}/executions/$executionPublicId"

fun manifestKey(
    experimentPublicId: UUID,
    executionPublicId: UUID,
): String = "${executionKey(experimentPublicId, executionPublicId)}/manifest.json"

fun logKey(
    experimentPublicId: UUID,
    executionPublicId: UUID,
): String = "${executionKey(experimentPublicId, executionPublicId)}/launcher.log"

/**
 * Where one execution's launcher certifies how one unit ended. Kept under the execution rather than
 * beside the parquet, so an earlier attempt's marker can never be read as this one's.
 */
fun outcomeKey(
    experimentPublicId: UUID,
    executionPublicId: UUID,
    scenarioIndex: Int,
    seed: Long,
): String = "${executionKey(experimentPublicId, executionPublicId)}/units/$scenarioIndex/seed=$seed.json"
