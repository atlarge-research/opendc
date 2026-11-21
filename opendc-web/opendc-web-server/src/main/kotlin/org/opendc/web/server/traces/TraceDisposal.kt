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

package org.opendc.web.server.traces

import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TraceGrant
import org.opendc.web.server.model.TraceImport
import org.opendc.web.server.model.TraceOrigin
import org.opendc.web.server.model.TracePart
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.rest.conflict
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.traceKey
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

private const val SWEEP_INTERVAL = "1h"

/**
 * Removes traces, and sweeps the uploads nobody finished: registering, sending tables and completing
 * are separate requests, so a closed browser leaves an invisible row and objects that hold storage.
 */
@ApplicationScoped
class TraceDisposal(private val store: ObjectStore) {
    /**
     * Frees [slug] for a new trace of [owner]'s by taking over an unfinished one of that name, which
     * nobody can see. One that is still being imported is left to finish.
     */
    fun claim(
        slug: String,
        owner: UserAccount,
    ) {
        val existing = Trace.findBySlug(slug) ?: return
        if (existing.owner?.id != owner.id || existing.isComplete()) {
            throw conflict("You already have a trace called $slug")
        }
        if (TraceImport.isRunningFor(existing.id)) {
            throw conflict("$slug is still being imported")
        }
        discard(existing)
    }

    /** Removes a trace and everything belonging to it, in the store as well as the database. */
    fun discard(trace: Trace) {
        TraceImport.findByTrace(trace.id)?.delete()
        for (grant in TraceGrant.findByTrace(trace.id)) {
            grant.delete()
        }
        for (part in TracePart.findByTrace(trace.id)) {
            part.delete()
        }
        for (table in trace.kind.tables) {
            store.delete(traceKey(trace.publicId, table))
        }
        trace.delete()
        Trace.flush()
    }

    @Scheduled(every = SWEEP_INTERVAL, concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    fun sweepAbandoned() {
        val cutoff = Instant.now().minus(ABANDONED_AFTER)
        val stale = Trace.list("origin = ?1 and updatedAt < ?2", TraceOrigin.UPLOADED, cutoff)
        for (trace in stale.filterNot { it.isComplete() || TraceImport.isRunningFor(it.id) }) {
            LOG.info("Removing {}, registered {} ago and never finished", trace.slug, ABANDONED_AFTER)
            discard(trace)
        }
    }

    private companion object {
        val ABANDONED_AFTER: Duration = Duration.ofHours(24)
        val LOG = LoggerFactory.getLogger(TraceDisposal::class.java)
    }
}
