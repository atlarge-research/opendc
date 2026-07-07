/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.topology

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Determines how a shared resource's capacity is distributed among competing consumers.
 */
@Serializable
public sealed interface DistributionPolicy

/** Distributes capacity fairly, maximizing the minimum share across consumers. */
@Serializable
@SerialName("maxMinFairness")
public data object MaxMinFairness : DistributionPolicy

/**
 * Distributes capacity opportunistically, refreshing shares on a fixed cadence.
 *
 * @property updateIntervalMs The interval, in milliseconds, between share recalculations.
 */
@Serializable
@SerialName("bestEffort")
public data class BestEffort(
    public val updateIntervalMs: Long = 1000,
) : DistributionPolicy

/** Grants every consumer an identical share of the capacity. */
@Serializable
@SerialName("equalShare")
public data object EqualShare : DistributionPolicy

/** Satisfies consumers in order, assigning each its full demand until capacity is exhausted. */
@Serializable
@SerialName("firstFit")
public data object FirstFit : DistributionPolicy

/**
 * Grants each consumer a fixed fraction of the capacity.
 *
 * @property shareRatio The fraction of capacity assigned per consumer.
 */
@Serializable
@SerialName("fixedShare")
public data class FixedShare(
    public val shareRatio: Double = 1.0,
) : DistributionPolicy
