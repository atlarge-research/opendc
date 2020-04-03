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

import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.virt.driver.SimpleVirtDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.core.resource.TagContainer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID

/**
 * A hypervisor managing the VMs of a node.
 */
object HypervisorImage : Image {
    override val uid: UUID = UUID.randomUUID()
    override val name: String = "vmm"
    override val tags: TagContainer = emptyMap()

    override suspend fun invoke(ctx: ServerContext) {
        coroutineScope {
            val driver = SimpleVirtDriver(ctx, this)
            ctx.publishService(VirtDriver.Key, driver)

            // Suspend image until it is cancelled
            try {
                suspendCancellableCoroutine<Unit> {}
            } finally {
                driver.cancel()
            }
        }
    }
}
