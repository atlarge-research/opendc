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

package org.opendc.compute.service.internal

import org.opendc.compute.api.Flavor
import org.opendc.compute.api.Image
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.api.ServerWatcher
import java.time.Instant
import java.util.*

/**
 * A [Server] implementation that is passed to clients but delegates its implementation to another class.
 */
internal class ClientServer(private val delegate: Server) : Server, ServerWatcher {
    private val watchers = mutableListOf<ServerWatcher>()

    override val uid: UUID = delegate.uid

    override var name: String = delegate.name
        private set

    override var flavor: Flavor = delegate.flavor
        private set

    override var image: Image = delegate.image
        private set

    override var labels: Map<String, String> = delegate.labels.toMap()
        private set

    override var meta: Map<String, Any> = delegate.meta.toMap()
        private set

    override var state: ServerState = delegate.state
        private set

    override var launchedAt: Instant? = null
        private set

    override suspend fun start() {
        delegate.start()
        refresh()
    }

    override suspend fun stop() {
        delegate.stop()
        refresh()
    }

    override suspend fun delete() {
        delegate.delete()
        refresh()
    }

    override fun watch(watcher: ServerWatcher) {
        if (watchers.isEmpty()) {
            delegate.watch(this)
        }

        watchers += watcher
    }

    override fun unwatch(watcher: ServerWatcher) {
        watchers += watcher

        if (watchers.isEmpty()) {
            delegate.unwatch(this)
        }
    }

    override suspend fun refresh() {
        delegate.refresh()

        name = delegate.name
        flavor = delegate.flavor
        image = delegate.image
        labels = delegate.labels
        meta = delegate.meta
        state = delegate.state
        launchedAt = delegate.launchedAt
    }

    override fun onStateChanged(server: Server, newState: ServerState) {
        val watchers = watchers

        for (watcher in watchers) {
            watcher.onStateChanged(this, newState)
        }
    }

    override fun equals(other: Any?): Boolean = other is Server && other.uid == uid

    override fun hashCode(): Int = uid.hashCode()

    override fun toString(): String = "Server[uid=$uid,name=$name,state=$state]"
}
