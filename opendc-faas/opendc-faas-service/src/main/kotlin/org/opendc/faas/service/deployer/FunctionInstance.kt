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

package org.opendc.faas.service.deployer

import org.opendc.faas.service.FunctionObject

/**
 * A [FunctionInstance] is a self-contained worker—typically a container—capable of handling function executions.
 *
 * Multiple, concurrent function instances can exist for a single function, for scalability purposes.
 */
public interface FunctionInstance : AutoCloseable {
    /**
     * The state of the instance.
     */
    public val state: FunctionInstanceState

    /**
     * The [FunctionObject] that is represented by this instance.
     */
    public val function: FunctionObject

    /**
     * Invoke the function instance.
     *
     * This method will suspend execution util the function instance has returned.
     */
    public suspend fun invoke()

    /**
     * Indicate to the resource manager that the instance is not needed anymore and may be cleaned up by the resource
     * manager.
     */
    public override fun close()
}
