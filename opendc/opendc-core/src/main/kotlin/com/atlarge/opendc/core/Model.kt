/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.core

import com.atlarge.odcsim.ProcessContext

/**
 * A simulation model for large-scale simulation of datacenter infrastructure, built with the *odcsim* API.
 *
 * @property environment The environment in which brokers operate.
 * @property brokers The brokers acting on the cloud platforms.
 */
data class Model(val environment: Environment, val brokers: List<Broker>) {
    /**
     * Build the runtime behavior of the universe.
     */
    suspend operator fun invoke(ctx: ProcessContext) {
        // Setup the environment
        val platforms = environment.platforms.map { platform ->
            val channel = ctx.open<PlatformMessage>()
            ctx.spawn({ platform(it, channel.receive) }, name = platform.name)
            channel.send
        }

        for (broker in brokers) {
            ctx.spawn { broker(it, platforms) }
        }
    }
}
