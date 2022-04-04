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

package org.opendc.web.client.internal

import com.fasterxml.jackson.core.type.TypeReference
import org.opendc.web.client.transport.TransportClient

/**
 * Perform a GET request for resource at [path] and convert to type [T].
 */
internal inline fun <reified T> TransportClient.get(path: String): T? {
    return get(path, object : TypeReference<T>() {})
}

/**
 * Perform a POST request for resource at [path] and convert to type [T].
 */
internal inline fun <B, reified T> TransportClient.post(path: String, body: B): T? {
    return post(path, body, object : TypeReference<T>() {})
}

/**
 * Perform a PUT request for resource at [path] and convert to type [T].
 */
internal inline fun <B, reified T> TransportClient.put(path: String, body: B): T? {
    return put(path, body, object : TypeReference<T>() {})
}

/**
 * Perform a DELETE request for resource at [path] and convert to type [T].
 */
internal inline fun <reified T> TransportClient.delete(path: String): T? {
    return delete(path, object : TypeReference<T>() {})
}
