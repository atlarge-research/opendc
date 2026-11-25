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

package org.opendc.web.server.execution

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import org.opendc.web.dispatcher.kubernetes.PullPolicy
import java.time.Duration
import java.util.Optional

/** Where a deployment runs its simulations. Each kind reads its own section of [DispatcherConfig]. */
enum class DispatcherKind {
    /** Subprocesses of this server. What a development machine and a self-hosted install use. */
    LOCAL,

    /** One Job per execution on a Kubernetes cluster. */
    KUBERNETES,

    /** One batch job per execution on a SLURM cluster reached over SSH, staged through its shared filesystem. */
    SLURM,
}

@ConfigMapping(prefix = "opendc.dispatcher")
interface DispatcherConfig {
    /** The `lib` directory of the installed launcher, for every dispatcher that starts it from files. */
    fun launcherLib(): String

    fun local(): Local

    fun kubernetes(): Kubernetes

    fun slurm(): Slurm

    interface Local {
        /** Cores one execution may use. Defaults to every core this machine has. */
        fun cores(): Optional<Int>

        /** Memory all executions may use between them. Defaults to half of the machine's. */
        fun memoryMb(): Optional<Double>

        /** Where manifests and launcher logs are written. */
        @WithDefault("data/executions")
        fun workDir(): String
    }

    interface Kubernetes {
        /** Where Jobs are created. Absent, the namespace this server runs in. */
        fun namespace(): Optional<String>

        /** The launcher image, which has to match this server's version: the manifest is its contract. */
        @WithDefault("ghcr.io/atlarge-research/opendc-launcher:latest")
        fun image(): String

        @WithDefault("if-not-present")
        fun imagePullPolicy(): PullPolicy

        /** Carries only the image pull secrets: the pod mounts no token. */
        @WithDefault("default")
        fun serviceAccount(): String

        fun priorityClass(): Optional<String>

        /** The shape of one execution, which is what bags are packed for. */
        @WithDefault("4")
        fun slotCores(): Int

        @WithDefault("8192")
        fun slotMemoryMb(): Double

        /** How many Jobs may be in flight at once. */
        @WithDefault("16")
        fun maxConcurrent(): Int

        /** Each pod's writable space, for its staged inputs and its output. */
        @WithDefault("20480")
        fun scratchSizeMb(): Int

        /** How long a finished Job lingers if nothing deletes it. Only a safety net. */
        @WithDefault("PT168H")
        fun ttl(): Duration

        /** How long a Job may wait for a node before it is withdrawn and tried again. */
        @WithDefault("PT1H")
        fun pendingTimeout(): Duration
    }

    interface Slurm {
        fun host(): Optional<String>

        @WithDefault("22")
        fun port(): Int

        fun user(): Optional<String>

        /** An unencrypted private key, readable by this server only. */
        fun identityFile(): Optional<String>

        /** Has to vouch for the jump host as well as the head node; nothing else is trusted. */
        @WithDefault("~/.ssh/known_hosts")
        fun knownHosts(): String

        /** `[user@]host[:port]` of a bastion the head node is reached through. */
        fun jumpHost(): Optional<String>

        /** Where launchers, cached traces and executions live, on a filesystem the compute nodes share. */
        @WithDefault("opendc")
        fun remoteRoot(): String

        /** The JRE on the shared filesystem the nodes run the launcher with: Java 21 or newer. */
        @WithDefault("java")
        fun java(): String

        fun partition(): Optional<String>

        /** Site flags every submission carries, such as an account or a QOS. */
        fun sbatchOptions(): Optional<List<String>>

        /** One node's cores and memory, which is what bags are packed for. */
        fun slotCores(): Optional<Int>

        fun slotMemoryMb(): Optional<Double>

        /** The longest a job may run, where the site caps it below the partition's own limit. */
        fun timeCap(): Optional<Duration>

        /** How many jobs may be pending or running at once. */
        @WithDefault("4")
        fun maxJobs(): Int

        @WithDefault("PT1H")
        fun pendingTimeout(): Duration

        /** How often the queue is asked about. Follow the site's etiquette. */
        @WithDefault("PT15S")
        fun pollInterval(): Duration

        /** At least the shared filesystem's attribute cache time, so a fresh exit record is seen. */
        @WithDefault("PT90S")
        fun recordGrace(): Duration

        /** At least opendc.execution.url-lifetime, so nothing live still reads what is removed. */
        @WithDefault(MAX_URL_LIFETIME)
        fun cacheRetention(): Duration
    }
}
