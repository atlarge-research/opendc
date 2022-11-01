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

package org.opendc.compute.service.internal

import org.opendc.compute.api.Flavor
import java.util.UUID

/**
 * Internal stateful representation of a [Flavor].
 */
internal class InternalFlavor(
    private val service: ComputeServiceImpl,
    override val uid: UUID,
    name: String,
    cpuCount: Int,
    memorySize: Long,
    labels: Map<String, String>,
    meta: Map<String, Any>
) : Flavor {
    override var name: String = name
        private set

    override var cpuCount: Int = cpuCount
        private set

    override var memorySize: Long = memorySize
        private set

    override val labels: MutableMap<String, String> = labels.toMutableMap()

    override val meta: MutableMap<String, Any> = meta.toMutableMap()

    override fun reload() {
        // No-op: this object is the source-of-truth
    }

    override fun delete() {
        service.delete(this)
    }

    override fun equals(other: Any?): Boolean = other is Flavor && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()

    override fun toString(): String = "Flavor[uid=$uid,name=$name]"
}
