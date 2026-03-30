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
 * Identifier of the task.
 */
public const val TASK_ID: String = "id"

/**
 * Identifier of the task.
 */
public const val TASK_NAME: String = "name"

/**
 * The time of submission of the task.
 */
public const val TASK_SUBMISSION_TIME: String = "submission_time"

/**
 * The duration of a task in ms
 */
public const val TASK_DURATION: String = "duration"

/**
 * Number of CPUs for the task.
 */
public const val TASK_CPU_COUNT: String = "cpu_count"

/**
 * Total CPU capacity of the task in MHz.
 */
public const val TASK_CPU_CAPACITY: String = "cpu_capacity"

/**
 * Memory capacity for the task in KB.
 */
public const val TASK_MEM_CAPACITY: String = "mem_capacity"

/**
 * Number of GPU cores for the task.
 */
public const val TASK_GPU_COUNT: String = "gpu_count"

/**
 * Total GPU capacity of the task in MHz.
 */
public const val TASK_GPU_CAPACITY: String = "gpu_capacity"

/**
 * Total GPU memory capacity of the task in MB.
 */
public const val TASK_GPU_MEM_CAPACITY: String = "gpu_mem_capacity"

/**
 * The parents of the task that need to be completed before this task can be used.
 */
public const val TASK_PARENTS: String = "parents"

/**
 * The children of the task that cannot be started before this is completed.
 */
public const val TASK_CHILDREN: String = "children"

/**
 * Nature of the task. Delayable, interruptible, etc.
 */
public const val TASK_DEFERRABLE: String = "deferrable"

/**
 * Deadline of the task.
 */
public const val TASK_DEADLINE: String = "deadline"
