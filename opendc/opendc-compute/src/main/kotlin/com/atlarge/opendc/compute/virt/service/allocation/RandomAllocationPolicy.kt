/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.core.image.VmImage
import com.atlarge.opendc.compute.virt.service.HypervisorView
import com.atlarge.opendc.compute.virt.service.SimpleVirtProvisioningService
import kotlin.random.Random

/**
 * An [AllocationPolicy] that select a random node on which the server fits.
 */
public class RandomAllocationPolicy(val random: Random = Random(0)) : AllocationPolicy {
    @OptIn(ExperimentalStdlibApi::class)
    override fun invoke(): AllocationPolicy.Logic = object : AllocationPolicy.Logic {
        override fun select(
            hypervisors: Set<HypervisorView>,
            image: SimpleVirtProvisioningService.ImageView
        ): HypervisorView? {
            return hypervisors.asIterable()
                .filter { hv ->
                    val fitsMemory = hv.availableMemory >= (image.image as VmImage).requiredMemory
                    val fitsCpu = hv.server.flavor.cpuCount >= image.flavor.cpuCount
                    fitsMemory && fitsCpu
                }
                .randomOrNull(random)
        }
    }
}
