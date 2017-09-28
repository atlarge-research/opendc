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

import nl.atlarge.opendc.integration.jpa.schema.Experiment as InternalExperiment
import nl.atlarge.opendc.integration.jpa.schema.ExperimentState
import javax.persistence.EntityManager

/**
 * A manager for [Experiment]s received from a JPA database.
 *
 * @property manager The JPA entity manager to retrieve entities from the database from.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class JpaExperimentManager(private val manager: EntityManager) {
	/**
	 * The amount of experiments in the queue. This property makes a call to the database and does therefore not
	 * run in O(1) time.
	 */
	val size: Int
		get() {
			return manager.createQuery("SELECT COUNT(e.id) FROM experiments e WHERE e.state = :s",
					java.lang.Long::class.java)
				.setParameter("s", ExperimentState.QUEUED)
				.singleResult.toInt()
		}

	/**
	 * Poll an [Experiment] from the database and claim it.
	 *
	 * @return The experiment that has been polled from the database or `null` if there are no experiments in the
	 * 		   queue.
	 */
	fun poll(): Experiment<Unit>? {
		manager.transaction.begin()
		var experiment: InternalExperiment? = null
		val results = manager.createQuery("SELECT e FROM experiments e WHERE e.state = :s",
				InternalExperiment::class.java)
			.setParameter("s", ExperimentState.QUEUED)
			.setMaxResults(1)
			.resultList


		if (!results.isEmpty()) {
			experiment = results.first()
			experiment!!.state = ExperimentState.CLAIMED
		}
		manager.transaction.commit()
		return experiment?.let { JpaExperiment(manager, it) }
	}
}
