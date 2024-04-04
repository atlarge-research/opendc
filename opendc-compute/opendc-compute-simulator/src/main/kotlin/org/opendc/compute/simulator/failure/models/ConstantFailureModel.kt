package org.opendc.compute.simulator.failure.models

import kotlinx.coroutines.delay
import org.opendc.compute.service.ComputeService
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

public class ConstantFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: RandomGenerator,
    private val numberOfVictims: Int,
    private val waitDuration: Long,
    private val faultDuration: Long,
    ): FailureModelNew(context, clock, service, random) {
    override suspend fun runInjector() {
        delay(waitDuration)

        val victims = victimSelector.select(hosts, numberOfVictims)

        fault.apply(victims, faultDuration)
    }
}
