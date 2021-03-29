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

package org.opendc.serverless.service.internal

import org.opendc.serverless.api.ServerlessFunction
import java.util.*

/**
 * Internal stateful representation of a [ServerlessFunction].
 */
internal class InternalFunction(
    private val service: ServerlessServiceImpl,
    override val uid: UUID,
    name: String,
    allocatedMemory: Long,
    labels: Map<String, String>,
    meta: Map<String, Any>
) : ServerlessFunction {
    override var name: String = name
        private set

    override var memorySize: Long = allocatedMemory
        private set

    override val labels: MutableMap<String, String> = labels.toMutableMap()

    override val meta: MutableMap<String, Any> = meta.toMutableMap()

    override suspend fun refresh() {
        // No-op: this object is the source-of-truth
    }

    override suspend fun invoke() {
        service.invoke(this)
    }

    override suspend fun delete() {
        service.delete(this)
    }

    override fun equals(other: Any?): Boolean = other is ServerlessFunction && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()
}
