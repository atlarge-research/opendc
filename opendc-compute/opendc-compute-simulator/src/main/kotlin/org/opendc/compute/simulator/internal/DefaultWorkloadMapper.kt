/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.compute.simulator.internal

import org.opendc.compute.api.Server
import org.opendc.compute.simulator.SimMetaWorkloadMapper
import org.opendc.compute.simulator.SimWorkloadMapper
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.compute.workload.SimWorkloads
import java.time.Duration

/**
 * A [SimWorkloadMapper] to introduces a boot delay of 1 ms. This object exists to retain the old behavior while
 * introducing the possibility of adding custom boot delays.
 */
internal object DefaultWorkloadMapper : SimWorkloadMapper {
    private val delegate = SimMetaWorkloadMapper()

    override fun createWorkload(server: Server): SimWorkload {
        val workload = delegate.createWorkload(server)
        // FIXME: look at connecting this to frontend. Probably not needed since the duration is so small
        val bootWorkload = SimWorkloads.runtime(Duration.ofMillis(1), 0.8, 0L, 0L)
        return SimWorkloads.chain(bootWorkload, workload)
    }
}
