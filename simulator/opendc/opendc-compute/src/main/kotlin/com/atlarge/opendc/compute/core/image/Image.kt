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

package com.atlarge.opendc.compute.core.image

import com.atlarge.opendc.compute.core.execution.ServerContext
import com.atlarge.opendc.core.resource.Resource

/**
 * An image containing a bootable operating system that can directly be executed by physical or virtual server.
 *
 * OpenStack: A collection of files used to create or rebuild a server. Operators provide a number of pre-built OS
 * images by default. You may also create custom images from cloud servers you have launched. These custom images are
 * useful for backup purposes or for producing “gold” server images if you plan to deploy a particular server
 * configuration frequently.
 */
public interface Image : Resource {
    /**
     * Launch the machine image in the specified [ServerContext].
     *
     * This method should encapsulate and characterize the runtime behavior of the instance resulting from launching
     * the image on some machine, in terms of the resource consumption on the machine.
     */
    public suspend operator fun invoke(ctx: ServerContext)
}
