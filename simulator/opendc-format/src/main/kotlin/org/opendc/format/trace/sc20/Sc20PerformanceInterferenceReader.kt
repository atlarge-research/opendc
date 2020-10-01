/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.format.trace.sc20

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opendc.compute.core.workload.PerformanceInterferenceModel
import org.opendc.compute.core.workload.PerformanceInterferenceModelItem
import org.opendc.format.trace.PerformanceInterferenceModelReader
import java.io.InputStream
import java.util.*
import kotlin.random.Random

/**
 * A parser for the JSON performance interference setup files used for the SC20 paper.
 *
 * @param input The input stream to read from.
 * @param mapper The Jackson object mapper to use.
 */
public class Sc20PerformanceInterferenceReader(input: InputStream, mapper: ObjectMapper = jacksonObjectMapper()) :
    PerformanceInterferenceModelReader {
    /**
     * The computed value from the file.
     */
    private val items: Map<String, TreeSet<PerformanceInterferenceModelItem>>

    init {
        val entries: List<PerformanceInterferenceEntry> = mapper.readValue(input)
        val res = mutableMapOf<String, TreeSet<PerformanceInterferenceModelItem>>()
        for (entry in entries) {
            val item = PerformanceInterferenceModelItem(TreeSet(entry.vms), entry.minServerLoad, entry.performanceScore)
            for (workload in entry.vms) {
                res.computeIfAbsent(workload) { TreeSet() }.add(item)
            }
        }

        items = res
    }

    override fun construct(random: Random): Map<String, PerformanceInterferenceModel> {
        return items.mapValues { PerformanceInterferenceModel(it.value, Random(random.nextInt())) }
    }

    override fun close() {}
}
