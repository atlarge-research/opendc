package org.opendc.compute.simulator.failure.models

import kotlinx.coroutines.delay
import org.apache.commons.math3.distribution.RealDistribution
import org.opendc.compute.service.ComputeService
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong


/**
 * Sample based failure model
 *
 * @property context
 * @property clock
 * @property service
 * @property random
 * @property iatSampler A distribution from which the time until the next fault is sampled in ms
 * @property durationSampler A distribution from which the duration of a fault is sampled in s
 * @property nohSampler A distribution from which the number of hosts that fault is sampled.
 */
public class SampleBasedFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: RandomGenerator,

    private val iatSampler: RealDistribution,
    private val durationSampler: RealDistribution,
    private val nohSampler: RealDistribution
    ): FailureModelNew(context, clock, service, random) {
    override suspend fun runInjector() {
        while(true) {
            val d = (iatSampler.sample() * 3.6e6).roundToLong()

            // Handle long overflow
            if (clock.millis() + d <= 0) {
                return
            }

            delay(d)

            val victims = victimSelector.select(hosts, nohSampler.sample())

            val faultDuration = (durationSampler.sample() * 3.6e6).toLong()
            fault.apply(victims, faultDuration)

            break
        }
    }
}
