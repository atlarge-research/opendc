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
import java.util.SplittableRandom
import java.util.random.RandomGenerator

/**
 * The [ProvisioningContext] class provides access to shared state between subsequent [ProvisioningStep]s, as well as
 * access to the simulation dispatcher, the virtual clock, and a randomness seeder to allow
 * the provisioning steps to initialize the (simulated) resources.
 */
public interface ProvisioningContext {
    /**
     * The [Dispatcher] provided by the provisioner to schedule future events during the simulation.
     */
    public val dispatcher: Dispatcher

    /**
     * A [SplittableRandom] instance used to seed the provisioners.
     */
    public val seeder: RandomGenerator

    /**
     * A [MutableServiceRegistry] where the provisioned services are registered.
     */
    public val registry: MutableServiceRegistry
}
