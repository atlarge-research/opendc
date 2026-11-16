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

package org.opendc.simulator.compute.scheduler.priority

import org.opendc.simulator.compute.task.SimTask

/**
 * The priority of a [SimTask] waiting to be placed: the higher the value, the sooner it is placed. See
 * [org.opendc.simulator.compute.scheduler.ComputeScheduler.taskPriority].
 */
public fun interface TaskPriority {
    /**
     * The priority of [task].
     */
    public fun of(task: SimTask): Double
}

/**
 * Priorities given per task id, for example the priorities a workflow planner assigned to its jobs. A task without
 * one gets [default].
 */
public class ScorePriority(
    scores: Map<Int, Double>,
    private val default: Double = 0.0,
) : TaskPriority {
    private val scores = HashMap(scores)

    override fun of(task: SimTask): Double = scores[task.id] ?: default

    override fun toString(): String = "ScorePriority(${scores.size} tasks)"
}
