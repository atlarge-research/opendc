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

import com.atlarge.opendc.core.services.ServiceKey

/**
 * An event that is emitted by a [Server].
 */
public sealed class ServerEvent {
    /**
     * The server that emitted the event.
     */
    public abstract val server: Server

    /**
     * This event is emitted when the state of [server] changes.
     *
     * @property server The server of which the state changed.
     * @property previousState The previous state of the server.
     */
    public data class StateChanged(override val server: Server, val previousState: ServerState) : ServerEvent()

    /**
     * This event is emitted when a server publishes a service.
     *
     * @property server The server that published the service.
     * @property key The service key of the service that was published.
     */
    public data class ServicePublished(override val server: Server, val key: ServiceKey<*>) : ServerEvent()
}
