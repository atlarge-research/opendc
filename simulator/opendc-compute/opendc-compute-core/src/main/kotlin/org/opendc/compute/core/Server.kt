/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.compute.core

import org.opendc.compute.core.image.Image
import org.opendc.core.resource.Resource

/**
 * A stateful object representing a server instance that is running on some physical or virtual machine.
 */
public interface Server : Resource {
    /**
     * The name of the server.
     */
    public override val name: String

    /**
     * The flavor of the server.
     */
    public val flavor: Flavor

    /**
     * The image of the server.
     */
    public val image: Image

    /**
     * The tags assigned to the server.
     */
    public override val tags: Map<String, String>

    /**
     * The last known state of the server.
     */
    public val state: ServerState

    /**
     * Register the specified [ServerWatcher] to watch the state of the server.
     *
     * @param watcher The watcher to register for the server.
     */
    public fun watch(watcher: ServerWatcher)

    /**
     * De-register the specified [ServerWatcher] from the server to stop it from receiving events.
     *
     * @param watcher The watcher to de-register from the server.
     */
    public fun unwatch(watcher: ServerWatcher)

    /**
     * Refresh the local state of the resource.
     */
    public suspend fun refresh()
}
