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

package org.opendc.web.server.rest

import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput
import org.opendc.web.server.auth.Download
import org.opendc.web.server.auth.DownloadLinks
import org.opendc.web.server.model.Experiment
import org.opendc.web.server.model.RunUnit
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TracePart
import org.opendc.web.server.model.UnitState
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.resultKey
import org.opendc.web.server.storage.runKey
import org.opendc.web.server.storage.traceKey
import java.time.Instant
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// The media types of the bodies that are not JSON.
const val APPLICATION_ZIP = "application/zip"
const val TEXT_PLAIN_UTF8 = "text/plain; charset=utf-8"

private val WHITESPACE = Regex("\\s+")

private val UNSAFE_IN_A_PATH = Regex("[^a-z0-9_-]")

/**
 * What a signed download link fetches. Open to anyone holding a valid link: who may read the thing
 * was checked when the link was made.
 */
@Path("downloads")
@PermitAll
class DownloadsResource(
    private val links: DownloadLinks,
    private val store: ObjectStore,
) {
    @GET
    @Path("{ticket}")
    @Produces(APPLICATION_ZIP)
    fun fetch(
        @PathParam("ticket") ticket: String,
    ): Response =
        when (val download = links.verify(ticket, Instant.now())) {
            is Download.Archive -> {
                val experiment = Experiment.findByPublicId(UUID.fromString(download.experimentId)) ?: throw notFound("Experiment")
                archiveResponse(experiment, archiveKeys(experiment, store), store)
            }
            is Download.TraceContent -> {
                val trace = Trace.findByPublicId(UUID.fromString(download.traceId)) ?: throw notFound("Trace")
                traceContentResponse(trace, store)
            }
        }
}

/**
 * What an experiment's archive holds: only what a unit certified, since an attempt cut off partway
 * can have published some of its files. A 404 when nothing has been produced yet, so a browser never
 * follows a link onto an error.
 */
fun archiveKeys(
    experiment: Experiment,
    store: ObjectStore,
): List<String> {
    val keys =
        RunUnit
            .findByExperiment(experiment.id)
            .filter { it.state == UnitState.SUCCEEDED }
            .flatMap { store.list(runKey(experiment.publicId, it.scenarioIndex, it.seed)) }
    if (keys.isEmpty()) {
        throw notFound("Results")
    }
    return keys
}

/**
 * Everything the experiment's runs produced, as one streamed zip laid out like a local run's output
 * directory, so the same tooling reads both.
 */
fun archiveResponse(
    experiment: Experiment,
    keys: List<String>,
    store: ObjectStore,
): Response {
    val prefix = resultKey(experiment.publicId)
    val name = archiveName(experiment.name)
    val body =
        StreamingOutput { out ->
            ZipOutputStream(out).use { zip ->
                // Parquet is compressed already.
                zip.setLevel(Deflater.NO_COMPRESSION)
                for (key in keys) {
                    zip.putNextEntry(ZipEntry("$name/raw-output/${key.removePrefix("$prefix/")}"))
                    store.open(key).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    return zipAttachment(body, name)
}

/** A trace as a zip of its tables, so a workload and a carbon trace are each one download. */
fun traceContentResponse(
    trace: Trace,
    store: ObjectStore,
): Response {
    val tables = TracePart.findByTrace(trace.id).map { it.tableName }
    if (tables.isEmpty()) {
        throw notFound("Trace content")
    }
    val fileName = trace.slug.substringAfterLast('/')
    val publicId = trace.publicId
    val body =
        StreamingOutput { out ->
            ZipOutputStream(out).use { zip ->
                zip.setLevel(Deflater.NO_COMPRESSION)
                for (table in tables) {
                    zip.putNextEntry(ZipEntry("$fileName/$table.parquet"))
                    store.open(traceKey(publicId, table)).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    return zipAttachment(body, fileName)
}

private fun zipAttachment(
    body: StreamingOutput,
    name: String,
): Response = Response.ok(body).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$name.zip\"").build()

/**
 * An experiment's name as somewhere to unpack it. It ends up in a header and in every entry path, so
 * anything that could read as a directory or end the header is dropped rather than escaped.
 */
private fun archiveName(name: String): String =
    name
        .lowercase()
        .replace(WHITESPACE, "-")
        .replace(UNSAFE_IN_A_PATH, "")
        .trim('-')
        .ifEmpty { "experiment" }
