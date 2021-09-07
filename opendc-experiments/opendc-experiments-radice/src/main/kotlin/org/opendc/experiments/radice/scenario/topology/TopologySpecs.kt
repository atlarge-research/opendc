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

@file:JvmName("TopologySpecs")
package org.opendc.experiments.radice.scenario.topology

/**
 * Base topology
 */
@JvmField
val BASE_TOPOLOGY = TopologySpec(
    id = "base",
    clusters = listOf(
        ClusterSpec("1", "1", DELL_R620_FAST, 16),
        ClusterSpec("2", "2", DELL_R710, 16),
        ClusterSpec("3", "3", DELL_R620_FAST, 16),
        ClusterSpec("4", "4", DELL_R620, 16),
        ClusterSpec("5", "5", DELL_R620, 16),
        ClusterSpec("6", "6", DELL_R620, 16),
        ClusterSpec("3", "3", DELL_R620, 16),
        ClusterSpec("7", "7", DELL_R620, 16),
        ClusterSpec("8", "8", DELL_R620, 16),
        ClusterSpec("9", "9", DELL_R620, 16),
        ClusterSpec("10", "10", DELL_R820, 6),
        ClusterSpec("11", "11", DELL_R820, 6),
        ClusterSpec("12", "12", DELL_R820, 6),
    )
)

/**
 * Base topology
 */
@JvmField
val BASE_TOPOLOGY_90P = BASE_TOPOLOGY.scale("base-90%", 0.9)

/**
 * Base topology
 */
@JvmField
val BASE_TOPOLOGY_75P = BASE_TOPOLOGY.scale("base-75%", 0.75)

/**
 * Base topology
 */
@JvmField
val BASE_TOPOLOGY_50P = BASE_TOPOLOGY.scale("base-50%", 0.5)

/**
 * Base topology
 */
@JvmField
val BASE_TOPOLOGY_25P = BASE_TOPOLOGY.scale("base-25%", 0.25)

/**
 * Base topology
 */
@JvmField
val BASE_TOPOLOGY_200P = BASE_TOPOLOGY.scale("base-200%", 2.0)

/**
 * Base topology
 */
@JvmField
val BASE_TOPOLOGY_400P = BASE_TOPOLOGY.scale("base-400%", 4.0)
