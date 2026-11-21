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

package org.opendc.web.server

import io.quarkus.narayana.jta.QuarkusTransaction
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TraceKind
import org.opendc.web.server.model.TraceOrigin
import org.opendc.web.server.model.TracePart
import org.opendc.web.server.model.UserAccount
import java.time.Instant
import java.util.UUID

/** A trace in someone's library, as its row says it is; the bytes behind it are never read here. */
data class TestTrace(
    val id: UUID,
    val slug: String,
)

object TestTraces {
    /** A trace of [owner]'s, whole when [finished] and an upload that never completed otherwise. */
    fun owned(
        owner: TestPerson,
        kind: TraceKind,
        finished: Boolean = true,
    ): TestTrace =
        QuarkusTransaction.requiringNew().call {
            val trace = Trace()
            trace.slug = "${owner.handle}/trace-${UUID.randomUUID().toString().take(8)}"
            trace.kind = kind
            trace.origin = TraceOrigin.UPLOADED
            trace.owner = UserAccount.findById(owner.id)
            trace.createdAt = Instant.now()
            trace.updatedAt = trace.createdAt
            trace.persist()
            if (finished) {
                for (table in kind.tables) {
                    val part = TracePart()
                    part.trace = trace
                    part.tableName = table
                    part.persist()
                }
            }
            TestTrace(trace.publicId, trace.slug)
        }
}
