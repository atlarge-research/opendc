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

package org.opendc.experiments.radice.scenario.mapper

import com.fasterxml.jackson.databind.util.StdConverter
import org.opendc.experiments.radice.scenario.topology.*

/**
 * A converter from [String] to [MachineSpec].
 */
class StringToMachineSpecConverter : StdConverter<String, MachineSpec>() {
    private val models = listOf(
        DELL_R620,
        DELL_R620_FAST,
        DELL_R620_28_CORE,
        DELL_R620_128_CORE,
        DELL_R710,
        DELL_R820,
        DELL_R820_128_CORE,
        DELL_R740,
        DELL_R240,
        DELL_R6515,
        DELL_R7515,
        DELL_R7525,
        HPE_DL345_GEN10_PLUS,
        HPE_DL380_GEN10_PLUS,
        HPE_DL110_GEN10_PLUS
    ).associateBy { it.id }

    override fun convert(value: String): MachineSpec = checkNotNull(models[value]) { "Unknown machine model $value" }
}
