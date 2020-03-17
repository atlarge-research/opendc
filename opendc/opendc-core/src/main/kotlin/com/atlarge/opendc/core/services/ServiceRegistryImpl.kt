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
 * Default implementation of the [ServiceRegistry] interface.
 */
public class ServiceRegistryImpl : ServiceRegistry {
    /**
     * The map containing the registered services.
     */
    private val services: MutableMap<ServiceKey<*>, Any> = mutableMapOf()

    override fun <T : Any> set(key: ServiceKey<T>, service: T) {
        services[key] = service
    }

    override fun contains(key: ServiceKey<*>): Boolean = key in services

    override fun <T : Any> get(key: ServiceKey<T>): T {
        @Suppress("UNCHECKED_CAST")
        return services[key] as T
    }

    override fun toString(): String = services.toString()
}
