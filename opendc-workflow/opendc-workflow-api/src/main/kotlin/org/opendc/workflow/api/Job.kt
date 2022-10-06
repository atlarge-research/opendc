/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.workflow.api

import java.util.UUID

/**
 * A workload that represents a directed acyclic graph (DAG) of tasks with control and data dependencies between tasks.
 *
 * @property uid A unique identified of this workflow.
 * @property name The name of this workflow.
 * @property tasks The tasks that are part of this workflow.
 * @property metadata Additional metadata for the job.
 */
public data class Job(
    val uid: UUID,
    val name: String,
    val tasks: Set<Task>,
    val metadata: Map<String, Any> = emptyMap()
) {
    override fun equals(other: Any?): Boolean = other is Job && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()

    override fun toString(): String = "Job(uid=$uid, name=$name, tasks=${tasks.size}, metadata=$metadata)"
}
