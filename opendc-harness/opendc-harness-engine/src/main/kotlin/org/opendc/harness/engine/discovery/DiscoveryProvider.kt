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

package org.opendc.harness.engine.discovery

import org.opendc.harness.engine.internal.CompositeDiscovery
import java.util.*

/**
 * A provider interface for the [Discovery] component.
 */
public interface DiscoveryProvider {
    /**
     * A unique identifier for this discovery implementation.
     *
     * Each discovery implementation must provide a unique ID, so that they can be selected by the user.
     * When in doubt, you may use the fully qualified name of your custom [Discovery] implementation class.
     */
    public val id: String

    /**
     * Factory method for creating a new [Discovery] instance.
     */
    public fun create(): Discovery

    public companion object {
        /**
         * The available [DiscoveryProvider]s.
         */
        private val providers by lazy { ServiceLoader.load(DiscoveryProvider::class.java) }

        /**
         * Obtain the [DiscoveryProvider] with the specified [id] or return `null`.
         */
        public fun findById(id: String): DiscoveryProvider? {
            return providers.find { it.id == id }
        }

        /**
         * Obtain a composite [Discovery] that combines the results of all available providers.
         */
        public fun createComposite(): Discovery {
            return CompositeDiscovery(providers)
        }
    }
}
