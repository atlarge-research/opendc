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

import com.atlarge.odcsim.internal.BehaviorInterpreter
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Test suite for [Behavior] and [BehaviorInterpreter].
 */
@DisplayName("Behavior")
class BehaviorTest {
    /**
     * Test whether we cannot start an actor with the [unhandled] behavior.
     */
    @Test
    fun `should not start with unhandled behavior`() {
        val ctx = mock<ActorContext<Unit>>()
        val interpreter = BehaviorInterpreter(unhandled<Unit>())
        assertThrows<IllegalArgumentException> { interpreter.start(ctx) }
    }

    /**
     * Test whether we cannot start an actor with deferred unhandled behavior.
     */
    @Test
    fun `should not start with deferred unhandled behavior`() {
        val ctx = mock<ActorContext<Unit>>()
        val interpreter = BehaviorInterpreter(setup<Unit> { unhandled() })
        assertThrows<IllegalArgumentException> { interpreter.start(ctx) }
    }

    /**
     * Test whether deferred behavior that returns [same] fails.
     */
    @Test
    fun `should not allow setup to return same`() {
        val ctx = mock<ActorContext<Unit>>()
        val interpreter = BehaviorInterpreter(setup<Unit> { same() })
        assertThrows<IllegalArgumentException> { interpreter.start(ctx) }
    }

    /**
     * Test whether deferred behavior that returns [unhandled] fails.
     */
    @Test
    fun `should not allow setup to return unhandled`() {
        val ctx = mock<ActorContext<Unit>>()
        val interpreter = BehaviorInterpreter(setup<Unit> { unhandled() })
        assertThrows<IllegalArgumentException> { interpreter.start(ctx) }
    }
}
