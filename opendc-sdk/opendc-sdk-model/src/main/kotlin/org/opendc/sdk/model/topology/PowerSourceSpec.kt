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
import org.opendc.common.units.Power
import org.opendc.sdk.model.carbon.CarbonModelSpec
import org.opendc.sdk.model.carbon.CarbonModelSpecSerializer
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.prefixed

/**
 * A power source feeding a cluster.
 *
 * @property name Human-readable identifier.
 * @property maxPower Maximum power the source can deliver. The default unit is Watt: a bare number such as
 *   `400` means 400 W.
 * @property carbon The carbon intensity of the energy the source delivers, if any.
 */
@Serializable
public data class PowerSourceSpec(
    public val name: String = "PowerSource",
    public val maxPower: Power = Power.ofWatts(Long.MAX_VALUE.toDouble()),
    @Serializable(with = CarbonModelSpecSerializer::class)
    public val carbon: CarbonModelSpec? = null,
) : Validatable {
    override fun validate(): List<ValidationIssue> = carbon?.validate()?.prefixed("carbon").orEmpty()
}
