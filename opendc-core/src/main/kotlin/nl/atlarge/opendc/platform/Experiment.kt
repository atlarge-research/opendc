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

package nl.atlarge.opendc.platform

import nl.atlarge.opendc.kernel.Kernel
import nl.atlarge.opendc.kernel.time.Duration

/**
 * A blueprint for a reproducible simulation in a pre-defined setting.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Experiment<out T> {
	/**
	 * Run the experiment on the specified simulation [Kernel].
	 *
	 * @param kernel The simulation kernel to run the experiment.
	 * @return The result of the experiment.
	 */
	fun run(kernel: Kernel): T

	/**
	 * Run the experiment on the specified simulation [Kernel].
	 *
	 * @param kernel The simulation kernel to run the experiment.
	 * @param timeout The maximum duration of the experiment before returning to the caller.
	 * @return The result of the experiment or `null`.
	 */
	fun run(kernel: Kernel, timeout: Duration): T?
}
