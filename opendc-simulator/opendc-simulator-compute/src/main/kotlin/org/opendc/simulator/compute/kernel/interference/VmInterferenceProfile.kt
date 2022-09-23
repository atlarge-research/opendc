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

package org.opendc.simulator.compute.kernel.interference

/**
 * A profile of a particular virtual machine describing its interference pattern with other virtual machines.
 *
 * @param model The model to which this profile belongs.
 * @property id The identifier of the profile inside the model.
 * @property membership The membership of the profile in the groups.
 * @param members The members in the model.
 * @param targets The targets in the model.
 * @param scores The scores in the model.
 */
public class VmInterferenceProfile internal constructor(
    private val model: VmInterferenceModel,
    private val id: Int,
    private val membership: IntArray,
    private val members: Array<IntArray>,
    private val targets: DoubleArray,
    private val scores: DoubleArray
) {
    /**
     * Create a new [VmInterferenceMember] based on this profile for the specified [domain].
     */
    internal fun newMember(domain: VmInterferenceDomain): VmInterferenceMember {
        return VmInterferenceMember(domain, model, id, membership, members, targets, scores)
    }

    override fun toString(): String = "VmInterferenceProfile[id=$id]"
}
