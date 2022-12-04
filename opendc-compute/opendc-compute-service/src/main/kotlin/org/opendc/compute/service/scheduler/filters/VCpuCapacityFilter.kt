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

package org.opendc.compute.service.scheduler.filters

import org.opendc.compute.api.Server
import org.opendc.compute.service.HostView

/**
 * A [HostFilter] that filters hosts based on the vCPU speed requirements of a [Server] and the available
 * capacity on the host.
 */
public class VCpuCapacityFilter : HostFilter {
    override fun test(host: HostView, server: Server): Boolean {
        val requiredCapacity = server.flavor.meta["cpu-capacity"] as? Double
        val hostModel = host.host.model
        val availableCapacity = hostModel.cpuCapacity / hostModel.cpuCount

        return requiredCapacity == null || availableCapacity >= (requiredCapacity / server.flavor.cpuCount)
    }
}
