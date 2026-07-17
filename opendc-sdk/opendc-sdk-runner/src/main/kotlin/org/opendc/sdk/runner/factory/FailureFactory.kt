/*
 * Copyright (c) 2025 AtLarge Research
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

import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.apache.commons.math3.random.Well19937c
import org.opendc.compute.failure.models.SampleBasedFailureModel
import org.opendc.compute.failure.models.TraceBasedFailureModel
import org.opendc.compute.failure.prefab.createFailureModelPrefab
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.sdk.model.failure.ConstantDistributionSpec
import org.opendc.sdk.model.failure.CustomFailureSpec
import org.opendc.sdk.model.failure.DistributionSpec
import org.opendc.sdk.model.failure.GammaDistributionSpec
import org.opendc.sdk.model.failure.LogNormalDistributionSpec
import org.opendc.sdk.model.failure.NoFailureSpec
import org.opendc.sdk.model.failure.NormalDistributionSpec
import org.opendc.sdk.model.failure.ParetoDistributionSpec
import org.opendc.sdk.model.failure.PrefabFailureSpec
import org.opendc.sdk.model.failure.TraceBasedFailureSpec
import org.opendc.sdk.model.failure.UniformDistributionSpec
import org.opendc.sdk.model.failure.WeibullDistributionSpec
import org.opendc.sdk.model.resource.ResourceReference
import java.nio.file.Path
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext
import org.apache.commons.math3.distribution.ExponentialDistribution as CmExponentialDistribution
import org.apache.commons.math3.distribution.GammaDistribution as CmGammaDistribution
import org.apache.commons.math3.distribution.LogNormalDistribution as CmLogNormalDistribution
import org.apache.commons.math3.distribution.NormalDistribution as CmNormalDistribution
import org.apache.commons.math3.distribution.ParetoDistribution as CmParetoDistribution
import org.apache.commons.math3.distribution.WeibullDistribution as CmWeibullDistribution
import org.opendc.compute.failure.models.FailureModel as EngineFailureModel
import org.opendc.compute.failure.prefab.FailurePrefab as EngineFailurePrefab
import org.opendc.sdk.model.failure.ExponentialDistributionSpec as SdkExponentialDistribution
import org.opendc.sdk.model.failure.FailureModelSpec as SdkFailureModel

/**
 * Converts an SDK [SdkFailureModel] into the engine failure model injected during replay, or null
 * when no failures are configured. Trace references are materialized through [resolve].
 */
internal fun SdkFailureModel.toEngine(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: RandomGenerator,
    resolve: (ResourceReference) -> Path,
): EngineFailureModel? =
    when (this) {
        NoFailureSpec -> null
        is TraceBasedFailureSpec -> TraceBasedFailureModel(context, clock, service, random, resolve(source).toString(), startPoint, repeat)
        is PrefabFailureSpec -> createFailureModelPrefab(context, clock, service, random, EngineFailurePrefab.valueOf(prefabName.name))
        is CustomFailureSpec -> {
            val rng = Well19937c(random.nextLong())
            SampleBasedFailureModel(
                context,
                clock,
                service,
                random,
                interArrival.toSampler(rng),
                duration.toSampler(rng),
                hostFraction.toSampler(rng),
            )
        }
    }

private fun DistributionSpec.toSampler(rng: org.apache.commons.math3.random.RandomGenerator): RealDistribution =
    when (this) {
        is ConstantDistributionSpec -> ConstantRealDistribution(value)
        is SdkExponentialDistribution -> CmExponentialDistribution(rng, mean)
        is GammaDistributionSpec -> CmGammaDistribution(rng, shape, scale)
        is LogNormalDistributionSpec -> CmLogNormalDistribution(rng, scale, shape)
        is NormalDistributionSpec -> CmNormalDistribution(rng, mean, std)
        is ParetoDistributionSpec -> CmParetoDistribution(rng, scale, shape)
        is UniformDistributionSpec -> UniformRealDistribution(rng, lower, upper)
        is WeibullDistributionSpec -> CmWeibullDistribution(rng, alpha, beta)
    }
