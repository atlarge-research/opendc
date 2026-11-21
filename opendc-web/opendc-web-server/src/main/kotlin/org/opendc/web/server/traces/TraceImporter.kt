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

import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.scheduler.Scheduled
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.event.TransactionPhase
import org.opendc.web.server.model.ImportState
import org.opendc.web.server.model.TraceImport
import org.opendc.web.server.rest.InvalidDocumentException
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.traceKey
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** 10 GiB. */
private const val DEFAULT_MAX_BYTES = "10737418240"

private const val EXPIRY_INTERVAL = "10m"

@ConfigMapping(prefix = "opendc.traces.import")
interface TraceImportConfig {
    /** The largest table this server fetches. Over S3 storage it is spooled to local disk first. */
    @WithDefault(DEFAULT_MAX_BYTES)
    fun maxBytes(): Long

    /** How long an import may take before it is failed, whether or not anything is still working on it. */
    @WithDefault("PT6H")
    fun deadline(): Duration

    /** Whether URLs may name private, loopback and link-local addresses; off so no URL reaches into the cluster. */
    @WithDefault("false")
    fun allowPrivateHosts(): Boolean

    /** How many imports run at once. */
    @WithDefault("2")
    fun concurrency(): Int
}

/** An import is to start, once the row that tracks it is committed. */
data class ImportRequested(
    val importId: UUID,
    val traceId: UUID,
    val sources: Map<String, URI>,
)

/**
 * Fetches traces from URLs in the background, then checks and records them as an upload is. Every
 * table is fetched before any is inspected, so a trace is never half there.
 */
@ApplicationScoped
class TraceImporter(
    private val store: ObjectStore,
    private val ingest: TraceIngest,
    private val config: TraceImportConfig,
) {
    private val workers: ExecutorService =
        Executors.newFixedThreadPool(config.concurrency()) { task -> Thread(task, "trace-import").also { it.isDaemon = true } }

    fun onRequested(
        @Observes(during = TransactionPhase.AFTER_SUCCESS) event: ImportRequested,
    ) {
        workers.execute { run(event) }
    }

    /** Fails imports past their deadline, including those whose server went away while running them. */
    @Scheduled(every = EXPIRY_INTERVAL, concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    fun expire() {
        QuarkusTransaction.requiringNew().run {
            val now = Instant.now()
            for (stale in TraceImport.findRunningSince(now.minus(config.deadline()))) {
                stale.fail(now, "The import did not finish before its deadline")
            }
        }
    }

    @PreDestroy
    fun close() {
        workers.shutdownNow()
    }

    private fun run(event: ImportRequested) {
        val started = QuarkusTransaction.requiringNew().call { TraceImport.findByPublicId(event.importId)?.createdAt } ?: return
        val fetch = RemoteFetch(config.allowPrivateHosts(), config.maxBytes(), started.plus(config.deadline()))
        val settled =
            try {
                for ((table, uri) in event.sources) {
                    fetch.read(uri) { body -> store.put(traceKey(event.traceId, table), body) }
                }
                settle(event.importId) {
                    ingest.record(it.trace)
                    it.succeed(Instant.now())
                }
            } catch (e: Exception) {
                LOG.info("Import {} failed: {}", event.importId, e.message)
                settle(event.importId) { it.fail(Instant.now(), reasonOf(e)) }
            }
        if (!settled) {
            // Failed by the deadline or discarded while this was fetching: what was written is nobody's.
            event.sources.keys.forEach { store.delete(traceKey(event.traceId, it)) }
        }
    }

    /** Applies [change] to the import in a transaction of its own if it is still running. Returns whether it was. */
    private fun settle(
        importId: UUID,
        change: (TraceImport) -> Unit,
    ): Boolean =
        QuarkusTransaction.requiringNew().call {
            val running = TraceImport.lockByPublicId(importId)?.takeIf { it.state == ImportState.RUNNING } ?: return@call false
            change(running)
            true
        }

    private fun reasonOf(e: Exception): String =
        when (e) {
            is InvalidDocumentException -> {
                val issue = e.problem.issues.firstOrNull()
                if (issue == null) e.problem.title else "${e.problem.title}: ${issue.message}"
            }
            is FetchFailure -> e.message ?: "The fetch failed"
            else -> "The fetch failed: ${e.message ?: e.javaClass.simpleName}"
        }

    private companion object {
        val LOG = LoggerFactory.getLogger(TraceImporter::class.java)
    }
}
