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

@file:JvmName("HostAttributes")
package org.opendc.telemetry.compute

import io.opentelemetry.api.common.AttributeKey

/**
 * The identifier of the node hosting virtual machines.
 */
public val HOST_ID: AttributeKey<String> = AttributeKey.stringKey("node.id")

/**
 * The name of the node hosting virtual machines.
 */
public val HOST_NAME: AttributeKey<String> = AttributeKey.stringKey("node.name")

/**
 * The CPU architecture of the host node.
 */
public val HOST_ARCH: AttributeKey<String> = AttributeKey.stringKey("node.arch")

/**
 * The number of CPUs in the host node.
 */
public val HOST_NCPUS: AttributeKey<Long> = AttributeKey.longKey("node.num_cpus")

/**
 * The amount of memory installed in the host node in MiB.
 */
public val HOST_MEM_CAPACITY: AttributeKey<Long> = AttributeKey.longKey("node.mem_capacity")
