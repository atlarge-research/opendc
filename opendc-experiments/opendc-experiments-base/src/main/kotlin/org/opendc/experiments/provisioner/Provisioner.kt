/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.experiments.provisioner

import org.opendc.common.Dispatcher
import org.opendc.experiments.MutableServiceRegistry
import org.opendc.experiments.ServiceRegistry
import org.opendc.experiments.internal.ServiceRegistryImpl
import java.util.ArrayDeque
import java.util.SplittableRandom

/**
 * A helper class to set up the experimental environment in a reproducible manner.
 *
 * With this class, users describe the environment using multiple [ProvisioningStep]s. These re-usable
 * [ProvisioningStep]s are executed sequentially and ensure that the necessary infrastructure is configured and teared
 * down after the simulation completes.
 *
 * @param dispatcher The [Dispatcher] implementation for scheduling future tasks.
 * @param seed A seed for initializing the randomness of the environment.
 */
public class Provisioner(dispatcher: Dispatcher, seed: Long) : AutoCloseable {
    /**
     * Implementation of [ProvisioningContext].
     */
    private val context = object : ProvisioningContext {
        override val dispatcher: Dispatcher = dispatcher
        override val seeder: SplittableRandom = SplittableRandom(seed)
        override val registry: MutableServiceRegistry = ServiceRegistryImpl()

        override fun toString(): String = "Provisioner.ProvisioningContext"
    }

    /**
     * The stack of handles to run during the clean-up process.
     */
    private val stack = ArrayDeque<AutoCloseable>()

    /**
     * The [ServiceRegistry] containing the services registered in this environment.
     */
    public val registry: ServiceRegistry
        get() = context.registry

    /**
     * Run a single [ProvisioningStep] for this environment.
     *
     * @param step The step to apply to the environment.
     */
    public fun runStep(step: ProvisioningStep) {
        val handle = step.apply(context)
        stack.push(handle)
    }

    /**
     * Run multiple [ProvisioningStep]s for this environment.
     *
     * @param steps The steps to apply to the environment.
     */
    public fun runSteps(vararg steps: ProvisioningStep) {
        val ctx = context
        val stack = stack
        for (step in steps) {
            val handle = step.apply(ctx)
            stack.push(handle)
        }
    }

    /**
     * Clean-up the environment.
     */
    override fun close() {
        val stack = stack
        while (stack.isNotEmpty()) {
            stack.pop().close()
        }
    }
}
