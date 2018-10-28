/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
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

package com.atlarge.odcsim

import com.atlarge.odcsim.engine.omega.OmegaActorSystem
import org.junit.jupiter.api.Test

/**
 * A test to verify the system runs without smoking.
 */
class SmokeTest {
    @Test
    fun `hello-world`() {
        val system = OmegaActorSystem(object : Behavior<String> {
            override fun receive(ctx: ActorContext<String>, msg: String): Behavior<String> {
                println("${ctx.time} $msg")
                return this
            }

            override fun receiveSignal(ctx: ActorContext<String>, signal: Signal): Behavior<String> {
                println("${ctx.time} $signal")
                return this
            }
        }, name = "test")

        system.send("Hello World", after = 1.2)
        system.run()
    }
}
