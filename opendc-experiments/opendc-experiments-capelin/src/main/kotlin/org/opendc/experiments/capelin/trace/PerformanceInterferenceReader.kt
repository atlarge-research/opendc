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

package org.opendc.experiments.capelin.trace

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opendc.simulator.compute.kernel.interference.VmInterferenceGroup
import java.io.InputStream

/**
 * A parser for the JSON performance interference setup files used for the TPDS article on Capelin.
 */
class PerformanceInterferenceReader {
    /**
     * The [ObjectMapper] to use.
     */
    private val mapper = jacksonObjectMapper()

    init {
        mapper.addMixIn(VmInterferenceGroup::class.java, GroupMixin::class.java)
    }

    /**
     * Read the performance interface model from the input.
     */
    fun read(input: InputStream): List<VmInterferenceGroup> {
        return input.use { mapper.readValue(input) }
    }

    private data class GroupMixin(
        @JsonProperty("minServerLoad")
        val targetLoad: Double,
        @JsonProperty("performanceScore")
        val score: Double,
        @JsonProperty("vms")
        val members: Set<String>,
    )
}
