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

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Disposes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.nio.file.Path
import java.time.Duration
import java.util.Optional

enum class ObjectStoreKind {
    /** A directory on disk, for development and tests. */
    LOCAL,

    /** Any S3-compatible object storage, for a deployment. */
    S3,
}

@ConfigMapping(prefix = "opendc.storage")
interface ObjectStoreConfig {
    @WithDefault("local")
    fun kind(): ObjectStoreKind

    /** Where [ObjectStoreKind.LOCAL] keeps its objects. */
    @WithDefault("data")
    fun directory(): String

    fun s3(): S3Config

    interface S3Config {
        fun bucket(): Optional<String>

        /** The provider's endpoint, such as `https://fsn1.your-objectstorage.com`; absent for AWS itself. */
        fun endpoint(): Optional<String>

        /** Where a browser reaches the bucket, if not at [endpoint], as with MinIO in the compose stack. */
        fun publicEndpoint(): Optional<String>

        /** Required by the SDK even where the provider ignores it. */
        @WithDefault("us-east-1")
        fun region(): String

        fun accessKey(): Optional<String>

        fun secretKey(): Optional<String>

        /** Whether a bucket is addressed as `endpoint/bucket`, which unlike `bucket.endpoint` every provider serves. */
        @WithDefault("true")
        fun pathStyle(): Boolean

        /** How long a browser has to start using a signed upload target. */
        @WithDefault("PT1H")
        fun uploadWindow(): Duration
    }
}

@ApplicationScoped
class ObjectStorage(private val config: ObjectStoreConfig) {
    @Produces
    @Singleton
    fun objectStore(): ObjectStore =
        when (config.kind()) {
            ObjectStoreKind.LOCAL -> LocalObjectStore(Path.of(config.directory()))
            ObjectStoreKind.S3 -> s3Store(config.s3())
        }

    fun closeStore(
        @Disposes store: ObjectStore,
    ) {
        store.close()
    }

    private fun s3Store(s3: ObjectStoreConfig.S3Config): ObjectStore {
        val credentials =
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(required(s3.accessKey(), "access-key"), required(s3.secretKey(), "secret-key")),
            )
        val region = Region.of(s3.region())
        val endpoint = s3.endpoint().map(URI::create)
        val publicEndpoint = s3.publicEndpoint().map(URI::create).or { endpoint }
        return S3ObjectStore(
            client = buildClient(s3, credentials, region, endpoint),
            launcherPresigner = buildPresigner(s3, credentials, region, endpoint),
            browserPresigner = buildPresigner(s3, credentials, region, publicEndpoint),
            bucket = required(s3.bucket(), "bucket"),
            uploadWindow = s3.uploadWindow(),
        )
    }

    private fun buildClient(
        s3: ObjectStoreConfig.S3Config,
        credentials: AwsCredentialsProvider,
        region: Region,
        endpoint: Optional<URI>,
    ): S3Client {
        val builder =
            S3Client
                .builder()
                .region(region)
                .credentialsProvider(credentials)
                .forcePathStyle(s3.pathStyle())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                // Several S3-compatible providers reject the flexible-checksum headers the SDK adds by default.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
        endpoint.ifPresent { builder.endpointOverride(it) }
        return builder.build()
    }

    private fun buildPresigner(
        s3: ObjectStoreConfig.S3Config,
        credentials: AwsCredentialsProvider,
        region: Region,
        endpoint: Optional<URI>,
    ): S3Presigner {
        val builder =
            S3Presigner
                .builder()
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.pathStyle()).build())
        endpoint.ifPresent { builder.endpointOverride(it) }
        return builder.build()
    }

    private fun required(
        value: Optional<String>,
        name: String,
    ): String =
        value.orElseThrow {
            IllegalStateException("opendc.storage.s3.$name must be set when opendc.storage.kind=s3")
        }
}
