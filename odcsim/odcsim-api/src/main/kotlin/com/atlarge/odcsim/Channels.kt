/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

suspend fun <T : Any, U : Any> SendRef<T>.ask(block: (SendRef<U>) -> T): U {
    val ctx = processContext
    val outlet = ctx.connect(this)
    val channel = ctx.open<U>()
    try {
        outlet.send(block(channel.send))
    } finally {
        outlet.close()
    }

    val inlet = ctx.listen(channel.receive)
    try {
        return inlet.receive()
    } finally {
        inlet.close()
    }
}

suspend fun <T : Any> SendRef<T>.sendOnce(msg: T) {
    val outlet = processContext.connect(this)
    try {
        outlet.send(msg)
    } finally {
        outlet.close()
    }
}
