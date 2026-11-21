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

import com.sun.management.OperatingSystemMXBean
import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Disposes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.dispatcher.ExecutionSlot
import org.opendc.web.dispatcher.TimeCap
import org.opendc.web.dispatcher.kubernetes.KubernetesDispatcher
import org.opendc.web.dispatcher.kubernetes.KubernetesDispatcherConfig
import org.opendc.web.dispatcher.kubernetes.Namespace
import org.opendc.web.dispatcher.kubernetes.PriorityClass
import org.opendc.web.dispatcher.local.LocalDispatcher
import org.opendc.web.dispatcher.local.LocalDispatcherConfig
import org.opendc.web.dispatcher.slurm.JumpHost
import org.opendc.web.dispatcher.slurm.Partition
import org.opendc.web.dispatcher.slurm.SlurmDispatcher
import org.opendc.web.dispatcher.slurm.SlurmDispatcherConfig
import org.opendc.web.dispatcher.slurm.SshTarget
import org.opendc.web.launcher.BYTES_PER_MB
import org.opendc.web.server.storage.ObjectStoreConfig
import org.opendc.web.server.storage.ObjectStoreKind
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URI
import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

/**
 * Builds the one dispatcher this deployment runs work on, and closes it again on shutdown.
 *
 * Unremovable because a test supplies its own dispatcher, and dropping this producer would drop the
 * only injection point of [DispatcherConfig], so its settings would be refused as unmapped.
 */
@Unremovable
@ApplicationScoped
class Dispatch(
    private val config: ExecutionConfig,
    private val dispatchers: DispatcherConfig,
    private val storage: ObjectStoreConfig,
) {
    @Produces
    @Singleton
    fun dispatcher(): Dispatcher {
        val problems = deploymentProblems(config.dispatcher(), storage.kind(), config.telemetryUrl())
        check(problems.isEmpty()) { problems.joinToString("; ") }
        return when (config.dispatcher()) {
            DispatcherKind.LOCAL -> dispatchers.localDispatcher()
            DispatcherKind.KUBERNETES -> KubernetesDispatcher.connect(dispatchers.kubernetes().toConfig())
            DispatcherKind.SLURM -> SlurmDispatcher(dispatchers.slurm().toConfig(dispatchers.launcherDistribution()))
        }
    }

    fun close(
        @Disposes dispatcher: Dispatcher,
    ) {
        dispatcher.close()
    }
}

/** How much of the machine's memory a deployment that says nothing may use for simulations. */
private const val MEMORY_SHARE = 0.5

/** Hosts a launcher somewhere else cannot reach this server on. */
private val LOOPBACK = setOf("localhost", "::1", "[::1]", "0.0.0.0")

/**
 * What stops this deployment from running on [kind]; empty when nothing does. A pod can neither read
 * this server's disk nor reach its loopback address; a SLURM job reaches only its shared filesystem,
 * which this server stages into itself.
 */
fun deploymentProblems(
    kind: DispatcherKind,
    storage: ObjectStoreKind,
    telemetryUrl: String,
): List<String> =
    when (kind) {
        DispatcherKind.LOCAL, DispatcherKind.SLURM -> emptyList()
        DispatcherKind.KUBERNETES ->
            buildList {
                if (storage == ObjectStoreKind.LOCAL) {
                    add("opendc.execution.dispatcher=kubernetes cannot read this server's disk; set opendc.storage.kind=s3")
                }
                val host = URI.create(telemetryUrl).host.orEmpty()
                if (host in LOOPBACK || host.startsWith("127.")) {
                    add("opendc.execution.telemetry-url points at $host, which a pod cannot reach")
                }
            }
    }

fun DispatcherConfig.localDispatcher(): LocalDispatcher =
    LocalDispatcher(
        LocalDispatcherConfig(
            cores = local().cores().orElseGet { Runtime.getRuntime().availableProcessors() },
            memoryMb = local().memoryMb().orElseGet { machineMemoryMb() * MEMORY_SHARE },
            // The JVM, not a shell, expands the wildcard to every jar in the directory.
            classpath = "${launcherDistribution().toAbsolutePath()}${File.separator}*",
            workDir = Path.of(local().workDir()),
        ),
    )

fun DispatcherConfig.Kubernetes.toConfig(): KubernetesDispatcherConfig =
    KubernetesDispatcherConfig(
        namespace = namespace().map<Namespace> { Namespace.Named(it) }.orElse(Namespace.OfThisServer),
        image = image(),
        pullPolicy = imagePullPolicy(),
        serviceAccount = serviceAccount(),
        priorityClass = priorityClass().map<PriorityClass> { PriorityClass.Named(it) }.orElse(PriorityClass.ClusterDefault),
        slot = ExecutionSlot(slotCores(), slotMemoryMb(), TimeCap.Unlimited),
        maxConcurrent = maxConcurrent(),
        scratchMb = scratchSizeMb(),
        ttl = ttl(),
        pendingTimeout = pendingTimeout(),
    )

fun DispatcherConfig.Slurm.toConfig(launcherLib: Path): SlurmDispatcherConfig {
    val user = required(user(), "user")
    return SlurmDispatcherConfig(
        ssh =
            SshTarget(
                host = required(host(), "host"),
                port = port(),
                user = user,
                identity = Path.of(home(required(identityFile(), "identity-file"))),
                knownHosts = Path.of(home(knownHosts())),
                jump = jumpHost().map { JumpHost.parse(it, user) }.orElse(JumpHost.Direct),
            ),
        remoteRoot = remoteRoot(),
        java = java(),
        launcherLib = launcherLib,
        partition = partition().map<Partition> { Partition.Named(it) }.orElse(Partition.ClusterDefault),
        sbatchOptions = sbatchOptions().orElse(emptyList()),
        slot =
            ExecutionSlot(
                cores = required(slotCores(), "slot-cores"),
                memoryMb = required(slotMemoryMb(), "slot-memory-mb"),
                timeCap = timeCap().map<TimeCap> { TimeCap.Limited(it.seconds.toInt()) }.orElse(TimeCap.Unlimited),
            ),
        maxJobs = maxJobs(),
        pendingTimeout = pendingTimeout(),
        pollInterval = pollInterval(),
        recordGrace = recordGrace(),
        cacheRetention = cacheRetention(),
    )
}

private fun <T : Any> required(
    value: Optional<T>,
    key: String,
): T = value.orElseThrow { IllegalStateException("opendc.dispatcher.slurm.$key must be set when opendc.execution.dispatcher=slurm") }

/** [path] with a leading `~/` read as this user's home. */
private fun home(path: String): String = if (path.startsWith("~/")) System.getProperty("user.home") + path.removePrefix("~") else path

/** The launcher's `lib` directory, refused at boot unless it holds the launcher. */
private fun DispatcherConfig.launcherDistribution(): Path {
    val lib = Path.of(launcherLib())
    check(lib.isDirectory() && lib.listDirectoryEntries("*.jar").isNotEmpty()) {
        "opendc.dispatcher.launcher-lib is $lib, which holds no launcher; build it with :opendc-web:opendc-web-launcher:installDist"
    }
    return lib
}

private fun machineMemoryMb(): Double =
    (ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean).totalMemorySize / BYTES_PER_MB
