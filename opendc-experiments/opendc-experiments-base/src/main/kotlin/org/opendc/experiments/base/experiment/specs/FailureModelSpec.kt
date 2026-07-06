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

package org.opendc.experiments.base.experiment.specs
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
import org.opendc.compute.simulator.service.ComputeService
import java.io.File
import java.time.InstantSource
import kotlin.coroutines.CoroutineContext

/**
 * Specification of the failure model that is injected during a simulation.
 *
 * A failure model is selected in a JSON file by adding its [SerialName] as the `type`. The following
 * implementations are available:
 * - [NoFailureModel] (`"no"`): no failures are injected.
 * - [TraceBasedFailureModelSpec] (`"trace-based"`): failures are replayed from a failure trace.
 * - [PrefabFailureModelSpec] (`"prefab"`): a failure model that is already present in OpenDC.
 * - [CustomFailureModelSpec] (`"custom"`): a failure model defined by three distributions to sample from.
 *
 * @property name Human-readable name of the failure model.
 */

@Serializable
public sealed interface FailureModelSpec {
    public var name: String

    /**
     * Validate the constraints of this failure model specification.
     *
     * The default implementation performs no checks; implementations with constraints override it.
     * When any constraint is violated all violations are collected and reported together by throwing
     * an [InvalidFailureModelException].
     *
     * @throws InvalidFailureModelException if one or more constraints are violated.
     */
    public fun validate() {}
}

/**
 * The No failure model does not inject failures.
 *
 * This is only used in an experiment that requires simulation with and without failures.
 */
@Serializable
@SerialName("no")
public data class NoFailureModel(
    override var name: String = "no failure model",
) : FailureModelSpec

/**
 * A failure model spec for failure models based on a failure trace.
 *
 * @property pathToFile Path to the parquet file that contains the failure trace.
 * @property startPoint Relative point in the trace at which replaying starts, as a fraction in `[0.0, 1.0)`.
 * Default is 0.0, or the start of the trace.
 * @property repeat Whether the trace is replayed from the beginning once its end is reached. Default is true.
 */
@Serializable
@SerialName("trace-based")
public data class TraceBasedFailureModelSpec(
    public val pathToFile: String,
    public val startPoint: Double = 0.0,
    public val repeat: Boolean = true,
) : FailureModelSpec {
    override var name: String = File(pathToFile).nameWithoutExtension

    override fun validate() {
        val errors =
            buildList {
                if (!File(pathToFile).exists()) {
                    add("Path to file $pathToFile does not exist")
                }
                if (startPoint >= 1.0) {
                    add("Starting point must be smaller than 1.0 (currently startPoint=$startPoint)")
                }
                if (startPoint < 0.0) {
                    add("Starting point must be equal or larger than 0.0 (currently startPoint=$startPoint)")
                }
            }

        if (errors.isNotEmpty()) {
            throw InvalidFailureModelException(errors)
        }
    }
}

/**
 * A specification for a failure model that is already present in OpenDC.
 * See [FailurePrefab] for the available prefabs.
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

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Custom Failure Model
// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    override fun validate() {
        val errors =
            buildList {
                for (sampler in listOf(iatSampler, durationSampler, nohSampler)) {
                    try {
                        sampler.validate()
                    } catch (e: InvalidDistributionException) {
                        addAll(e.errors)
                    }
                }
            }

        if (errors.isNotEmpty()) {
            throw InvalidFailureModelException(errors)
        }
    }
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
public sealed interface DistributionSpec {
    /**
     * Validate the constraints of this distribution specification.
     *
     * The default implementation performs no checks; implementations with constraints override it.
     * The first violated constraint is reported by throwing an [InvalidDistributionException].
     *
     * @throws InvalidDistributionException if one of the constraints is violated.
     */
    public fun validate() {}
}

/**
 * A specification for a constant distribution that always returns the same value.
 *
 * @property value THe constant value that is returned by the distribution. Must be greater than 0.0.
 */
@Serializable
@SerialName("constant")
public data class ConstantDistributionSpec(
    public val value: Double,
) : DistributionSpec {
    override fun validate() {
        if (value <= 0.0) {
            throw InvalidDistributionException(
                listOf("Value must be greater than 0.0 (currently value=$value)"),
            )
        }
    }
}

/**
 * Specification of the exponential distribution.
 * Distribution is sampled using math3.distributions package.
 * https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/distribution/ExponentialDistribution.html
 *
 * @property mean mean value of the distribution
 */
@Serializable
@SerialName("exponential")
public data class ExponentialDistributionSpec(
    public val mean: Double,
) : DistributionSpec

/**
 * Specification of the gamma distribution.
 * Distribution is sampled using math3.distributions package.
 * https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/distribution/GammaDistribution.html
 *
 * @property shape shape parameter of the distribution
 * @property scale scale parameter of the distribution
 */
@Serializable
@SerialName("gamma")
public data class GammaDistributionSpec(
    public val shape: Double,
    public val scale: Double,
) : DistributionSpec

/**
 * Specification of the log-normal distribution.
 * Distribution is sampled using math3.distributions package.
 * https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/distribution/LogNormalDistribution.html
 *
 * @property shape shape parameter of the distribution
 * @property scale scale parameter of the distribution
 */
@Serializable
@SerialName("log-normal")
public data class LogNormalDistributionSpec(
    public val scale: Double,
    public val shape: Double,
) : DistributionSpec

/**
 * Specification of the log-normal distribution.
 * Distribution is sampled using math3.distributions package.
 * https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/distribution/NormalDistribution.html
 *
 * @property mean shape parameter of the distribution
 * @property std scale parameter of the distribution
 */
@Serializable
@SerialName("normal")
public data class NormalDistributionSpec(
    public val mean: Double,
    public val std: Double,
) : DistributionSpec

/**
 * Specification of the pareto distribution.
 * Distribution is sampled using math3.distributions package.
 * https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/distribution/ParetoDistribution.html
 *
 * @property shape shape parameter of the distribution
 * @property scale scale parameter of the distribution
 */
@Serializable
@SerialName("pareto")
public data class ParetoDistributionSpec(
    public val scale: Double,
    public val shape: Double,
) : DistributionSpec

/**
 * Specification of the Uniform distribution.
 * Distribution is sampled using math3.distributions package.
 * https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/distribution/UniformRealDistribution.html
 *
 * @property upper The upper bound (exclusive) of the distribution
 * @property lower The lower bound (inclusive) of the distribution
 */
@Serializable
@SerialName("uniform")
public data class UniformDistributionSpec(
    public val upper: Double,
    public val lower: Double,
) : DistributionSpec {
    override fun validate() {
        if (upper <= lower) {
            throw InvalidDistributionException(
                listOf("Upper bound must be greater than the lower bound (currently upper=$upper, lower=$lower)"),
            )
        }
    }
}

/**
 * Specification of the Weibull distribution.
 * Distribution is sampled using math3.distributions package.
 * https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/distribution/WeibullDistribution.html
 *
 * @property alpha The alpha value of the distribution. Must be greater than 0.0
 * @property beta The beta value of the distribution. Must be greater than 0.0
 */
@Serializable
@SerialName("weibull")
public data class WeibullDistributionSpec(
    public val alpha: Double,
    public val beta: Double,
) : DistributionSpec {
    override fun validate() {
        val errors =
            buildList {
                if (alpha <= 0.0) {
                    add("Alpha must be greater than 0.0 (currently alpha=$alpha)")
                }
                if (beta <= 0.0) {
                    add("Beta must be greater than 0.0 (currently beta=$beta)")
                }
            }

        if (errors.isNotEmpty()) {
            throw InvalidDistributionException(errors)
        }
    }
}

/**
 * Exception thrown when a [FailureModelSpec] violates one or more of its constraints.
 *
 * @property errors The human-readable descriptions of every violated constraint.
 */
public class InvalidFailureModelException(
    public val errors: List<String>,
) : IllegalArgumentException(
        "Invalid failure model specification:\n" + errors.joinToString("\n") { "  - $it" },
    )

/**
 * Exception thrown when a [DistributionSpec] violates one of its constraints.
 *
 * Unlike a plain [IllegalArgumentException], [message] is non-null, so callers that catch this
 * specific type can use it directly without a null fallback.
 */
public class InvalidDistributionException(
    public val errors: List<String>,
) : IllegalArgumentException(
        "Invalid distribution model specification:\n" + errors.joinToString("\n") { "  - $it" },
    )

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
    return TraceBasedFailureModel(context, clock, service, random, failureModel.pathToFile, failureModel.startPoint, failureModel.repeat)
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
