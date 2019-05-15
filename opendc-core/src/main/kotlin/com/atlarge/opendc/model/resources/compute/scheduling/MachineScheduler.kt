/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.model.resources.compute.scheduling

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.TimerScheduler
import com.atlarge.opendc.model.resources.compute.Machine
import com.atlarge.opendc.model.resources.compute.MachineMessage

/**
 * A factory interface for constructing a [MachineSchedulerLogic].
 */
interface MachineScheduler {
    /**
     * Construct a [MachineSchedulerLogic] in the given [ActorContext].
     *
     * @param machine The machine to create the scheduler for.
     * @param ctx The actor context to construct a scheduler for.
     * @param scheduler The timer scheduler to use.
     */
    operator fun invoke(
        machine: Machine,
        ctx: ActorContext<MachineMessage>,
        scheduler: TimerScheduler<MachineMessage>
    ): MachineSchedulerLogic
}
