/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.model.odc.integration.jpa.schema

import com.atlarge.opendc.simulator.Instant
import com.atlarge.opendc.simulator.instrumentation.interpolate
import com.atlarge.opendc.simulator.instrumentation.lerp
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consume
import javax.persistence.Entity
import kotlin.coroutines.experimental.CoroutineContext

/**
 * The state of a [Machine].
 *
 * @property id The unique identifier of the state.
 * @property machine The machine of the state.
 * @property task The task the machine has been assigned.
 * @property experiment The experiment the machine is running in.
 * @property time The current moment in time.
 * @property temperature The temperature of the machine.
 * @property memoryUsage The memory usage of the machine.
 * @property load The load of the machine.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
@Entity
data class MachineState(
    val id: Int,
    val machine: Machine,
    val task: Task?,
    val experiment: Experiment,
    val time: Instant,
    val temperature: Double,
    val memoryUsage: Int,
    val load: Double
)

/**
 * Linearly interpolate [n] amount of elements between every two occurrences of task progress measurements represented
 * as [MachineState] instances passing through the channel.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 *
 * @param context The context of the coroutine.
 * @param n The amount of elements to interpolate between the actual elements in the channel.
 */
fun ReceiveChannel<MachineState>.interpolate(n: Int, context: CoroutineContext = Unconfined): ReceiveChannel<MachineState> =
    interpolate(n, context) { f, a, b ->
        a.copy(
            id = 0,
            time = lerp(a.time, b.time, f),
            temperature = lerp(a.temperature, b.temperature, f),
            memoryUsage = lerp(a.memoryUsage, b.memoryUsage, f),
            load = lerp(a.load, b.load, f)
        )
    }
