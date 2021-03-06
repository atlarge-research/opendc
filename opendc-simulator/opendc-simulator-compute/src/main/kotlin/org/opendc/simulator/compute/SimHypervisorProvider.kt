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

package org.opendc.simulator.compute

import java.time.Clock
import kotlin.coroutines.CoroutineContext

/**
 * A service provider interface for constructing a [SimHypervisor].
 */
public interface SimHypervisorProvider {
    /**
     * A unique identifier for this hypervisor implementation.
     *
     * Each hypervisor must provide a unique ID, so that they can be selected by the user.
     * When in doubt, you may use the fully qualified name of your custom [SimHypervisor] implementation class.
     */
    public val id: String

    /**
     * Create a [SimHypervisor] instance with the specified [listener].
     */
    public fun create(context: CoroutineContext, clock: Clock, listener: SimHypervisor.Listener? = null): SimHypervisor
}
