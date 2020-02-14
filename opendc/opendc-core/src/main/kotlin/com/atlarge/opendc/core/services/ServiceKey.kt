/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.core.services

import com.atlarge.opendc.core.Identity
import java.util.UUID

/**
 * An interface for identifying service implementations of the same type (providing the same service).
 *
 * @param T The shape of the messages the service responds to.
 */
interface ServiceKey<T : Any> : Identity

/**
 * Helper class for constructing a [ServiceKey].
 *
 * @property uid The unique identifier of the service.
 * @property name The name of the service.
 */
abstract class AbstractServiceKey<T : Any>(override val uid: UUID, override val name: String) : ServiceKey<T> {
    override fun equals(other: Any?): Boolean = other is ServiceKey<*> && uid == other.uid
    override fun hashCode(): Int = uid.hashCode()
    override fun toString(): String = "ServiceKey[uid=$uid, name=$name]"
}
