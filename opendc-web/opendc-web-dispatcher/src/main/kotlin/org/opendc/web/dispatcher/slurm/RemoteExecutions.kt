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

package org.opendc.web.dispatcher.slurm

import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.common.SftpConstants
import org.apache.sshd.sftp.common.SftpException
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.web.dispatcher.LOG_TAIL_BYTES
import org.opendc.web.dispatcher.logTail
import org.opendc.web.launcher.LaunchManifest
import org.opendc.web.launcher.Payload
import org.opendc.web.launcher.StagedInput
import org.opendc.web.launcher.TelemetryTarget
import org.opendc.web.launcher.fetch
import org.opendc.web.launcher.publish
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/** A store key names bytes under the cache, so it may not climb out of it. */
private val CACHE_KEY = Regex("""^[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*$""")

private const val WRAPPER = "wrapper.sh"

private const val WRAPPER_RESOURCE = "/org/opendc/web/dispatcher/slurm/wrapper.sh"

private const val ORIGINAL_MANIFEST = "manifest.original.json"

private const val MANIFEST = "manifest.json"

private const val OUTPUT = "slurm.out"

private const val OWNER_ONLY = 384 // 0600

private const val EXECUTABLE = 493 // 0755

/** How many hex digits of a launcher's content hash name its directory. */
private const val HASH_DIGITS = 16

/**
 * Where everything lives under one root on the cluster's shared filesystem.
 *
 * Launchers are kept by the hash of their content, so a queued job keeps the launcher it was staged
 * with when a newer server stages another. Traces are cached by their store key, which names bytes
 * that never change. Each execution has a directory of its own.
 */
internal class RemoteLayout(val root: String) {
    fun launcher(hash: String): String = "$root/launchers/$hash"

    fun cached(key: String): String = "$root/cache/$key"

    fun executions(): String = "$root/executions"

    fun execution(executionId: UUID): String = "${executions()}/$executionId"
}

/**
 * [this] manifest as a job on the cluster reads it: inputs from the cache, outputs and outcomes into
 * the execution's own directory, and no telemetry, since compute nodes cannot reach the server. Units
 * are matched by position, so nothing about a scenario has to be read to rewrite it.
 */
internal fun LaunchManifest.localized(
    layout: RemoteLayout,
    executionId: UUID,
): LaunchManifest {
    val directory = layout.execution(executionId)
    return copy(
        inputs = inputs.map { it.copy(source = "file://${layout.cached(it.key)}") },
        units =
            units.mapIndexed { index, unit ->
                unit.copy(
                    outputs = unit.outputs.map { it.copy(target = "file://$directory/outputs/$index/${it.file}") },
                    outcome = "file://$directory/units/$index.json",
                )
            },
        telemetry = TelemetryTarget.None,
    )
}

/**
 * The launcher this server ships, as it is put on the cluster: its `lib` directory and the wrapper
 * that runs it, under a name derived from their content.
 */
internal class LauncherDistribution(private val lib: Path) {
    val wrapper: ByteArray =
        checkNotNull(javaClass.getResourceAsStream(WRAPPER_RESOURCE)) { "the wrapper is missing" }.use { it.readBytes() }

    val jars: List<Path> = lib.listDirectoryEntries("*.jar").sortedBy { it.name }

    /** The first digits of a SHA-256 over every file's name and bytes, the wrapper included. */
    val hash: String =
        MessageDigest
            .getInstance("SHA-256")
            .apply {
                for (jar in jars) {
                    update(jar.name.toByteArray())
                    update(Files.readAllBytes(jar))
                }
                update(WRAPPER.toByteArray())
                update(wrapper)
            }.digest()
            .joinToString("") { "%02x".format(it) }
            .take(HASH_DIGITS)
}

/**
 * What one dispatcher does on the cluster's filesystem, through one SSH connection.
 *
 * Anything that appears under its final name appears whole: it is written under a dot-prefixed name
 * beside its destination and renamed into place, and the rename is the only completion marker there is.
 */
internal class RemoteExecutions(
    private val ssh: SshConnection,
    val layout: RemoteLayout,
    private val distribution: LauncherDistribution,
) {
    /** Whether this process has already made sure its launcher is on the cluster. */
    @Volatile
    private var launcherPresent = false

    /** One lock per cache key being staged, so the same bytes are fetched once. */
    private val staging = ConcurrentHashMap<String, Any>()

    /**
     * Where the launcher this server ships lives on the cluster, uploaded first if it is not there yet.
     * Done once per process, however many executions are staged at once.
     */
    @Synchronized
    fun launcher(): String {
        val target = layout.launcher(distribution.hash)
        if (launcherPresent) return target
        ssh.sftp { sftp ->
            if (!sftp.exists(target)) {
                val staging = "${layout.root}/launchers/.${distribution.hash}.${UUID.randomUUID()}"
                sftp.mkdirs("$staging/lib")
                for (jar in distribution.jars) {
                    sftp.write("$staging/lib/${jar.name}").use { out -> Files.newInputStream(jar).use { it.copyTo(out) } }
                }
                sftp.write("$staging/$WRAPPER").use { it.write(distribution.wrapper) }
                sftp.setStat("$staging/$WRAPPER", SftpClient.Attributes().perms(EXECUTABLE))
                if (!sftp.renameUnlessPresent(staging, target)) {
                    ssh.exec(listOf("rm", "-rf", "--", staging))
                }
            }
        }
        launcherPresent = true
        return target
    }

    /**
     * Puts [input] in the cache unless it is there already, in which case it is marked as used. Two
     * executions staging the same input at once fetch it once: the second waits for the first.
     */
    fun stage(input: StagedInput) {
        require(CACHE_KEY.matches(input.key) && input.key.split('/').none { it == "." || it == ".." }) {
            "'${input.key}' cannot name a cached input"
        }
        val target = layout.cached(input.key)
        synchronized(staging.computeIfAbsent(input.key) { Any() }) { cache(input, target) }
    }

    private fun cache(
        input: StagedInput,
        target: String,
    ) {
        ssh.sftp { sftp ->
            if (sftp.exists(target)) {
                sftp.setStat(target, SftpClient.Attributes().modifyTime(Instant.now().epochSecond, TimeUnit.SECONDS))
                return@sftp
            }
            val directory = target.substringBeforeLast('/')
            sftp.mkdirs(directory)
            val part = "$directory/.${target.substringAfterLast('/')}.${UUID.randomUUID()}.part"
            fetch(input.source) { body -> sftp.write(part).use { body.copyTo(it) } }
            if (!sftp.renameUnlessPresent(part, target)) sftp.remove(part)
        }
    }

    /** Writes an execution's directory: the manifest its job reads, the original it was given, and its limits. */
    fun prepare(
        executionId: UUID,
        localized: LaunchManifest,
        original: LaunchManifest,
        limits: Limits,
    ) {
        val directory = layout.execution(executionId)
        ssh.sftp { sftp ->
            sftp.mkdirs(directory)
            sftp.write("$directory/$MANIFEST").use { it.write(encode(localized)) }
            sftp.write("$directory/$ORIGINAL_MANIFEST").use { it.write(encode(original)) }
            // The original holds the signed targets the results go back through.
            sftp.setStat("$directory/$ORIGINAL_MANIFEST", SftpClient.Attributes().perms(OWNER_ONLY))
            sftp.writeRecord("$directory/${Record.LIMITS}", "time-limit=${limits.timeLimitSeconds}\ncapped=${limits.capped}")
        }
    }

    fun writeRecord(
        executionId: UUID,
        name: String,
        lines: List<String>,
    ) {
        ssh.sftp { it.writeRecord("${layout.execution(executionId)}/$name", lines.joinToString("\n")) }
    }

    fun exists(executionId: UUID): Boolean = ssh.sftp { it.exists(layout.execution(executionId)) }

    /** Everything the execution's directory says about how its job went. */
    fun records(executionId: UUID): JobRecords {
        val directory = layout.execution(executionId)
        return ssh.sftp { sftp ->
            val names = listOf(Record.LIMITS, Record.STOP, Record.STARTED, Record.TERMINATED, Record.EXIT, Record.PEAK_MEMORY)
            val files = names.filter { sftp.exists("$directory/$it") }.associateWith { sftp.readText("$directory/$it") }
            val output = "$directory/$OUTPUT"
            val lastOutput = if (sftp.exists(output)) sftp.stat(output).modifyTime.toInstant() else Instant.EPOCH
            records(files, lastOutput)
        }
    }

    /** When the wrapper recorded the job starting, if it has. */
    fun startedAt(executionId: UUID): Start = records(executionId).start

    /** When this dispatcher recorded submitting the job, if it has. */
    fun submittedAt(executionId: UUID): Instant {
        val file = "${layout.execution(executionId)}/${Record.SUBMITTED}"
        return ssh.sftp { sftp ->
            if (!sftp.exists(file)) return@sftp Instant.now()
            val at = sftp.readText(file).lineSequence().firstOrNull { it.startsWith("at=") }?.substringAfter('=')?.toLongOrNull()
            at?.let(Instant::ofEpochSecond) ?: Instant.now()
        }
    }

    /** The end of what the job printed. */
    fun logTail(executionId: UUID): String {
        val output = "${layout.execution(executionId)}/$OUTPUT"
        return ssh.sftp { sftp ->
            if (!sftp.exists(output)) return@sftp ""
            val size = sftp.stat(output).size
            sftp.read(output).use { input ->
                input.skipNBytes((size - LOG_TAIL_BYTES).coerceAtLeast(0))
                logTail(input.readBytes().decodeToString())
            }
        }
    }

    /**
     * Copies back every unit that certified its outcome: its files first, then its marker, each to the
     * signed target the server gave it. A unit without a marker did not finish and is not copied.
     */
    fun collect(executionId: UUID) {
        val directory = layout.execution(executionId)
        ssh.sftp { sftp ->
            val original = SdkJson.json.decodeFromString(LaunchManifest.serializer(), sftp.readText("$directory/$ORIGINAL_MANIFEST"))
            for ((index, unit) in original.units.withIndex()) {
                val marker = "$directory/units/$index.json"
                if (!sftp.exists(marker)) continue
                for (output in unit.outputs) {
                    val file = "$directory/outputs/$index/${output.file}"
                    if (sftp.exists(file)) {
                        publish(Payload(sftp.stat(file).size) { sftp.read(file) }, output.target)
                    }
                }
                publish(Payload(sftp.stat(marker).size) { sftp.read(marker) }, unit.outcome)
            }
        }
    }

    fun remove(executionId: UUID) {
        ssh.exec(listOf("rm", "-rf", "--", layout.execution(executionId)))
    }

    /** The executions with a directory here. */
    fun executionIds(): List<UUID> =
        ssh.sftp { sftp ->
            if (!sftp.exists(layout.executions())) return@sftp emptyList()
            sftp.readDir(layout.executions()).mapNotNull { runCatching { UUID.fromString(it.filename) }.getOrNull() }
        }

    /** Removes cached traces and other launchers nobody has used for longer than [retention]. */
    fun sweep(retention: Duration) {
        val cutoff = Instant.now().minus(retention)
        ssh.sftp { sftp ->
            sftp.removeOlderThan("${layout.root}/cache", cutoff)
            val launchers = "${layout.root}/launchers"
            if (sftp.exists(launchers)) {
                for (entry in sftp.readDir(launchers)) {
                    val name = entry.filename
                    if (name == "." || name == ".." || name == distribution.hash) continue
                    if (entry.attributes.modifyTime.toInstant().isBefore(cutoff)) {
                        ssh.exec(listOf("rm", "-rf", "--", "$launchers/$name"))
                    }
                }
            }
        }
    }

    private fun encode(manifest: LaunchManifest): ByteArray =
        SdkJson.json.encodeToString(LaunchManifest.serializer(), manifest).encodeToByteArray()
}

internal fun SftpClient.exists(path: String): Boolean =
    try {
        stat(path)
        true
    } catch (e: SftpException) {
        if (e.status != SftpConstants.SSH_FX_NO_SUCH_FILE) throw e
        false
    }

/**
 * Renames [from] to [to], or leaves [from] where it is when [to] already exists, which another staging
 * of the same content got to first. Returns whether the rename happened.
 */
internal fun SftpClient.renameUnlessPresent(
    from: String,
    to: String,
): Boolean =
    try {
        if (exists(to)) {
            false
        } else {
            rename(from, to, emptyList())
            true
        }
    } catch (e: SftpException) {
        if (!exists(to)) throw e
        false
    }

/** Creates [path] and every directory above it that is missing. */
internal fun SftpClient.mkdirs(path: String) {
    val absolute = path.startsWith('/')
    var current = if (absolute) "" else "."
    for (segment in path.split('/').filter { it.isNotEmpty() }) {
        current = "$current/$segment"
        if (exists(current)) continue
        try {
            mkdir(current)
        } catch (e: SftpException) {
            // Another staging may have made it in between.
            if (!exists(current)) throw e
        }
    }
}

private fun SftpClient.readText(path: String): String = read(path).use { it.readBytes().decodeToString() }

/** Writes a record whole: beside it under a dot-prefixed name, then renamed over it. */
private fun SftpClient.writeRecord(
    path: String,
    text: String,
) {
    val part = "${path.substringBeforeLast('/')}/.${path.substringAfterLast('/')}.part"
    write(part).use { it.write("$text\n".encodeToByteArray()) }
    if (exists(path)) remove(path)
    rename(part, path, emptyList())
}

private fun SftpClient.removeOlderThan(
    directory: String,
    cutoff: Instant,
) {
    if (!exists(directory)) return
    for (entry in readDir(directory)) {
        val name = entry.filename
        if (name == "." || name == "..") continue
        val path = "$directory/$name"
        if (entry.attributes.isDirectory) {
            removeOlderThan(path, cutoff)
        } else if (entry.attributes.modifyTime.toInstant().isBefore(cutoff)) {
            remove(path)
        }
    }
}
