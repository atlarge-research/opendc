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

package org.opendc.faas.api

import java.util.UUID

/**
 * Client interface to the OpenDC FaaS platform.
 */
public interface FaaSClient : AutoCloseable {
    /**
     * Obtain the list of [FaaSFunction]s accessible by the requesting user.
     */
    public suspend fun queryFunctions(): List<FaaSFunction>

    /**
     * Obtain a [FaaSFunction] by its unique identifier.
     *
     * @param id The identifier of the flavor.
     */
    public suspend fun findFunction(id: UUID): FaaSFunction?

    /**
     * Obtain a [FaaSFunction] by its name.
     *
     * @param name The name of the function.
     */
    public suspend fun findFunction(name: String): FaaSFunction?

    /**
     * Create a new serverless function.
     *
     * @param name The name of the function.
     * @param memorySize The memory allocated for the function in MB.
     * @param labels The labels associated with the function.
     * @param meta The metadata associated with the function.
     */
    public suspend fun newFunction(
        name: String,
        memorySize: Long,
        labels: Map<String, String> = emptyMap(),
        meta: Map<String, Any> = emptyMap()
    ): FaaSFunction

    /**
     * Invoke the function with the specified [name].
     */
    public suspend fun invoke(name: String)

    /**
     * Release the resources associated with this client, preventing any further API calls.
     */
    public override fun close()
}
