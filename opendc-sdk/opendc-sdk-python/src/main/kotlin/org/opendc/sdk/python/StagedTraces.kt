/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.sdk.python

import org.opendc.sdk.model.resource.ProvisionedResource
import org.opendc.sdk.model.resource.ResourceProvisioner
import org.opendc.sdk.model.resource.ResourceReference
import java.nio.file.Path

/** Provisions each trace Python passed from its absolute path in [locations], by its [key], and any other through [fallback]. */
class StagedTraces(
    private val locations: Map<String, String>,
    private val fallback: ResourceProvisioner,
) : ResourceProvisioner {
    override fun provision(reference: ResourceReference): ProvisionedResource {
        val location = locations[reference.key()] ?: return provisionUnpassed(reference)

        return object : ProvisionedResource {
            override val path: Path = Path.of(location)

            override fun close() {}
        }
    }

    private fun provisionUnpassed(reference: ResourceReference): ProvisionedResource =
        try {
            fallback.provision(reference)
        } catch (failure: IllegalArgumentException) {
            throw unresolved(failure)
        } catch (failure: UnsupportedOperationException) {
            throw unresolved(failure)
        }

    /** [failure] of the fallback, naming the traces that were passed as well. */
    private fun unresolved(failure: RuntimeException) =
        IllegalArgumentException("${failure.message}; the traces passed are ${locations.keys.sorted()}", failure)
}
