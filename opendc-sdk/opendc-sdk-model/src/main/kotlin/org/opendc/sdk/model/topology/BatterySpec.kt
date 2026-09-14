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

import kotlinx.serialization.Serializable

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
public data class BatterySpec(
    public val name: String = "Battery",
    public val capacity: Double,
    public val chargingSpeed: Double,
    public val initialCharge: Double = 0.0,
    public val policy: BatteryPolicySpec,
    public val embodiedCarbon: Double = 0.0,
    public val expectedLifetime: Double = 0.0,
)
