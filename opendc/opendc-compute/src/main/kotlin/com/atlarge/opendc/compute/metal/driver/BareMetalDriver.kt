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

package com.atlarge.opendc.compute.metal.driver

import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.PowerState
import kotlinx.coroutines.flow.Flow

/**
 * A driver interface for the management interface of a bare-metal compute node.
 */
public interface BareMetalDriver {
    /**
     * The load of the machine.
     */
    public val load: Flow<Double>

    /**
     * Initialize the driver.
     */
    public suspend fun init(monitor: ServerMonitor): Node

    /**
     * Update the power state of the compute node.
     */
    public suspend fun setPower(powerState: PowerState): Node

    /**
     * Update the boot disk image of the compute node.
     *
     * Changing the boot disk image of node does not affect it while the node is running. In order to start the new boot
     * disk image, the compute node must be restarted.
     */
    public suspend fun setImage(image: Image): Node

    /**
     * Obtain the state of the compute node.
     */
    public suspend fun refresh(): Node
}
