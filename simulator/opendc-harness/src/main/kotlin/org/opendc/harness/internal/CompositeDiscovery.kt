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

package org.opendc.harness.internal

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.engine.discovery.Discovery
import org.opendc.harness.engine.discovery.DiscoveryProvider
import org.opendc.harness.engine.discovery.DiscoveryRequest

/**
 * A composite [Discovery] instance that combines the results of multiple delegate instances.
 */
internal class CompositeDiscovery(providers: Iterable<DiscoveryProvider>) : Discovery {
    /**
     * The [Discovery] instances to delegate to.
     */
    private val delegates = providers.map { it.create() }

    @OptIn(FlowPreview::class)
    override fun discover(request: DiscoveryRequest): Flow<ExperimentDefinition> {
        return delegates.asFlow()
            .map { it.discover(request) }
            .flattenMerge(delegates.size)
    }
}
