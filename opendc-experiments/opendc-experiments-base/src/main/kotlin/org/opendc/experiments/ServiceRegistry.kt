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
 * A read-only registry of services used during experiments to resolve services.
 *
 * The service registry is similar conceptually to the Domain Name System (DNS), which is a naming system used to
 * identify computers reachable via the Internet. The service registry should be used in a similar fashion.
 */
public interface ServiceRegistry {
    /**
     * Lookup the service with the specified [name] and [type].
     *
     * @param name The name of the service to resolve, which should follow the rules for domain names as defined by DNS.
     * @param type The type of the service to resolve, identified by the interface that is implemented by the service.
     * @return The service with specified [name] and implementing [type] or `null` if it does not exist.
     */
    public fun <T : Any> resolve(name: String, type: Class<T>): T?

    /**
     * Create a copy of the registry.
     */
    public fun clone(): ServiceRegistry
}
