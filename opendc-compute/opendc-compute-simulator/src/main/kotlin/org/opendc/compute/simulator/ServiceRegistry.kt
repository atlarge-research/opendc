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

package org.opendc.compute.simulator

/**
 * Implementation of the [ServiceRegistry] interface.
 */
public class ServiceRegistry(private val registry: MutableMap<String, MutableMap<Class<*>, Any>> = mutableMapOf()) {
    public fun <T : Any> resolve(
        name: String,
        type: Class<T>,
    ): T? {
        val servicesForName = registry[name] ?: return null

        val service = servicesForName[type]

        if (service == null) {
            throw IllegalStateException("Service $type not registered for name $name")
        }

        try {
            @Suppress("UNCHECKED_CAST")
            return service as T?
        } catch (e: ClassCastException) {
            throw IllegalStateException("Service $type registered for name $name is not of the given type")
        }
    }

    public fun <T : Any> hasService(
        name: String,
        type: Class<T>,
    ): Boolean {
        val servicesForName = registry[name] ?: return false

        servicesForName[type] ?: return false

        return true
    }

    public fun <T : Any> register(
        name: String,
        type: Class<T>,
        service: T,
    ) {
        val services = registry.computeIfAbsent(name) { mutableMapOf() }

        if (type in services) {
            throw IllegalStateException("Duplicate service $type registered for name $name")
        }

        services[type] = service
    }

    public fun remove(
        name: String,
        type: Class<*>,
    ) {
        val services = registry[name] ?: return
        services.remove(type)
    }

    public fun remove(name: String) {
        registry.remove(name)
    }

    public fun clone(): ServiceRegistry {
        val res = mutableMapOf<String, MutableMap<Class<*>, Any>>()
        registry.mapValuesTo(res) { (_, v) -> v.toMutableMap() }
        return ServiceRegistry(res)
    }

    override fun toString(): String {
        val entries = registry.map { "${it.key}=${it.value}" }.joinToString()
        return "ServiceRegistry{$entries}"
    }
}
