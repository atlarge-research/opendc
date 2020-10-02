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

package org.opendc.compute.core.image

import org.opendc.compute.core.execution.ServerContext
import org.opendc.core.resource.TagContainer
import org.opendc.simulator.compute.workload.SimWorkload
import java.util.*

/**
 * An application [Image] that runs a [SimWorkload].
 *
 * @property uid The unique identifier of this image.
 * @property name The name of this image.
 * @property tags The tags attached to the image.
 * @property workload The workload to run for this image.
 */
public data class SimWorkloadImage(
    public override val uid: UUID,
    public override val name: String,
    public override val tags: TagContainer,
    public val workload: SimWorkload
) : Image {
    override suspend fun invoke(ctx: ServerContext): Unit = TODO()
}
