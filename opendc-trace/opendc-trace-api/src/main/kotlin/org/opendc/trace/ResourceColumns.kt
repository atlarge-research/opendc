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

@file:JvmName("ResourceColumns")
package org.opendc.trace

import java.time.Instant

/**
 * Identifier of the resource.
 */
@JvmField
public val RESOURCE_ID: TableColumn<String> = stringColumn("resource:id")

/**
 * Start time for the resource.
 */
@JvmField
public val RESOURCE_START_TIME: TableColumn<Instant> = TableColumn("resource:start_time", Instant::class.java)

/**
 * End time for the resource.
 */
@JvmField
public val RESOURCE_STOP_TIME: TableColumn<Instant> = TableColumn("resource:stop_time", Instant::class.java)

/**
 * Number of CPUs for the resource.
 */
@JvmField
public val RESOURCE_NCPUS: TableColumn<Int> = intColumn("resource:num_cpus")

/**
 * Memory capacity for the resource in KB.
 */
@JvmField
public val RESOURCE_MEM_CAPACITY: TableColumn<Double> = doubleColumn("resource:mem_capacity")
