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

package org.opendc.experiments.radice.scenario

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Helper class to describe various costs in a datacenter.
 *
 * @property costPerCpuH The cost of a vCPU per hour.
 * @property powerCost The cost of power as $ per MWh.
 * @property carbonCost The cost of carbon as $ per tCO2.
 * @property carbonSocialCost The social cost of carbon as $ per tCO2.
 * @property investigationCost The cost of an investigation into performance degradation.
 */
data class DatacenterCosts(
    @JsonProperty("vcpu") val costPerCpuH: Double,
    @JsonProperty("power") val powerCost: Double,
    @JsonProperty("co2") val carbonCost: Double,
    @JsonProperty("co2-social") val carbonSocialCost: Double,
    @JsonProperty("investigation") val investigationCost: Double,
)
