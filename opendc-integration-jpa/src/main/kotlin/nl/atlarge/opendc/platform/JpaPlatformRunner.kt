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

import mu.KotlinLogging
import nl.atlarge.opendc.kernel.omega.OmegaKernel
import java.util.concurrent.Executors
import javax.persistence.Persistence

val logger = KotlinLogging.logger {}

/**
 * The main entry point of the program. This program polls experiments from a database and runs the
 * simulation and reports the results back to the database.
 *
 * @param args The command line arguments of the program.
 */
fun main(args: Array<String>) {
	val threads = 1
	val executorService = Executors.newFixedThreadPool(threads)
	val factory = Persistence.createEntityManagerFactory("opendc-frontend")
	val experiments = JpaExperimentManager(factory)

	logger.info { "Waiting for enqueued experiments..." }
	while (true) {
		val experiment = experiments.poll()
		executorService.submit {
			experiment?.run {
				logger.info { "Found experiment. Running simulating now..." }
				run(OmegaKernel)
			}
		}

		Thread.sleep(500)
	}
}
