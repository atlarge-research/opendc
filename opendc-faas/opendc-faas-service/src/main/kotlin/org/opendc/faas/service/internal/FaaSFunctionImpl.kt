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

package org.opendc.faas.service.internal

import org.opendc.faas.api.FaaSFunction
import org.opendc.faas.service.FunctionObject
import java.util.UUID

/**
 * A [FaaSFunction] implementation that is passed to clients.
 */
internal class FaaSFunctionImpl(
    private val service: FaaSServiceImpl,
    private val state: FunctionObject
) : FaaSFunction {
    override val uid: UUID = state.uid

    override var name: String = state.name
        private set

    override var memorySize: Long = state.memorySize
        private set

    override var labels: Map<String, String> = state.labels.toMap()
        private set

    override var meta: Map<String, Any> = state.meta.toMap()
        private set

    override suspend fun delete() {
        service.delete(state)
    }

    override suspend fun invoke() {
        service.invoke(state)
    }

    override suspend fun refresh() {
        name = state.name
        memorySize = state.memorySize
        labels = state.labels
        meta = state.meta
    }

    override fun equals(other: Any?): Boolean = other is FaaSFunctionImpl && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()

    override fun toString(): String = "FaaSFunction[uid=$uid,name=$name]"
}
