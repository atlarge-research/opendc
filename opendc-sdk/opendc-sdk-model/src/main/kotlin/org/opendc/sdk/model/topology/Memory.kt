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
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency

/**
 * A memory (RAM) specification for a host.
 *
 * @property size Total memory capacity.
 * @property speed Memory clock speed; a negative value denotes "unspecified".
 * @property vendor Hardware vendor name.
 * @property modelName Commercial model name.
 * @property architecture Memory technology identifier.
 */
@Serializable
public data class Memory(
    public val size: DataSize,
    public val speed: Frequency = Frequency.ofMHz(-1),
    public val vendor: String = "unknown",
    public val modelName: String = "unknown",
    public val architecture: String = "unknown",
)
