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

package com.atlarge.opendc.compute.core.execution

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.core.services.AbstractServiceKey

/**
 * Represents the execution context in which an bootable [Image] runs on a [Server].
 */
public interface ServerContext {
    /**
     * The server on which the image runs.
     */
    public val server: Server

    /**
     * Request for each core the specified amount of cpu time to run from the server and wait until all the threads have
     * finished processing. If none of the cores are non-zero, the method will return immediately.
     *
     * @param req An array specifying for each core the amount of cpu time to request.
     * @param reqDuration A [Long] specifying the duration in which this request needs to be fulfilled.
     * @return An array specifying for each core the amount of cpu time it actually received.
     */
    public suspend fun run(req: LongArray, reqDuration: Long): LongArray

    /**
     * Publishes the given [service] with key [serviceKey] in the server's registry.
     */
    public suspend fun <T : Any> publishService(serviceKey: AbstractServiceKey<T>, service: T) {
        server.serviceRegistry[serviceKey] = service
    }
}
