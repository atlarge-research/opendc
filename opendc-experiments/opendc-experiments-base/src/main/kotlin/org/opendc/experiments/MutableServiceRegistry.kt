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

package org.opendc.experiments

/**
 * A mutable [ServiceRegistry].
 */
public interface MutableServiceRegistry : ServiceRegistry {
    /**
     * Register [service] for the specified [name] in this registry.
     *
     * @param name The name of the service to register, which should follow the rules for domain names as defined by
     *             DNS.
     * @param type The interface provided by the service.
     * @param service A reference to the actual implementation of the service.
     */
    public fun <T : Any> register(name: String, type: Class<T>, service: T)

    /**
     * Remove the service with [name] and [type] from this registry.
     *
     * @param name The name of the service to remove, which should follow the rules for domain names as defined by DNS.
     * @param type The type of the service to remove.
     */
    public fun remove(name: String, type: Class<*>)

    /**
     * Remove all services registered with [name].
     *
     * @param name The name of the services to remove, which should follow the rules for domain names as defined by DNS.
     */
    public fun remove(name: String)

    /**
     * Create a copy of the registry.
     */
    public override fun clone(): MutableServiceRegistry
}
