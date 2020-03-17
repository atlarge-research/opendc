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

package com.atlarge.opendc.core.services

/**
 * An immutable service registry interface.
 */
public interface ServiceRegistry {
    /**
     * The keys in this registry.
     */
    public val keys: Collection<ServiceKey<*>>

    /**
     * Determine if this map contains the service with the specified [ServiceKey].
     *
     * @param key The key of the service to check for.
     * @return `true` if the service is in the map, `false` otherwise.
     */
    public operator fun contains(key: ServiceKey<*>): Boolean

    /**
     * Obtain the service with the specified [ServiceKey].
     *
     * @param key The key of the service to obtain.
     * @return The references to the service.
     * @throws IllegalArgumentException if the key does not exist in the map.
     */
    public operator fun <T : Any> get(key: ServiceKey<T>): T

    /**
     * Return the result of associating the specified [service] with the given [key] in this registry.
     */
    public fun <T : Any> put(key: ServiceKey<T>, service: T): ServiceRegistry
}

/**
 * Construct an empty [ServiceRegistry].
 */
@Suppress("FunctionName")
public fun ServiceRegistry(): ServiceRegistry = ServiceRegistryImpl(emptyMap())
