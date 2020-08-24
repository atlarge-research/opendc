/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.workflows.workload

import com.atlarge.opendc.core.User
import com.atlarge.opendc.core.workload.Workload
import java.util.UUID

/**
 * A workload that represents a directed acyclic graph (DAG) of tasks with control and data dependencies between tasks.
 *
 * @property uid A unique identified of this workflow.
 * @property name The name of this workflow.
 * @property owner The owner of the workflow.
 * @property tasks The tasks that are part of this workflow.
 * @property metadata Additional metadata for the job.
 */
data class Job(
    override val uid: UUID,
    override val name: String,
    override val owner: User,
    val tasks: Set<Task>,
    val metadata: Map<String, Any> = emptyMap()
) : Workload {
    override fun equals(other: Any?): Boolean = other is Job && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()

    override fun toString(): String = "Job(uid=$uid, name=$name, tasks=${tasks.size}, metadata=$metadata)"
}
