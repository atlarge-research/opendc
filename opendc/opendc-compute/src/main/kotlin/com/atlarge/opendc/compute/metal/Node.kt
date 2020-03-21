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

package com.atlarge.opendc.compute.metal

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.core.Identity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * A bare-metal compute node.
 */
public data class Node(
    /**
     * The unique identifier of the node.
     */
    public override val uid: UUID,

    /**
     * The optional name of the node.
     */
    public override val name: String,

    /**
     * Metadata of the node.
     */
    public val metadata: Map<String, Any>,

    /**
     * The last known state of the compute node.
     */
    public val state: NodeState,

    /**
     * The boot image of the node.
     */
    public val image: Image,

    /**
     * The server instance that is running on the node or `null` if no server is running.
     */
    public val server: Server?,

    /**
     * The events that are emitted by the node.
     */
    public val events: Flow<NodeEvent>
) : Identity {
    override fun hashCode(): Int = uid.hashCode()
    override fun equals(other: Any?): Boolean = other is Node && uid == other.uid
}
