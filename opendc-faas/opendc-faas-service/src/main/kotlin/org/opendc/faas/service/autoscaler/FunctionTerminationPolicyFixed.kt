/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.faas.service.autoscaler

import org.opendc.common.Dispatcher
import org.opendc.common.util.TimerScheduler
import org.opendc.faas.service.deployer.FunctionInstance
import org.opendc.faas.service.deployer.FunctionInstanceState
import java.time.Duration

/**
 * A [FunctionTerminationPolicy] that terminates idle function instances after a fixed keep-alive time.
 *
 * @param timeout The idle timeout after which the function instance is terminated.
 */
public class FunctionTerminationPolicyFixed(
    dispatcher: Dispatcher,
    public val timeout: Duration
) : FunctionTerminationPolicy {
    /**
     * The [TimerScheduler] used to schedule the function terminations.
     */
    private val scheduler = TimerScheduler<FunctionInstance>(dispatcher)

    override fun enqueue(instance: FunctionInstance) {
        // Cancel the existing timeout timer
        scheduler.cancel(instance)
    }

    override fun onStateChanged(instance: FunctionInstance, newState: FunctionInstanceState) {
        when (newState) {
            FunctionInstanceState.Active -> scheduler.cancel(instance)
            FunctionInstanceState.Idle -> schedule(instance)
            else -> {}
        }
    }

    /**
     * Schedule termination for the specified [instance].
     */
    private fun schedule(instance: FunctionInstance) {
        scheduler.startSingleTimer(instance, timeout.toMillis()) { instance.close() }
    }
}
