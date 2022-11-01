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

import org.opendc.compute.api.Image
import java.util.UUID

/**
 * An [Image] implementation that is passed to clients but delegates its implementation to another class.
 */
internal class ClientImage(private val delegate: Image) : Image {
    override val uid: UUID = delegate.uid

    override var name: String = delegate.name
        private set

    override var labels: Map<String, String> = delegate.labels.toMap()
        private set

    override var meta: Map<String, Any> = delegate.meta.toMap()
        private set

    override fun delete() {
        delegate.delete()
        reload()
    }

    override fun reload() {
        delegate.reload()

        name = delegate.name
        labels = delegate.labels
        meta = delegate.meta
    }

    override fun equals(other: Any?): Boolean = other is Image && other.uid == uid

    override fun hashCode(): Int = uid.hashCode()

    override fun toString(): String = "Image[uid=$uid,name=$name]"
}
