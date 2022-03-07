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

package org.opendc.web.client

import org.opendc.web.client.internal.delete
import org.opendc.web.client.internal.get
import org.opendc.web.client.internal.post
import org.opendc.web.client.transport.TransportClient
import org.opendc.web.proto.user.Portfolio

/**
 * A resource representing the portfolios available to the user.
 */
public class PortfolioResource internal constructor(private val client: TransportClient) {
    /**
     * List all portfolios that belong to the specified [project].
     */
    public fun getAll(project: Long): List<Portfolio> = client.get("projects/$project/portfolios") ?: emptyList()

    /**
     * Obtain the portfolio for [project] with [number].
     */
    public fun get(project: Long, number: Int): Portfolio? = client.get("projects/$project/portfolios/$number")

    /**
     * Create a new portfolio for [project] with the specified [request].
     */
    public fun create(project: Long, request: Portfolio.Create): Portfolio {
        return checkNotNull(client.post("projects/$project/portfolios", request))
    }

    /**
     * Delete the portfolio for [project] with [index].
     */
    public fun delete(project: Long, index: Int): Portfolio {
        return requireNotNull(client.delete("projects/$project/portfolios/$index")) { "Unknown portfolio $index" }
    }
}
