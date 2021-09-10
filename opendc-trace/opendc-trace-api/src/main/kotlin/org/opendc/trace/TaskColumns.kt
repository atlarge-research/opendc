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

@file:JvmName("TaskColumns")
package org.opendc.trace

import java.time.Duration
import java.time.Instant

/**
 * A column containing the task identifier.
 */
@JvmField
public val TASK_ID: TableColumn<String> = stringColumn("task:id")

/**
 * A column containing the identifier of the workflow.
 */
@JvmField
public val TASK_WORKFLOW_ID: TableColumn<String> = stringColumn("task:workflow_id")

/**
 * A column containing the submit time of the task.
 */
@JvmField
public val TASK_SUBMIT_TIME: TableColumn<Instant> = TableColumn("task:submit_time", type = Instant::class.java)

/**
 * A column containing the wait time of the task.
 */
@JvmField
public val TASK_WAIT_TIME: TableColumn<Instant> = TableColumn("task:wait_time", type = Instant::class.java)

/**
 * A column containing the runtime time of the task.
 */
@JvmField
public val TASK_RUNTIME: TableColumn<Duration> = TableColumn("task:runtime", type = Duration::class.java)

/**
 * A column containing the parents of a task.
 */
@Suppress("UNCHECKED_CAST")
@JvmField
public val TASK_PARENTS: TableColumn<Set<String>> = TableColumn("task:parents", type = Set::class.java as Class<Set<String>>)

/**
 * A column containing the children of a task.
 */
@Suppress("UNCHECKED_CAST")
@JvmField
public val TASK_CHILDREN: TableColumn<Set<String>> = TableColumn("task:children", type = Set::class.java as Class<Set<String>>)

/**
 * A column containing the requested CPUs of a task.
 */
@JvmField
public val TASK_REQ_NCPUS: TableColumn<Int> = intColumn("task:req_ncpus")

/**
 * A column containing the allocated CPUs of a task.
 */
@JvmField
public val TASK_ALLOC_NCPUS: TableColumn<Int> = intColumn("task:alloc_ncpus")

/**
 * A column containing the status of a task.
 */
@JvmField
public val TASK_STATUS: TableColumn<Int> = intColumn("task:status")

/**
 * A column containing the group id of a task.
 */
@JvmField
public val TASK_GROUP_ID: TableColumn<Int> = intColumn("task:group_id")

/**
 * A column containing the user id of a task.
 */
@JvmField
public val TASK_USER_ID: TableColumn<Int> = intColumn("task:user_id")
