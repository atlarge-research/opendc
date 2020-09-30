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

package com.atlarge.opendc.compute.core

import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.core.resource.Resource
import com.atlarge.opendc.core.resource.TagContainer
import com.atlarge.opendc.core.services.ServiceRegistry
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * A server instance that is running on some physical or virtual machine.
 */
public data class Server(
    /**
     * The unique identifier of the server.
     */
    public override val uid: UUID,

    /**
     * The optional name of the server.
     */
    public override val name: String,

    /**
     * The tags of this server.
     */
    public override val tags: TagContainer,

    /**
     * The hardware configuration of the server.
     */
    public val flavor: Flavor,

    /**
     * The image running on the server.
     */
    public val image: Image,

    /**
     * The last known state of the server.
     */
    public val state: ServerState,

    /**
     * The services published by this server.
     */
    public val services: ServiceRegistry,

    /**
     * The events that are emitted by the server.
     */
    public val events: Flow<ServerEvent>
) : Resource {
    override fun hashCode(): Int = uid.hashCode()
    override fun equals(other: Any?): Boolean = other is Server && uid == other.uid
}
