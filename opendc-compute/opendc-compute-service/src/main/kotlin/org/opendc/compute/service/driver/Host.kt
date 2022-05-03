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

package org.opendc.compute.service.driver

import org.opendc.compute.api.Server
import org.opendc.compute.service.driver.telemetry.GuestCpuStats
import org.opendc.compute.service.driver.telemetry.GuestSystemStats
import org.opendc.compute.service.driver.telemetry.HostCpuStats
import org.opendc.compute.service.driver.telemetry.HostSystemStats
import java.util.*

/**
 * Base interface for representing compute resources that host virtualized [Server] instances.
 */
public interface Host {
    /**
     * A unique identifier representing the host.
     */
    public val uid: UUID

    /**
     * The name of this host.
     */
    public val name: String

    /**
     * The machine model of the host.
     */
    public val model: HostModel

    /**
     * The state of the host.
     */
    public val state: HostState

    /**
     * Meta-data associated with the host.
     */
    public val meta: Map<String, Any>

    /**
     * The [Server] instances known to the host.
     */
    public val instances: Set<Server>

    /**
     * Determine whether the specified [instance][server] can still fit on this host.
     */
    public fun canFit(server: Server): Boolean

    /**
     * Register the specified [instance][server] on the host.
     *
     * Once the method returns, the instance should be running if [start] is true or else the instance should be
     * stopped.
     */
    public suspend fun spawn(server: Server, start: Boolean = true)

    /**
     * Determine whether the specified [instance][server] exists on the host.
     */
    public operator fun contains(server: Server): Boolean

    /**
     * Start the server [instance][server] if it is currently not running on this host.
     *
     * @throws IllegalArgumentException if the server is not present on the host.
     */
    public suspend fun start(server: Server)

    /**
     * Stop the server [instance][server] if it is currently running on this host.
     *
     * @throws IllegalArgumentException if the server is not present on the host.
     */
    public suspend fun stop(server: Server)

    /**
     * Delete the specified [instance][server] on this host and cleanup all resources associated with it.
     */
    public suspend fun delete(server: Server)

    /**
     * Add a [HostListener] to this host.
     */
    public fun addListener(listener: HostListener)

    /**
     * Remove a [HostListener] from this host.
     */
    public fun removeListener(listener: HostListener)

    /**
     * Query the system statistics of the host.
     */
    public fun getSystemStats(): HostSystemStats

    /**
     * Query the system statistics of a [Server] that is located on this host.
     *
     * @param server The [Server] to obtain the system statistics of.
     * @throws IllegalArgumentException if the server is not present on the host.
     */
    public fun getSystemStats(server: Server): GuestSystemStats

    /**
     * Query the CPU statistics of the host.
     */
    public fun getCpuStats(): HostCpuStats

    /**
     * Query the CPU statistics of a [Server] that is located on this host.
     *
     * @param server The [Server] to obtain the CPU statistics of.
     * @throws IllegalArgumentException if the server is not present on the host.
     */
    public fun getCpuStats(server: Server): GuestCpuStats
}
