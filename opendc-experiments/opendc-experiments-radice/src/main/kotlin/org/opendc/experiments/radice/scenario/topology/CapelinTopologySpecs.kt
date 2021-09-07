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

@file:JvmName("CapelinTopologySpecs")
package org.opendc.experiments.radice.scenario.topology

/**
 * Expansion (velocity, vertical, heterogeneous)
 */
val CAPELIN_EXP_VEL_VER_HET = TopologySpec(
    id = "exp-vel-ver-het",
    clusters = BASE_TOPOLOGY.clusters + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_FAST, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620_FAST, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_FAST, 16),
        ClusterSpec("NEW4", "NEW4", DELL_R620_FAST, 16),
        ClusterSpec("NEW5", "NEW5", DELL_R620, 16),
        ClusterSpec("NEW6", "NEW6", DELL_R620, 16),
    )
)

/**
 * Expansion (velocity, vertical, homogenous)
 */
val CAPELIN_EXP_VEL_VER_HOM = TopologySpec(
    id = "exp-vel-ver-hom",
    clusters = BASE_TOPOLOGY.clusters + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_FAST, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620_FAST, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_FAST, 16),
        ClusterSpec("NEW4", "NEW4", DELL_R620_FAST, 16),
        ClusterSpec("NEW5", "NEW5", DELL_R620_FAST, 16),
        ClusterSpec("NEW6", "NEW6", DELL_R620_FAST, 16),
    )
)

/**
 * Expansion (volume, horizontal, heterogeneous)
 */
val CAPELIN_EXP_VOL_HOR_HET = TopologySpec(
    id = "exp-vol-hor-het",
    clusters = BASE_TOPOLOGY.clusters + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW4", "NEW4", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW5", "NEW5", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW6", "NEW6", DELL_R620_128_CORE, 2),
    )
)

/**
 * Expansion (volume, horizontal, homogeneous)
 */
val CAPELIN_EXP_VOL_HOR_HOM = TopologySpec(
    id = "exp-vol-hor-hom",
    clusters = BASE_TOPOLOGY.clusters + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW4", "NEW4", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW5", "NEW5", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW6", "NEW6", DELL_R620_28_CORE, 16),
    )
)

/**
 * Expansion (volume, vertical, heterogeneous)
 */
val CAPELIN_EXP_VOL_VER_HET = TopologySpec(
    id = "exp-vol-ver-het",
    clusters = BASE_TOPOLOGY.clusters + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW4", "NEW4", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW5", "NEW5", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW6", "NEW6", DELL_R620_128_CORE, 2),
    )
)

/**
 * Expansion (volume, vertical, homogeneous)
 */
val CAPELIN_EXP_VOL_VER_HOM = TopologySpec(
    id = "exp-vol-ver-hom",
    clusters = BASE_TOPOLOGY.clusters + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW2", "NEW2", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW3", "NEW3", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW4", "NEW4", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW5", "NEW5", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW6", "NEW6", DELL_R620_128_CORE, 2),
    )
)

/**
 * Subset of clusters for the replacement topologies.
 */
private val REP_SUBSET = listOf(
    ClusterSpec("1", "1", DELL_R620_FAST, 16),
    ClusterSpec("2", "2", DELL_R710, 16),
    ClusterSpec("3", "3", DELL_R620_FAST, 16),
    ClusterSpec("4", "4", DELL_R820, 6),
    ClusterSpec("5", "5", DELL_R820, 6),
    ClusterSpec("6", "6", DELL_R820, 6),
)

/**
 * Replacement (velocity, vertical, heterogeneous)
 */
val CAPELIN_REP_VEL_VER_HET = TopologySpec(
    id = "rep-vel-ver-het",
    clusters = REP_SUBSET + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_FAST, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620_FAST, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_FAST, 16),
        ClusterSpec("NEW4", "NEW4", DELL_R620_FAST, 16),
        ClusterSpec("NEW5", "NEW5", DELL_R620, 16),
        ClusterSpec("NEW6", "NEW6", DELL_R620, 16),
    )
)

/**
 * Replacement (velocity, vertical, homogeneous)
 */
val CAPELIN_REP_VEL_VER_HOM = TopologySpec(
    id = "rep-vel-ver-hom",
    clusters = REP_SUBSET + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_FAST, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620_FAST, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_FAST, 16),
        ClusterSpec("NEW4", "NEW4", DELL_R620_FAST, 16),
        ClusterSpec("NEW5", "NEW5", DELL_R620_FAST, 16),
        ClusterSpec("NEW6", "NEW6", DELL_R620_FAST, 16),
    )
)

/**
 * Replacement (volume, horizontal, heterogeneous)
 */
val CAPELIN_REP_VOL_HOR_HET = TopologySpec(
    id = "rep-vol-hor-het",
    clusters = REP_SUBSET + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW4", "NEW4", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW5", "NEW5", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW6", "NEW6", DELL_R620_128_CORE, 2),
    )
)

/**
 * Replacement (volume, horizontal, homogeneous)
 */
val CAPELIN_REP_VOL_HOR_HOM = TopologySpec(
    id = "rep-vol-hor-hom",
    clusters = REP_SUBSET + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW4", "NEW4", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW5", "NEW5", DELL_R620_28_CORE, 16),
        ClusterSpec("NEW6", "NEW6", DELL_R620_28_CORE, 16),
    )
)

/**
 * Replacement (volume, vertical, complete)
 */
val CAPELIN_REP_VOL_VER_COM = TopologySpec(
    id = "rep-vol-ver-com",
    clusters = listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R820_128_CORE, 10),
        ClusterSpec("NEW2", "NEW1", DELL_R820_128_CORE, 12),
    )
)

/**
 * Replacement (volume, vertical, heterogeneous)
 */
val CAPELIN_REP_VOL_VER_HET = TopologySpec(
    id = "rep-vol-ver-het",
    clusters = REP_SUBSET + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620, 16),
        ClusterSpec("NEW2", "NEW2", DELL_R620, 16),
        ClusterSpec("NEW3", "NEW3", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW4", "NEW4", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW5", "NEW5", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW6", "NEW6", DELL_R620_128_CORE, 2),
    )
)

/**
 * Replacement (volumne, vertical, homogeneous)
 */
val CAPELIN_REP_VOL_VER_HOM = TopologySpec(
    id = "rep-vol-ver-hom",
    clusters = REP_SUBSET + listOf(
        ClusterSpec("NEW1", "NEW1", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW2", "NEW2", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW3", "NEW3", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW4", "NEW4", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW5", "NEW5", DELL_R620_128_CORE, 2),
        ClusterSpec("NEW6", "NEW6", DELL_R620_128_CORE, 2),
    )
)
