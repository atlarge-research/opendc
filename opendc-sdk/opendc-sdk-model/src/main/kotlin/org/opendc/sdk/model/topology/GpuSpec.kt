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
import org.opendc.common.units.DataRate
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency

/**
 * A GPU specification for a host.
 *
 * @property coreCount Number of cores per GPU package.
 * @property coreSpeed Clock speed of a single core.
 * @property count Number of identical GPU packages on the host.
 * @property memory Onboard GPU memory; a negative value denotes "unspecified".
 * @property memoryBandwidth Memory bandwidth; a negative value denotes "unspecified".
 * @property vendor Hardware vendor name.
 * @property modelName Commercial model name.
 * @property architecture Micro-architecture identifier.
 * @property virtualizationOverhead Overhead model applied when the GPU is shared.
 */
@Serializable
public data class GpuSpec(
    public val coreCount: Int,
    public val coreSpeed: Frequency,
    public val count: Int = 1,
    public val memory: DataSize = DataSize.ofMiB(-1),
    public val memoryBandwidth: DataRate = DataRate.ofKibps(-1),
    public val vendor: String = "unknown",
    public val modelName: String = "unknown",
    public val architecture: String = "unknown",
    public val virtualizationOverhead: VirtualizationOverheadSpec = NoVirtualizationOverheadSpec,
)
