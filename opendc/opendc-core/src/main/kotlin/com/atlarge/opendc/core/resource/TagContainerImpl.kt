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

package com.atlarge.opendc.core.resource

/**
 * Default implementation of [TagContainer] interface.
 *
 * @property map The internal map object to hold the tags.
 */
public class TagContainerImpl private constructor(private val map: Map<TagKey<*>, Any>) : TagContainer {
    /**
     * Construct an empty [TagContainerImpl].
     */
    public constructor() : this(emptyMap())

    override val keys: Collection<TagKey<*>>
        get() = map.keys

    override fun contains(key: TagKey<*>): Boolean = map.contains(key)

    override fun <T : Any> get(key: TagKey<T>): T {
        @Suppress("UNCHECKED_CAST")
        return map.getValue(key) as T
    }

    override fun <T : Any> put(key: TagKey<T>, value: T): TagContainer = TagContainerImpl(map.plus(key to value))

    override fun remove(key: TagKey<*>): TagContainer = TagContainerImpl(map.minus(key))
}
