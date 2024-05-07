/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.experiments.base.scenario.specs

/*
 * Copyright (c) 2024 AtLarge Research
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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.ExponentialDistribution
import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.distribution.LogNormalDistribution
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.ParetoDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.apache.commons.math3.distribution.WeibullDistribution
import org.apache.commons.math3.random.Well19937c
import org.opendc.compute.failure.models.FailureModel
import org.opendc.compute.failure.models.SampleBasedFailureModel
import org.opendc.compute.failure.models.TraceBasedFailureModel
import org.opendc.compute.failure.prefab.FailurePrefab
import org.opendc.compute.failure.prefab.createFailureModelPrefab
import org.opendc.compute.service.ComputeService
import java.io.File
import java.time.InstantSource
import kotlin.coroutines.CoroutineContext

/**
 * Specifications of the different Failure models
 * There are three types of Specs that can be used by using their SerialName as the type.
 *
 * @constructor Create empty Failure model spec
 */

@Serializable
public sealed interface FailureModelSpec {
    public var name: String
}

/**
 * A failure model spec for failure models based on a failure trace.
 *
 * @property pathToFile Path to the parquet file that contains the failure trace
 */
@Serializable
@SerialName("trace-based")
public data class TraceBasedFailureModelSpec(
    public val pathToFile: String,
) : FailureModelSpec {
    override var name: String = File(pathToFile).nameWithoutExtension

    init {
        require(File(pathToFile).exists()) { "Path to file $pathToFile does not exist" }
    }
}

/**
 * A specification for a failure model that is already present in OpenDC.
 *
 * @property prefabName The name of the prefab. It needs to be valid [FailurePrefab]
 */
@Serializable
@SerialName("prefab")
public data class PrefabFailureModelSpec(
    public val prefabName: FailurePrefab,
) : FailureModelSpec {
    override var name: String = prefabName.toString()
}

/**
 * Specification of a custom failure model that is defined by three distributions to sample from.
 * Distributions are defined using a [DistributionSpec].
 *
 * @property iatSampler Sampler for the time between failures defined in hours
 * @property durationSampler Sampler for the time of a failure defined in hours
 * @property nohSampler Sampler for ratio of hosts that fail defined as a double between 0.0 and 1.0
 * @constructor Create empty Custom failure model spec
 */
@Serializable
@SerialName("custom")
public data class CustomFailureModelSpec(
    public val iatSampler: DistributionSpec,
    public val durationSampler: DistributionSpec,
    public val nohSampler: DistributionSpec,
) : FailureModelSpec {
    override var name: String = "custom"
}

/**
 * Specifications of the different Distributions that can used to create a [CustomFailureModelSpec]
 * All [DistributionSpec]s have a different definition based on the variables they need to function.
 * Available [DistributionSpec] are:
 * - [ConstantDistributionSpec]
 * - [ExponentialDistributionSpec]
 * - [GammaDistributionSpec]
 * - [LogNormalDistributionSpec]
 * - [ParetoDistributionSpec]
 * - [UniformDistributionSpec]
 * - [WeibullDistributionSpec]
*/

@Serializable
public sealed interface DistributionSpec

@Serializable
@SerialName("constant")
public data class ConstantDistributionSpec(
    public val value: Double,
) : DistributionSpec {
    init {
        require(value > 0.0) { "Value must be greater than 0.0" }
    }
}

@Serializable
@SerialName("exponential")
public data class ExponentialDistributionSpec(
    public val mean: Double,
) : DistributionSpec

@Serializable
@SerialName("gamma")
public data class GammaDistributionSpec(
    public val shape: Double,
    public val scale: Double,
) : DistributionSpec

@Serializable
@SerialName("log-normal")
public data class LogNormalDistributionSpec(
    public val scale: Double,
    public val shape: Double,
) : DistributionSpec

@Serializable
@SerialName("normal")
public data class NormalDistributionSpec(
    public val mean: Double,
    public val std: Double,
) : DistributionSpec

@Serializable
@SerialName("pareto")
public data class ParetoDistributionSpec(
    public val scale: Double,
    public val shape: Double,
) : DistributionSpec

@Serializable
@SerialName("uniform")
public data class UniformDistributionSpec(
    public val upper: Double,
    public val lower: Double,
) : DistributionSpec {
    init {
        require(upper > lower) { "Upper bound must be greater than the lower bound" }
    }
}

@Serializable
@SerialName("weibull")
public data class WeibullDistributionSpec(
    public val alpha: Double,
    public val beta: Double,
) : DistributionSpec

/**
 * Create a [FailureModel] based on the provided [FailureModelSpec]
 *
 * @param context
 * @param clock
 * @param service
 * @param random
 * @param failureModelSpec
 * @return
 */
public fun createFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: java.util.random.RandomGenerator,
    failureModelSpec: FailureModelSpec?,
): FailureModel? {
    return when (failureModelSpec) {
        is PrefabFailureModelSpec -> createFailureModel(context, clock, service, random, failureModelSpec)
        is CustomFailureModelSpec -> createFailureModel(context, clock, service, random, failureModelSpec)
        is TraceBasedFailureModelSpec -> createFailureModel(context, clock, service, random, failureModelSpec)
        else -> null
    }
}

/**
 * Create [FailureModel] based on the provided [PrefabFailureModelSpec]
 *
 * @param context
 * @param clock
 * @param service
 * @param random
 * @param failureModel
 * @return
 */
public fun createFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: java.util.random.RandomGenerator,
    failureModel: PrefabFailureModelSpec,
): FailureModel {
    return createFailureModelPrefab(context, clock, service, random, failureModel.prefabName)
}

/**
 * Create [FailureModel] based on the provided [TraceBasedFailureModel]
 *
 * @param context
 * @param clock
 * @param service
 * @param random
 * @param failureModel
 * @return
 */
public fun createFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: java.util.random.RandomGenerator,
    failureModel: TraceBasedFailureModelSpec,
): FailureModel {
    return TraceBasedFailureModel(context, clock, service, random, failureModel.pathToFile)
}

/**
 * Create [FailureModel] based on the provided [CustomFailureModelSpec]
 *
 * @param context
 * @param clock
 * @param service
 * @param random
 * @param failureModel
 * @return
 */
public fun createFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: java.util.random.RandomGenerator,
    failureModel: CustomFailureModelSpec,
): FailureModel {
    val rng: org.apache.commons.math3.random.RandomGenerator = Well19937c(random.nextLong())

    val iatSampler = createSampler(rng, failureModel.iatSampler)
    val durationSampler = createSampler(rng, failureModel.durationSampler)
    val nohSampler = createSampler(rng, failureModel.nohSampler)

    return SampleBasedFailureModel(context, clock, service, random, iatSampler, durationSampler, nohSampler)
}

/**
 * Create a [RealDistribution] to sample from based on the provided [DistributionSpec]
 *
 * @param rng
 * @param distributionSpec
 * @return
 */
public fun createSampler(
    rng: org.apache.commons.math3.random.RandomGenerator,
    distributionSpec: DistributionSpec,
): RealDistribution {
    return when (distributionSpec) {
        is ConstantDistributionSpec -> ConstantRealDistribution(distributionSpec.value)
        is ExponentialDistributionSpec -> ExponentialDistribution(rng, distributionSpec.mean)
        is GammaDistributionSpec -> GammaDistribution(rng, distributionSpec.shape, distributionSpec.scale)
        is LogNormalDistributionSpec -> LogNormalDistribution(rng, distributionSpec.scale, distributionSpec.shape)
        is NormalDistributionSpec -> NormalDistribution(rng, distributionSpec.mean, distributionSpec.std)
        is ParetoDistributionSpec -> ParetoDistribution(rng, distributionSpec.scale, distributionSpec.shape)
        is UniformDistributionSpec -> UniformRealDistribution(rng, distributionSpec.lower, distributionSpec.upper)
        is WeibullDistributionSpec -> WeibullDistribution(rng, distributionSpec.alpha, distributionSpec.beta)
    }
}
