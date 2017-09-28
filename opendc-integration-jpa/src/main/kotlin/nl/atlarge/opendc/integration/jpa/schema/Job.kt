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

package nl.atlarge.opendc.integration.jpa.schema

import nl.atlarge.opendc.platform.workload.Job
import nl.atlarge.opendc.platform.workload.Task
import nl.atlarge.opendc.platform.workload.User
import javax.persistence.*

/**
 * A [Job] backed by the JPA API and an underlying database connection.
 *
 * @property id The unique identifier of the job.
 * @property tasks The collection of tasks the job consists of.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
@Entity
data class Job(
	override val id: Int,
	override val tasks: Set<Task>
) : Job {
	/**
	 * The owner of the job, which is a singleton, since the database has no
	 * concept of ownership yet.
	 */
	override val owner: User = object : User {
		/**
         * The unique identifier of the user.
         */
		override val id: Int = 0

		/**
         * The name of this user.
         */
		override val name: String = "admin"
	}
}
