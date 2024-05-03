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

package org.opendc.compute.api

import org.opendc.simulator.compute.workload.SimWorkload
import java.util.UUID

/**
 * A client interface for the OpenDC Compute service.
 */
public interface ComputeClient : AutoCloseable {
    /**
     * Obtain the list of [Flavor]s accessible by the requesting user.
     */
    public fun queryFlavors(): List<Flavor>

    /**
     * Obtain a [Flavor] by its unique identifier.
     *
     * @param id The identifier of the flavor.
     */
    public fun findFlavor(id: UUID): Flavor?

    /**
     * Create a new [Flavor] instance at this compute service.
     *
     * @param name The name of the flavor.
     * @param cpuCount The amount of CPU cores for this flavor.
     * @param memorySize The size of the memory in MB.
     * @param labels The identifying labels of the image.
     * @param meta The non-identifying meta-data of the image.
     */
    public fun newFlavor(
        name: String,
        cpuCount: Int,
        memorySize: Long,
        labels: Map<String, String> = emptyMap(),
        meta: Map<String, Any> = emptyMap(),
    ): Flavor

    /**
     * Obtain the list of [Image]s accessible by the requesting user.
     */
    public fun queryImages(): List<Image>

    /**
     * Obtain a [Image] by its unique identifier.
     *
     * @param id The identifier of the image.
     */
    public fun findImage(id: UUID): Image?

    /**
     * Create a new [Image] instance at this compute service.
     *
     * @param name The name of the image.
     * @param labels The identifying labels of the image.
     * @param meta The non-identifying meta-data of the image.
     */
    public fun newImage(
        name: String,
        labels: Map<String, String> = emptyMap(),
        meta: Map<String, Any> = emptyMap(),
    ): Image

    /**
     * Obtain the list of [Server]s accessible by the requesting user.
     */
    public fun queryServers(): List<Server>

    /**
     * Obtain a [Server] by its unique identifier.
     *
     * @param id The identifier of the server.
     */
    public fun findServer(id: UUID): Server?

    /**
     * Create a new [Server] instance at this compute service.
     *
     * @param name The name of the server to deploy.
     * @param image The image to be deployed.
     * @param flavor The flavor of the machine instance to run this [image] on.
     * @param labels The identifying labels of the server.
     * @param meta The non-identifying meta-data of the server.
     * @param start A flag to indicate that the server should be started immediately.
     */
    public fun newServer(
        name: String,
        image: Image,
        flavor: Flavor,
        labels: Map<String, String> = emptyMap(),
        meta: Map<String, Any> = emptyMap(),
        start: Boolean = true,
    ): Server

    public fun rescheduleServer(
        server: Server,
        workload: SimWorkload,
    )

    /**
     * Release the resources associated with this client, preventing any further API calls.
     */
    public override fun close()
}
