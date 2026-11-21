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

package org.opendc.web.server.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Optional
import java.util.UUID

/** How a store is addressed, and which one a deployment's configuration ends up with. */
class ObjectStorageTest {
    @TempDir
    lateinit var root: Path

    private val key = traceKey(UUID.randomUUID(), "tasks")

    @Test
    fun `a key is derived from the trace and the table, not from the bytes`() {
        val id = UUID.randomUUID()

        assertEquals("traces/$id/tasks.parquet", traceKey(id, "tasks"))
    }

    @Test
    fun `hands a launcher URLs that read and write the object itself`() {
        val id = UUID.randomUUID()
        val store = ObjectStorage(config(ObjectStoreKind.LOCAL)).objectStore()
        val key = traceKey(id, "tasks")
        store.put(key, "rows".byteInputStream())

        val read = store.readUrl(key, Duration.ofHours(1))

        assertTrue(read.startsWith("file:"), "a local store is read where it lies")
        assertEquals("rows", Files.readString(Path.of(URI.create(read))))
        assertEquals(read, store.writeUrl(key, Duration.ofHours(1)))
    }

    @Test
    fun `removes everything an experiment kept with one prefix`() {
        val id = UUID.randomUUID()
        val store = ObjectStorage(config(ObjectStoreKind.LOCAL)).objectStore()
        store.put("${runKey(id, 0, 0)}/host.parquet", "a".byteInputStream())
        store.put(manifestKey(id, UUID.randomUUID()), "b".byteInputStream())

        store.deletePrefix(experimentKey(id))

        assertEquals(emptyList<String>(), store.list(experimentKey(id)))
        assertTrue(resultKey(id).startsWith("${experimentKey(id)}/"), "results live under their experiment")
    }

    @Test
    fun `configuration selects which store is built`() {
        assertTrue(ObjectStorage(config(ObjectStoreKind.LOCAL)).objectStore() is LocalObjectStore)
    }

    // Nothing here reaches the network: the SDK connects on the first request, and signing is local.
    @Test
    fun `an s3 store signs an upload target against a third-party endpoint`() {
        val store =
            ObjectStorage(config(ObjectStoreKind.S3, bucket = "opendc-traces", endpoint = "https://fsn1.your-objectstorage.com"))
                .objectStore()

        val target = store.uploadTarget(key, 1024)

        assertTrue(target is UploadTarget.Direct, "a bucket must be writable from a browser")
        val parts = (target as UploadTarget.Direct).parts
        assertEquals(1, parts.size, "a kilobyte is not worth splitting")
        assertEquals(0L, parts.single().offset)
        assertEquals(1024L, parts.single().length)
        val url = parts.single().url
        assertTrue(url.startsWith("https://fsn1.your-objectstorage.com/opendc-traces/"), "path-style upload URL expected: $url")
        assertTrue("X-Amz-Signature" in url, "the target must be signed: $url")
        store.close()
    }

    @Test
    fun `an s3 store signs browser uploads for its public endpoint and launcher transfers for its own`() {
        val store =
            ObjectStorage(
                config(ObjectStoreKind.S3, endpoint = "http://minio:9000", publicEndpoint = "http://localhost:9000"),
            ).objectStore()

        val upload = (store.uploadTarget(key, 1024) as UploadTarget.Direct).parts.single().url

        assertTrue(upload.startsWith("http://localhost:9000/bucket/"), "browsers upload from outside: $upload")
        assertTrue(store.readUrl(key, Duration.ofHours(1)).startsWith("http://minio:9000/bucket/"))
        assertTrue(store.writeUrl(key, Duration.ofHours(1)).startsWith("http://minio:9000/bucket/"))
        store.close()
    }

    @Test
    fun `an incomplete s3 configuration says which key is missing`() {
        val missing = assertThrows<IllegalStateException> { ObjectStorage(config(ObjectStoreKind.S3, bucket = null)).objectStore() }

        assertTrue("bucket" in missing.message.orEmpty(), "unhelpful message: ${missing.message}")
    }

    private fun config(
        kind: ObjectStoreKind,
        bucket: String? = "bucket",
        endpoint: String? = null,
        publicEndpoint: String? = null,
    ): ObjectStoreConfig =
        object : ObjectStoreConfig {
            override fun kind() = kind

            override fun directory() = root.toString()

            override fun s3() =
                object : ObjectStoreConfig.S3Config {
                    override fun bucket(): Optional<String> = Optional.ofNullable(bucket)

                    override fun endpoint(): Optional<String> = Optional.ofNullable(endpoint)

                    override fun publicEndpoint(): Optional<String> = Optional.ofNullable(publicEndpoint)

                    override fun region() = "eu-central-1"

                    override fun accessKey(): Optional<String> = Optional.of("access")

                    override fun secretKey(): Optional<String> = Optional.of("secret")

                    override fun pathStyle() = true

                    override fun uploadWindow(): Duration = Duration.ofHours(1)
                }
        }
}
