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

package org.opendc.experiments.base.experiment.specs

import kotlinx.serialization.Serializable

/**
 * Specification of the checkpoint model used by the Data Center.
 *
 * When using checkpointing the system periodically snapshots the progress of a task.
 * When a failure occurs the task resumes from its most recent checkpoint instead of restarting from the beginning.
 *
 * @property checkpointInterval The wall-clock time between two consecutive checkpoints, in milliseconds.
 * Must be positive. Default value is 1 hour.
 * @property checkpointDuration The time it takes to create a single checkpoint, in milliseconds.
 * Must be positive. Default value is 5 minutes.
 * @property checkpointIntervalScaling Factor by which [checkpointInterval] is scaled after each checkpoint. A value
 * of `1.0` keeps the interval constant. Must be positive.
 */
@Serializable
public data class CheckpointModelSpec(
    val checkpointInterval: Long = 60 * 60 * 1000,
    val checkpointDuration: Long = 5 * 60 * 1000,
    val checkpointIntervalScaling: Double = 1.0,
) {
    /**
     * Validate the constraints of this checkpoint model specification.
     *
     * All violated constraints are collected and reported together, so a user fixing a checkpoint
     * model sees every problem at once instead of one per run. When any constraint is violated an
     * [InvalidCheckpointModelException] is thrown; otherwise this returns nothing.
     *
     * @throws InvalidCheckpointModelException if one or more constraints are violated.
     */
    public fun validate() {
        val errors =
            buildList {
                if (checkpointInterval <= 0) {
                    add("The checkpoint interval must be positive (currently checkpointInterval=$checkpointInterval)")
                }
                if (checkpointDuration <= 0) {
                    add("The checkpoint duration can not be negative (currently checkpointDuration=$checkpointDuration)")
                }
                if (checkpointIntervalScaling <= 0.0) {
                    add("The checkpoint interval scaling must be positive (currently checkpointIntervalScaling=$checkpointIntervalScaling)")
                }
            }

        if (errors.isNotEmpty()) {
            throw InvalidCheckpointModelException(errors)
        }
    }
}

/**
 * Exception thrown when a [CheckpointModelSpec] violates one or more of its constraints.
 *
 * @property errors The human-readable descriptions of every violated constraint.
 */
public class InvalidCheckpointModelException(
    public val errors: List<String>,
) : IllegalArgumentException(
        "Invalid checkpoint model specification:\n" + errors.joinToString("\n") { "  - $it" },
    )
