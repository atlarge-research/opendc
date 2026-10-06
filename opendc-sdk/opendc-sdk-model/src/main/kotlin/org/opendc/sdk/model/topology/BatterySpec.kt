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
import org.opendc.common.units.Energy
import org.opendc.common.units.Power

/**
 * An energy-storage unit attached to a cluster, charged and discharged according to its policy.
 *
 * @property name A human-readable identifier for the battery.
 * @property capacity The storage capacity. The default unit is Joule: a bare number such as `100` means 100 J, so
 *   write `"100 kWh"` for kilowatt-hours.
 * @property chargingSpeed The charging rate. The default unit is Watt: a bare number such as `1000` means 1000 W.
 * @property initialCharge The charge present at the start of the simulation. The default unit is Joule: a bare number
 *   such as `20` means 20 J, so write `"20 kWh"` for kilowatt-hours.
 * @property policy The policy governing charging and discharging.
 * @property embodiedCarbon The carbon emitted during manufacturing, in kgCO2.
 * @property expectedLifetime The expected operational lifetime, in years.
 */
@Serializable
public data class BatterySpec(
    public val name: String = "Battery",
    public val capacity: Energy,
    public val chargingSpeed: Power,
    public val initialCharge: Energy = Energy.zero,
    public val policy: BatteryPolicySpec,
    public val embodiedCarbon: Double = 0.0,
    public val expectedLifetime: Double = 0.0,
)
