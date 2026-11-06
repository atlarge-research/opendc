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

package org.opendc.sdk.runner.factory

import org.opendc.sdk.model.carbon.CarbonModelSpec
import org.opendc.sdk.model.carbon.InlineTraceBasedCarbonSpec
import org.opendc.sdk.model.carbon.TraceBasedCarbonSpec
import org.opendc.sdk.model.carbon.loader.CarbonTraceLoader
import org.opendc.sdk.model.carbon.loader.toCarbonFragments
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.simulator.compute.carbon.CarbonFragment
import java.nio.file.Path

/**
 * Converts an SDK [CarbonModelSpec] into the carbon fragments of the engine, ordered by start time. Trace references
 * are materialized through [resolve] and loaded with [CarbonTraceLoader].
 */
internal fun CarbonModelSpec.toFragments(resolve: (ResourceReference) -> Path): List<CarbonFragment> =
    when (this) {
        is TraceBasedCarbonSpec -> CarbonTraceLoader(resolve(source).toFile()).load()
        is InlineTraceBasedCarbonSpec -> intensities.toCarbonFragments()
    }
