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

package org.opendc.harness.engine.internal

import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.api.Parameter
import org.opendc.harness.api.Scenario

/**
 * Internal implementation of a [Scenario].
 */
internal data class ScenarioImpl(
    override val id: Int,
    override val experiment: ExperimentDefinition,
    val parameters: Map<Parameter<*>, Any?>
) : Scenario {

    override fun <T> get(param: Parameter<T>): T {
        if (!parameters.containsKey(param)) {
            throw IllegalArgumentException("Unknown parameter for this scenario.")
        }

        // This cast should always succeed
        @Suppress("UNCHECKED_CAST")
        return parameters[param] as T
    }

    override fun toString(): String = "Scenario"
}
