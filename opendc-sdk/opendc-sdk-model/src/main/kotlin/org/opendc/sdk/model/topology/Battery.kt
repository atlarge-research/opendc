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
 * Decides when a battery charges from or discharges to the grid, typically driven by carbon intensity.
 */
@Serializable
public sealed interface BatteryPolicy

/**
 * Switches behavior around a single carbon-intensity threshold.
 *
 * @property carbonThreshold The carbon intensity above which the battery discharges.
 */
@Serializable
@SerialName("single")
public data class SingleThresholdPolicy(
    public val carbonThreshold: Double,
) : BatteryPolicy

/**
 * Uses separate thresholds to charge and discharge, creating a hysteresis band.
 *
 * @property lowerThreshold The carbon intensity below which the battery charges.
 * @property upperThreshold The carbon intensity above which the battery discharges.
 */
@Serializable
@SerialName("double")
public data class DoubleThresholdPolicy(
    public val lowerThreshold: Double,
    public val upperThreshold: Double,
) : BatteryPolicy

/**
 * Compares carbon intensity against a running mean over a sliding window.
 *
 * @property startingThreshold The initial threshold used before the window is populated.
 * @property windowSize The number of samples in the sliding window.
 */
@Serializable
@SerialName("runningMean")
public data class RunningMeanPolicy(
    public val startingThreshold: Double,
    public val windowSize: Int,
) : BatteryPolicy

/**
 * Compares carbon intensity against a running mean plus its variability over a sliding window.
 *
 * @property startingThreshold The initial threshold used before the window is populated.
 * @property windowSize The number of samples in the sliding window.
 */
@Serializable
@SerialName("runningMeanPlus")
public data class RunningMeanPlusPolicy(
    public val startingThreshold: Double,
    public val windowSize: Int,
) : BatteryPolicy

/**
 * Compares carbon intensity against a running median over a sliding window.
 *
 * @property startingThreshold The initial threshold used before the window is populated.
 * @property windowSize The number of samples in the sliding window.
 */
@Serializable
@SerialName("runningMedian")
public data class RunningMedianPolicy(
    public val startingThreshold: Double,
    public val windowSize: Int,
) : BatteryPolicy

/**
 * Compares carbon intensity against running quartiles over a sliding window.
 *
 * @property startingThreshold The initial threshold used before the window is populated.
 * @property windowSize The number of samples in the sliding window.
 */
@Serializable
@SerialName("runningQuartiles")
public data class RunningQuartilesPolicy(
    public val startingThreshold: Double,
    public val windowSize: Int,
) : BatteryPolicy

/**
 * An energy-storage unit attached to a cluster, charged and discharged according to its policy.
 *
 * @property name A human-readable identifier for the battery.
 * @property capacity The storage capacity, in kWh.
 * @property chargingSpeed The charging rate, in W.
 * @property initialCharge The charge present at the start of the simulation, in kWh.
 * @property policy The policy governing charging and discharging.
 * @property embodiedCarbon The carbon emitted during manufacturing, in kgCO2.
 * @property expectedLifetime The expected operational lifetime, in years.
 */
@Serializable
public data class Battery(
    public val name: String = "Battery",
    public val capacity: Double,
    public val chargingSpeed: Double,
    public val initialCharge: Double = 0.0,
    public val policy: BatteryPolicy,
    public val embodiedCarbon: Double = 0.0,
    public val expectedLifetime: Double = 0.0,
)
