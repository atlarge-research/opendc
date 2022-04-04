/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.web.client.transport

import com.fasterxml.jackson.core.type.TypeReference

/**
 * Low-level interface for dealing with the transport layer of the API.
 */
public interface TransportClient {
    /**
     * Obtain a resource at [path] of [targetType].
     */
    public fun <T> get(path: String, targetType: TypeReference<T>): T?

    /**
     * Update a resource at [path] of [targetType].
     */
    public fun <B, T> post(path: String, body: B, targetType: TypeReference<T>): T?

    /**
     * Replace a resource at [path] of [targetType].
     */
    public fun <B, T> put(path: String, body: B, targetType: TypeReference<T>): T?

    /**
     * Delete a resource at [path] of [targetType].
     */
    public fun <T> delete(path: String, targetType: TypeReference<T>): T?
}
