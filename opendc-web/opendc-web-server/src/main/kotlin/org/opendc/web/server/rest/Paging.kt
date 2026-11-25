/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.web.server.rest

import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import kotlinx.serialization.Serializable

@Serializable
data class Page<T>(
    val items: List<T>,
    val total: Long,
)

data class Window(
    val offset: Int,
    val limit: Int,
) {
    companion object {
        const val MAX_LIMIT = 200

        // Strings, since they are the @DefaultValue of a query parameter.
        const val DEFAULT_LIMIT = "50"
        const val DEFAULT_OFFSET = "0"

        fun of(
            offset: Int,
            limit: Int,
        ): Window = Window(offset.coerceAtLeast(0), limit.coerceIn(1, MAX_LIMIT))
    }
}

fun <E : Any> PanacheQuery<E>.within(window: Window): List<E> = range(window.offset, window.offset + window.limit - 1).list()
