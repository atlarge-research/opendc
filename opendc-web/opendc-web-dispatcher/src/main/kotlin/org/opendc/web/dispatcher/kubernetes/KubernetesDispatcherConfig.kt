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

package org.opendc.web.dispatcher.kubernetes

import org.opendc.web.dispatcher.ExecutionSlot
import java.time.Duration

/** Where executions run. [OfThisServer] is the namespace the client was configured for. */
sealed interface Namespace {
    data object OfThisServer : Namespace

    data class Named(val name: String) : Namespace
}

sealed interface PriorityClass {
    data object ClusterDefault : PriorityClass

    data class Named(val name: String) : PriorityClass
}

enum class PullPolicy(val wire: String) {
    ALWAYS("Always"),
    IF_NOT_PRESENT("IfNotPresent"),
    NEVER("Never"),
}

/**
 * How executions are run on a cluster.
 *
 * @property slot The shape of one execution, which is what bags are packed for.
 * @property maxConcurrent How many Jobs may be in flight at once.
 * @property scratchMb The writable space each pod gets for staged inputs and output.
 * @property ttl How long a finished Job lingers if nothing deletes it. Only a safety net.
 * @property pendingTimeout How long a Job may wait to start before it is withdrawn.
 */
data class KubernetesDispatcherConfig(
    val namespace: Namespace,
    val image: String,
    val pullPolicy: PullPolicy,
    val serviceAccount: String,
    val priorityClass: PriorityClass,
    val slot: ExecutionSlot,
    val maxConcurrent: Int,
    val scratchMb: Int,
    val ttl: Duration,
    val pendingTimeout: Duration,
)
