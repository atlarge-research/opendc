/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.format.trace.sc20

import com.atlarge.opendc.compute.core.workload.PerformanceInterferenceModel
import com.atlarge.opendc.compute.core.workload.PerformanceInterferenceModelItem
import com.atlarge.opendc.format.trace.PerformanceInterferenceModelReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.util.TreeSet

/**
 * A parser for the JSON performance interference setup files used for the SC20 paper.
 *
 * @param input The input stream to read from.
 * @param mapper The Jackson object mapper to use.
 */
class Sc20PerformanceInterferenceReader(input: InputStream, mapper: ObjectMapper = jacksonObjectMapper()) :
    PerformanceInterferenceModelReader {
    /**
     * The environment that was read from the file.
     */
    private val performanceInterferenceModel: List<PerformanceInterferenceEntry> = mapper.readValue(input)

    override fun construct(): PerformanceInterferenceModel {
        return PerformanceInterferenceModel(
            performanceInterferenceModel.map { item ->
                PerformanceInterferenceModelItem(
                    TreeSet(item.vms),
                    item.minServerLoad,
                    item.performanceScore
                )
            }.toSet()
        )
    }

    override fun close() {}
}
