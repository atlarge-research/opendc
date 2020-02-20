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
 * An immutable map containing the tags of some resource.
 */
public interface TagContainer {
    /**
     * The keys in this container.
     */
    public val keys: Collection<TagKey<*>>

    /**
     * Determine if this container contains a tag with the specified [TagKey].
     *
     * @param key The key of the tag to check for.
     * @return `true` if the tag is in the container, `false` otherwise.
     */
    public operator fun contains(key: TagKey<*>): Boolean

    /**
     * Obtain the tag with the specified [TagKey].
     *
     * @param key The key of the tag to obtain.
     * @return The value of the tag.
     * @throws IllegalArgumentException if the tag does not exists in the container.
     */
    public operator fun <T : Any> get(key: TagKey<T>): T

    /**
     * Return the result of associating the specified [value] with the specified [key] in this container.
     */
    public fun <T : Any> put(key: TagKey<T>, value: T): TagContainer

    /**
     * Return the result of removing the specified [key] and its corresponding value from this container.
     */
    public fun remove(key: TagKey<*>): TagContainer
}
