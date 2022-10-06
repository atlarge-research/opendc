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

@file:JvmName("Tables")

package org.opendc.trace.conv

/**
 * A table containing all workflows in a workload.
 */
public const val TABLE_WORKFLOWS: String = "workflows"

/**
 * A table containing all tasks in a workload.
 */
public const val TABLE_TASKS: String = "tasks"

/**
 * A table containing all resources in a workload.
 */
public const val TABLE_RESOURCES: String = "resources"

/**
 * A table containing all resource states in a workload.
 */
public const val TABLE_RESOURCE_STATES: String = "resource_states"

/**
 * A table containing the groups of resources that interfere when run on the same execution platform.
 */
public const val TABLE_INTERFERENCE_GROUPS: String = "interference_groups"
