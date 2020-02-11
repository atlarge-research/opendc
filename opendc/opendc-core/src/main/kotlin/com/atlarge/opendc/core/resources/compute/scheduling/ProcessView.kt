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

package com.atlarge.opendc.core.resources.compute.scheduling

import com.atlarge.odcsim.SendRef
import com.atlarge.opendc.core.resources.compute.MachineEvent
import com.atlarge.opendc.core.workload.application.Application
import com.atlarge.opendc.core.workload.application.Pid
import com.atlarge.opendc.core.workload.application.ProcessMessage

/**
 * A process represents a application instance running on a particular machine from the machine scheduler's point of
 * view.
 *
 * @property application The application this is an instance of.
 * @property broker The broker of the process, which is informed about its progress.
 * @property pid The reference to the application instance.
 * @property state The state of the process.
 */
data class ProcessView(
    val application: Application,
    val broker: SendRef<MachineEvent>,
    val pid: Pid,
    var state: ProcessState = ProcessState.CREATED
) {
    /**
     * The slice of processing elements allocated for the process. Available as soon as the state
     * becomes [ProcessState.RUNNING]
     */
    lateinit var allocation: ProcessMessage.Allocation
}
