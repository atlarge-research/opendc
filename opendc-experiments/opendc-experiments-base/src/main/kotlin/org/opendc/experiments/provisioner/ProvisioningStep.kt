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

import org.eclipse.microprofile.config.Config

/**
 * A provisioning step is responsible for provisioning (acquiring or configuring) infrastructure necessary for a
 * simulation experiment.
 */
public fun interface ProvisioningStep {
    /**
     * Apply the step by provisioning the required resources for the experiment using the specified
     * [ProvisioningContext][ctx].
     *
     * @param ctx The environment in which the resources should be provisioned.
     * @return A handle that is invoked once the simulation completes, so that the resources can be cleaned up.
     */
    public fun apply(ctx: ProvisioningContext): AutoCloseable

    /**
     * A factory interface for [ProvisioningStep] instances.
     *
     * @param S The type that describes the input for constructing a [ProvisioningStep].
     */
    public abstract class Provider<S>(public val type: Class<S>) {
        /**
         * The name that identifies the provisioning step.
         */
        public abstract val name: String

        /**
         * Construct a [ProvisioningStep] with the specified [spec].
         *
         * @param spec The specification that describes the provisioner to be created.
         * @param config The external configuration of the experiment runner.
         * @return The [ProvisioningStep] constructed according to [spec].
         */
        public abstract fun create(spec: S, config: Config): ProvisioningStep
    }
}
