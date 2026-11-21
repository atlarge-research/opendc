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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.opendc.web.launcher.MetricSeries
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Samples held in Redis, each key written with an expiry so nothing needs sweeping. An unreachable
 * Redis is logged and read as empty, so a cache outage never fails `/status`.
 */
class RedisTelemetryStore(redis: RedisDataSource, private val ttl: Duration) : TelemetryStore {
    private val values = redis.value(String::class.java)
    private val keys = redis.key(String::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val codec = ListSerializer(MetricSeries.serializer())

    override fun write(
        key: RunKey,
        series: List<MetricSeries>,
    ) {
        try {
            values.setex(key.toString(), ttl.toSeconds(), json.encodeToString(codec, series))
        } catch (e: Exception) {
            LOG.warn("Could not record telemetry for {}", key, e)
        }
    }

    override fun read(keys: List<RunKey>): Map<RunKey, List<MetricSeries>> {
        if (keys.isEmpty()) {
            return emptyMap()
        }
        val byName = keys.associateBy { it.toString() }
        val stored =
            try {
                values.mget(*byName.keys.toTypedArray())
            } catch (e: Exception) {
                LOG.warn("Could not read telemetry for {} runs", keys.size, e)
                return emptyMap()
            }
        return stored.mapNotNull { (name, document) ->
            val key = byName[name] ?: return@mapNotNull null
            document?.let { key to json.decodeFromString(codec, it) }
        }.toMap()
    }

    override fun forget(keys: List<RunKey>) {
        if (keys.isEmpty()) {
            return
        }
        try {
            this.keys.del(*keys.map { it.toString() }.toTypedArray())
        } catch (e: Exception) {
            LOG.warn("Could not drop telemetry for {} runs", keys.size, e)
        }
    }

    private companion object {
        val LOG = LoggerFactory.getLogger(RedisTelemetryStore::class.java)
    }
}
