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

package org.opendc.simulator.compute.kernel.interference

/**
 * The interference domain of a hypervisor.
 */
public interface VmInterferenceDomain {
    /**
     * Compute the performance score of a participant in this interference domain.
     *
     * @param key The participant to obtain the score of or `null` if the participant has no key.
     * @param load The overall load on the interference domain.
     * @return A score representing the performance score to be applied to the resource consumer, with 1
     * meaning no influence, <1 means that performance degrades, and >1 means that performance improves.
     */
    public fun apply(key: VmInterferenceKey?, load: Double): Double

    /**
     * Construct an [VmInterferenceKey] for the specified [id].
     *
     * @param id The identifier of the virtual machine.
     * @return A key identifying the virtual machine as part of the interference domain. `null` if the virtual machine
     * does not participate in the domain.
     */
    public fun createKey(id: String): VmInterferenceKey?

    /**
     * Remove the specified [key] from this domain.
     */
    public fun removeKey(key: VmInterferenceKey)

    /**
     * Mark the specified [key] as active in this interference domain.
     *
     * @param key The key to join the interference domain with.
     */
    public fun join(key: VmInterferenceKey)

    /**
     * Mark the specified [key] as inactive in this interference domain.
     *
     * @param key The key of the virtual machine that wants to leave the domain.
     */
    public fun leave(key: VmInterferenceKey)
}
