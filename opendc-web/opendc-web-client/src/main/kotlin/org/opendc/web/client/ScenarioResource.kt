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
import org.opendc.web.proto.user.Scenario

/**
 * A resource representing the scenarios available to the user.
 */
public class ScenarioResource internal constructor(private val client: TransportClient) {
    /**
     * List all scenarios that belong to the specified [project].
     */
    public fun getAll(project: Long): List<Scenario> = client.get("projects/$project/scenarios") ?: emptyList()

    /**
     * List all scenarios that belong to the specified [portfolioNumber].
     */
    public fun getAll(project: Long, portfolioNumber: Int): List<Scenario> = client.get("projects/$project/portfolios/$portfolioNumber/scenarios") ?: emptyList()

    /**
     * Obtain the scenario for [project] with [index].
     */
    public fun get(project: Long, index: Int): Scenario? = client.get("projects/$project/scenarios/$index")

    /**
     * Create a new scenario for [portfolio][portfolioNumber] with the specified [request].
     */
    public fun create(project: Long, portfolioNumber: Int, request: Scenario.Create): Scenario {
        return checkNotNull(client.post("projects/$project/portfolios/$portfolioNumber", request))
    }

    /**
     * Delete the scenario for [project] with [index].
     */
    public fun delete(project: Long, index: Int): Scenario {
        return requireNotNull(client.delete("projects/$project/scenarios/$index")) { "Unknown scenario $index" }
    }
}
