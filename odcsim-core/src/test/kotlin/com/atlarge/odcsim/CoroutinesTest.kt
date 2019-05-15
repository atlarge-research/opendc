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

package com.atlarge.odcsim

import com.atlarge.odcsim.coroutines.SuspendingBehavior
import com.atlarge.odcsim.coroutines.suspending
import com.atlarge.odcsim.internal.BehaviorInterpreter
import com.atlarge.odcsim.internal.EmptyBehavior
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.coroutines.suspendCoroutine

/**
 * Test suite for [SuspendingBehavior] using Kotlin Coroutines.
 */
@DisplayName("Coroutines")
internal class CoroutinesTest {

    @Test
    fun `should immediately return new behavior`() {
        val ctx = mock<ActorContext<Nothing>>()
        val behavior = suspending<Nothing> { empty() }
        val interpreter = BehaviorInterpreter(behavior)
        interpreter.start(ctx)
        assertTrue(interpreter.behavior as Behavior<*> is EmptyBehavior)
    }

    @Test
    fun `should be able to invoke regular suspend methods`() {
        val ctx = mock<ActorContext<Unit>>()
        val behavior = suspending<Unit> {
            suspendCoroutine<Unit> {}
            stopped()
        }
        val interpreter = BehaviorInterpreter(behavior)
        interpreter.start(ctx)
        interpreter.interpretMessage(ctx, Unit)
    }
}
