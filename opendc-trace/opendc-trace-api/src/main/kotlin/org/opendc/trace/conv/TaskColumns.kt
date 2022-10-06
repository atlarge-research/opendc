/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.trace.conv

/**
 * A column containing the task identifier.
 */
public const val TASK_ID: String = "id"

/**
 * A column containing the identifier of the workflow.
 */
public const val TASK_WORKFLOW_ID: String = "workflow_id"

/**
 * A column containing the submission time of the task.
 */
public const val TASK_SUBMIT_TIME: String = "submit_time"

/**
 * A column containing the wait time of the task.
 */
public const val TASK_WAIT_TIME: String = "wait_time"

/**
 * A column containing the runtime time of the task.
 */
public const val TASK_RUNTIME: String = "runtime"

/**
 * A column containing the parents of a task.
 */
public const val TASK_PARENTS: String = "parents"

/**
 * A column containing the children of a task.
 */
public const val TASK_CHILDREN: String = "children"

/**
 * A column containing the requested CPUs of a task.
 */
public const val TASK_REQ_NCPUS: String = "req_ncpus"

/**
 * A column containing the allocated CPUs of a task.
 */
public const val TASK_ALLOC_NCPUS: String = "alloc_ncpus"

/**
 * A column containing the status of a task.
 */
public const val TASK_STATUS: String = "status"

/**
 * A column containing the group id of a task.
 */
public const val TASK_GROUP_ID: String = "group_id"

/**
 * A column containing the user id of a task.
 */
public const val TASK_USER_ID: String = "user_id"
