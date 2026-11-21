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

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.MultipartUpload
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import software.amazon.awssdk.services.s3.model.Part
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.UploadPartRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest
import java.io.InputStream
import java.nio.file.Files
import java.time.Duration

/**
 * How much of a file one connection is asked to carry. Kept small because one connection to object
 * storage settles at a few megabytes a second, so a large file needs many parts in parallel.
 */
private const val TARGET_PART_SIZE = 32L * 1024 * 1024

/** All S3 will accept in one upload. A file too large for that many gets larger parts instead. */
private const val MAXIMUM_PARTS = 10_000L

/** The most keys one delete request may name. */
private const val DELETE_BATCH = 1000

/**
 * The stretches a file of [sizeBytes] is cut into, in order and covering all of it. Every part but
 * the last is full-sized, so a missing part shows up as a gap in the numbering or a short part.
 */
internal fun planParts(sizeBytes: Long): List<LongRange> {
    val partSize = maxOf(TARGET_PART_SIZE, ceilDiv(sizeBytes, MAXIMUM_PARTS))
    val count = maxOf(1L, ceilDiv(sizeBytes, partSize))
    return (0 until count).map { index ->
        val offset = index * partSize
        offset..<minOf(offset + partSize, sizeBytes)
    }
}

private fun ceilDiv(
    value: Long,
    by: Long,
): Long = (value + by - 1) / by

/**
 * Objects in an S3-compatible bucket. URLs are signed separately for launchers beside the server and
 * browsers outside it, which may reach the bucket at different addresses.
 */
class S3ObjectStore(
    private val client: S3Client,
    private val launcherPresigner: S3Presigner,
    private val browserPresigner: S3Presigner,
    private val bucket: String,
    private val uploadWindow: Duration,
) : ObjectStore {
    override fun put(
        key: String,
        content: InputStream,
    ): Long {
        // Spooled because the SDK needs a content length up front, which a stream cannot give.
        val spool = Files.createTempFile("opendc-incoming-", ".part")
        try {
            val size = Files.newOutputStream(spool).use { content.copyTo(it) }
            client.putObject({ it.bucket(bucket).key(key) }, spool)
            return size
        } finally {
            Files.deleteIfExists(spool)
        }
    }

    override fun open(key: String): InputStream = client.getObject { it.bucket(bucket).key(key) }

    override fun open(
        key: String,
        offset: Long,
        length: Long,
    ): InputStream = client.getObject { it.bucket(bucket).key(key).range("bytes=$offset-${offset + length - 1}") }

    override fun size(key: String): Long = client.headObject { it.bucket(bucket).key(key) }.contentLength()

    override fun exists(key: String): Boolean =
        try {
            client.headObject { it.bucket(bucket).key(key) }
            true
        } catch (e: NoSuchKeyException) {
            false
        }

    // Paged: a bucket answers a thousand keys at a time, which a large experiment easily passes.
    override fun list(prefix: String): List<String> =
        client
            .listObjectsV2Paginator { it.bucket(bucket).prefix(prefix) }
            .contents()
            .map { it.key() }
            .sorted()

    override fun delete(key: String) {
        client.deleteObject { it.bucket(bucket).key(key) }
        // Parts of an unfinished upload are stored and charged for without being an object.
        for (upload in uploadsOf(key)) {
            client.abortMultipartUpload { it.bucket(bucket).key(key).uploadId(upload.uploadId()) }
        }
    }

    override fun uploadTarget(
        key: String,
        sizeBytes: Long,
    ): UploadTarget {
        val slices = planParts(sizeBytes)
        // A plain signed PUT carries a one-part file and leaves nothing behind if it is never sent.
        if (slices.size == 1) {
            val signed =
                browserPresigner.presignPutObject(
                    PutObjectPresignRequest
                        .builder()
                        .signatureDuration(uploadWindow)
                        .putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key).build())
                        .build(),
                )
            return UploadTarget.Direct(listOf(PartTarget(signed.url().toString(), 0, sizeBytes)))
        }

        val uploadId = client.createMultipartUpload { it.bucket(bucket).key(key) }.uploadId()
        return UploadTarget.Direct(
            slices.mapIndexed { index, slice ->
                val signed =
                    browserPresigner.presignUploadPart(
                        UploadPartPresignRequest
                            .builder()
                            .signatureDuration(uploadWindow)
                            .uploadPartRequest(
                                UploadPartRequest
                                    .builder()
                                    .bucket(bucket)
                                    .key(key)
                                    // S3 part numbers start at one.
                                    .partNumber(index + 1)
                                    .uploadId(uploadId)
                                    .build(),
                            ).build(),
                    )
                PartTarget(signed.url().toString(), slice.first, slice.last - slice.first + 1)
            },
        )
    }

    override fun readUrl(
        key: String,
        lifetime: Duration,
    ): String =
        launcherPresigner
            .presignGetObject(
                GetObjectPresignRequest
                    .builder()
                    .signatureDuration(lifetime)
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                    .build(),
            ).url()
            .toString()

    override fun writeUrl(
        key: String,
        lifetime: Duration,
    ): String =
        launcherPresigner
            .presignPutObject(
                PutObjectPresignRequest
                    .builder()
                    .signatureDuration(lifetime)
                    .putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key).build())
                    .build(),
            ).url()
            .toString()

    override fun deletePrefix(prefix: String) {
        for (batch in list("$prefix/").chunked(DELETE_BATCH)) {
            client.deleteObjects { request ->
                request.bucket(bucket).delete(
                    Delete
                        .builder()
                        .objects(batch.map { ObjectIdentifier.builder().key(it).build() })
                        .quiet(true)
                        .build(),
                )
            }
        }
    }

    override fun completeUpload(key: String): Boolean {
        val uploadId = uploadsOf(key).maxByOrNull { it.initiated() }?.uploadId() ?: return exists(key)
        val parts = client.listPartsPaginator { it.bucket(bucket).key(key).uploadId(uploadId) }.parts().toList()
        if (!arrivedWhole(parts)) {
            // Aborted so a fresh attempt starts clean and the orphaned parts stop being charged for.
            client.abortMultipartUpload { it.bucket(bucket).key(key).uploadId(uploadId) }
            return false
        }
        client.completeMultipartUpload { request ->
            request
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(
                    CompletedMultipartUpload
                        .builder()
                        .parts(parts.map { CompletedPart.builder().partNumber(it.partNumber()).eTag(it.eTag()).build() })
                        .build(),
                )
        }
        return true
    }

    override fun close() {
        client.close()
        launcherPresigner.close()
        browserPresigner.close()
    }

    /**
     * Whether these are all the parts of a file as [planParts] cut it: numbered from one without a
     * gap, and every part but the last the same full size. Missing trailing parts are not caught
     * here; they take the parquet footer with them, so the ingest check refuses the file.
     */
    private fun arrivedWhole(parts: List<Part>): Boolean {
        if (parts.isEmpty()) return false
        val ordered = parts.sortedBy { it.partNumber() }
        if (ordered.map { it.partNumber() } != List(ordered.size) { it + 1 }) return false
        val full = ordered.first().size()
        return ordered.dropLast(1).all { it.size() == full } && ordered.last().size() <= full
    }

    /** The uploads of [key] the bucket is still holding open, asked of the bucket so they cannot drift. */
    private fun uploadsOf(key: String): List<MultipartUpload> =
        client
            .listMultipartUploads { it.bucket(bucket).prefix(key) }
            .uploads()
            .filter { it.key() == key }
}
