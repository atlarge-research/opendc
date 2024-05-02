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
import org.apache.commons.math3.distribution.LogNormalDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.random.Well19937c
import org.opendc.compute.service.ComputeService
import org.opendc.compute.simulator.failure.models.FailureModel
import org.opendc.compute.simulator.failure.models.SampleBasedFailureModel
import org.opendc.compute.simulator.failure.models.TraceBasedFailureModel
import org.opendc.compute.simulator.failure.prefab.FailurePrefab
import org.opendc.compute.simulator.failure.prefab.getFailureModelPrefab
import java.io.File
import java.time.InstantSource
import kotlin.coroutines.CoroutineContext

@Serializable
public sealed interface FailureModelSpec {
    public var name: String
}

@Serializable
@SerialName("trace-based")
public data class TraceBasedFailureModelSpec(
    public val pathToFile: String
): FailureModelSpec {
    override var name: String = File(pathToFile).nameWithoutExtension
    init {
        require(File(pathToFile).exists()) { "Path to file $pathToFile does not exist" }
    }
}

@Serializable
@SerialName("prefab")
public data class PrefabFailureModelSpec(
    public val prefabName: FailurePrefab
): FailureModelSpec {
    override var name: String = prefabName.toString()
}

@Serializable
@SerialName("custom")
public data class CustomFailureModelSpec(
    public val iatSampler: DistributionSpec,
    public val durationSampler: DistributionSpec,
    public val nohSampler: DistributionSpec,
): FailureModelSpec {
    override var name: String = "custom"
}

@Serializable
public sealed interface DistributionSpec

@Serializable
@SerialName("log-normal")
public data class LogNormalDistributionSpec(
    public val scale: Double,
    public val size: Double,
): DistributionSpec

@Serializable
@SerialName("constant")
public data class ConstantDistributionSpec(
    public val value: Double
): DistributionSpec




public fun getFailureModel(context: CoroutineContext,
                           clock: InstantSource,
                           service: ComputeService,
                           random: java.util.random.RandomGenerator,
                           failureModel: PrefabFailureModelSpec): FailureModel {
    return getFailureModelPrefab(context, clock, service, random, failureModel.prefabName)
}

public fun getFailureModel(context: CoroutineContext,
                           clock: InstantSource,
                           service: ComputeService,
                           random: java.util.random.RandomGenerator,
                           failureModel: CustomFailureModelSpec): FailureModel {
    val rng: org.apache.commons.math3.random.RandomGenerator = Well19937c(random.nextLong())

    val iatSampler = getSampler(rng, failureModel.iatSampler)
    val durationSampler = getSampler(rng, failureModel.durationSampler)
    val nohSampler = getSampler(rng, failureModel.nohSampler)

    return SampleBasedFailureModel(context, clock, service, random, iatSampler, durationSampler, nohSampler)
}

public fun getSampler(rng: org.apache.commons.math3.random.RandomGenerator, distributionSpec: DistributionSpec): RealDistribution {
    return when (distributionSpec) {
        is LogNormalDistributionSpec -> LogNormalDistribution(rng, distributionSpec.scale, distributionSpec.size)
        is ConstantDistributionSpec -> ConstantRealDistribution(distributionSpec.value)
        else -> error("The given distributionSpec: $distributionSpec is not a valid DistributionSpec")
    }
}

public fun getFailureModel(context: CoroutineContext,
                           clock: InstantSource,
                           service: ComputeService,
                           random: java.util.random.RandomGenerator,
                           failureModel: TraceBasedFailureModelSpec): FailureModel {

    return TraceBasedFailureModel(context, clock, service, random, failureModel.pathToFile)
}

public fun getFailureModel(context: CoroutineContext,
                           clock: InstantSource,
                           service: ComputeService,
                           random: java.util.random.RandomGenerator,
                           failureModel: FailureModelSpec?): FailureModel? {

    return when(failureModel) {
        is PrefabFailureModelSpec -> getFailureModel(context, clock, service, random, failureModel)
        is CustomFailureModelSpec -> getFailureModel(context, clock, service, random, failureModel)
        is TraceBasedFailureModelSpec -> getFailureModel(context, clock, service, random, failureModel)
        else -> null
    }
}
