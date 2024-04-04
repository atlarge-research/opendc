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

@file:JvmName("FailureModels")

package org.opendc.compute.simulator.failure.prefab

import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.LogNormalDistribution
import org.apache.commons.math3.random.Well19937c
import org.opendc.compute.service.ComputeService
import org.opendc.compute.simulator.failure.models.SampleBasedFailureModel
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

public enum class FailurePrefab {
    Grid5000,
}

public fun getFailureModelPrefab(context: CoroutineContext,
                                 clock: InstantSource,
                                 service: ComputeService,
                                 random: RandomGenerator,
                                 prefab: FailurePrefab): SampleBasedFailureModel {
    when(prefab) {
        FailurePrefab.Grid5000 -> return createGrid5000(context, clock, service, random)

        else -> error("Unknown failure prefab: $prefab")
    }
}


/**
 * A [FailureModel] based on the GRID'5000 failure trace.
 *
 * This fault injector uses parameters from the GRID'5000 failure trace as described in
 * "A Framework for the Study of Grid Inter-Operation Mechanisms", A. Iosup, 2009.
 */
public fun createGrid5000(context: CoroutineContext,
                          clock: InstantSource,
                          service: ComputeService,
                          random: RandomGenerator): SampleBasedFailureModel {
    val rng = Well19937c(random.nextLong())

    return SampleBasedFailureModel(
        context,
        clock,
        service,
        random,

        LogNormalDistribution(rng, 1.0, 0.1),
        LogNormalDistribution(rng, 1.0, 0.1),
        ConstantRealDistribution(0.5)
    )
}
