package org.opendc.compute.simulator.failure.models

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opendc.compute.service.ComputeService
import java.io.File
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

public data class Failure(
    val failureStart: Long,
    val failureDuration: Long,
    val failureIntensity: Double,
) {
    init {
        require(failureStart >= 0.0) {"A failure cannot start at a negative time"}
        require(failureDuration >= 0.0) {"A failure can not have a duration of 0 or less"}
        require(failureIntensity >= 0.0) { "carbon intensity cannot be negative" }
    }
}

public class TraceBasedFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: RandomGenerator,
    pathToTrace: String
    ) : FailureModel(context, clock, service, random){

    private val failureList = FailureTraceLoader().get(File(pathToTrace)).iterator()

    override suspend fun runInjector() {
        while(failureList.hasNext()) {
            val failure = failureList.next()

            delay(failure.failureStart - clock.millis())

            val victims = victimSelector.select(hosts, failure.failureIntensity)
            scope.launch {
                fault.apply(victims, failure.failureDuration)
            }
        }
    }
}
