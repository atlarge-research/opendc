/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.experiments.internal

import org.opendc.experiments.MutableServiceRegistry

/**
 * Implementation of the [MutableServiceRegistry] interface.
 */
internal class ServiceRegistryImpl(private val registry: MutableMap<String, MutableMap<Class<*>, Any>> = mutableMapOf()) :
    MutableServiceRegistry {
    override fun <T : Any> resolve(name: String, type: Class<T>): T? {
        val servicesForName = registry[name] ?: return null

        @Suppress("UNCHECKED_CAST")
        return servicesForName[type] as T?
    }

    override fun <T : Any> register(name: String, type: Class<T>, service: T) {
        val services = registry.computeIfAbsent(name) { mutableMapOf() }

        if (type in services) {
            throw IllegalStateException("Duplicate service $type registered for name $name")
        }

        services[type] = service
    }

    override fun remove(name: String, type: Class<*>) {
        val services = registry[name] ?: return
        services.remove(type)
    }

    override fun remove(name: String) {
        registry.remove(name)
    }

    override fun clone(): MutableServiceRegistry {
        val res = mutableMapOf<String, MutableMap<Class<*>, Any>>()
        registry.mapValuesTo(res) { (_, v) -> v.toMutableMap() }
        return ServiceRegistryImpl(res)
    }

    override fun toString(): String {
        val entries = registry.map { "${it.key}=${it.value}" }.joinToString()
        return "ServiceRegistry{$entries}"
    }
}
