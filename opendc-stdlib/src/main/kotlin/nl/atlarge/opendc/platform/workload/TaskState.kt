/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package nl.atlarge.opendc.platform.workload

import nl.atlarge.opendc.kernel.time.Instant


/**
 * This class hierarchy describes the states of a [Task].
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
sealed class TaskState {
	/**
	 * A state to indicate the task has not yet arrived at the [Datacenter].
	 */
	object Underway : TaskState()

	/**
	 * A state to indicate the task has arrived at the [Datacenter].
	 *
	 * @property at The moment in time the task has arrived.
	 */
	data class Queued(val at: Instant) : TaskState()

	/**
	 * A state to indicate the task has started running on a machine.
	 *
	 * @property previous The previous state of the task.
	 * @property at The moment in time the task started.
	 */
	data class Running(val previous: Queued, val at: Instant) : TaskState()

	/**
	 * A state to indicate the task has finished.
	 *
	 * @property previous The previous state of the task.
	 * @property at The moment in time the task finished.
	 */
	data class Finished(val previous: Running, val at: Instant) : TaskState()

	/**
	 * A state to indicate the task has failed.
	 *
	 * @property previous The previous state of the task.
	 * @property at The moment in time the task failed.
	 * @property reason The reason of the failure.
	 */
	data class Failed(val previous: Running, val at: Instant, val reason: String) : TaskState()
}
