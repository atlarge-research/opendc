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

package org.opendc.web.server.telemetry

import io.quarkus.redis.datasource.RedisDataSource
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import java.time.Duration

enum class TelemetryStoreKind {
    /** This server's own memory, enough for one server. */
    MEMORY,

    /** Redis, shared by a deployment of several servers. */
    REDIS,
}

@ConfigMapping(prefix = "opendc.telemetry")
interface TelemetryConfig {
    @WithDefault("memory")
    fun kind(): TelemetryStoreKind

    /** How long a run's samples outlive its last report. */
    @WithDefault("PT6H")
    fun ttl(): Duration
}

@ApplicationScoped
class TelemetryStorage(
    private val config: TelemetryConfig,
    // Looked up lazily so the memory store never needs a Redis, which keeps CI free of containers.
    private val redis: Instance<RedisDataSource>,
) {
    @Produces
    @Singleton
    fun telemetryStore(): TelemetryStore =
        when (config.kind()) {
            TelemetryStoreKind.MEMORY -> MemoryTelemetryStore(config.ttl())
            TelemetryStoreKind.REDIS -> RedisTelemetryStore(redis.get(), config.ttl())
        }
}
