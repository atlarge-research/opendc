/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.virt

import com.atlarge.opendc.core.Identity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * A hypervisor (or virtual machine monitor) is software or firmware that virtualizes the host compute environment
 * into several virtual guest machines.
 */
public class Hypervisor(
    /**
     * The unique identifier of the hypervisor.
     */
    override val uid: UUID,

    /**
     * The optional name of the hypervisor.
     */
    override val name: String,

    /**
     * Metadata of the hypervisor.
     */
    public val metadata: Map<String, Any>,

    /**
     * The events that are emitted by the hypervisor.
     */
    public val events: Flow<HypervisorEvent>
) : Identity {
    override fun hashCode(): Int = uid.hashCode()
    override fun equals(other: Any?): Boolean = other is Hypervisor && uid == other.uid
}
