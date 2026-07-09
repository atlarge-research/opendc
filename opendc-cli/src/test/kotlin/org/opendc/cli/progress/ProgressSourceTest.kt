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

package org.opendc.cli.progress

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the [ProgressSource] seam: the local [ExperimentProgress] satisfies it, and so can an
 * arbitrary producer — which is exactly the drop-in point a future remote (API-polling) source needs.
 */
class ProgressSourceTest {
    /** A stand-in producer with no simulation behind it, mimicking a remote poller. */
    private class FakeProgressSource(private val fixed: ProgressSnapshot) : ProgressSource {
        override val snapshot: ProgressSnapshot get() = fixed
    }

    @Test
    fun `the local aggregate is a ProgressSource`() {
        val source: ProgressSource = ExperimentProgress(10)
        assertEquals(10, source.snapshot.totalTasks)
        assertEquals(0, source.snapshot.completedTasks)
    }

    @Test
    fun `an arbitrary producer can satisfy the seam`() {
        val source: ProgressSource = FakeProgressSource(ProgressSnapshot(completedTasks = 3, totalTasks = 10))
        assertEquals(3, source.snapshot.completedTasks)
        assertEquals(10, source.snapshot.totalTasks)
    }
}
