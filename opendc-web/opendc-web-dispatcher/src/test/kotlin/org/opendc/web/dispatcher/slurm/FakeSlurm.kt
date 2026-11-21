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

import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.config.keys.PublicKeyEntry
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.channel.ChannelSession
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.command.CommandFactory
import org.apache.sshd.server.forward.AcceptAllForwardingFilter
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** A fresh RSA key pair, which the SSH library reads without anything beyond the JDK. */
internal fun keyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

/** [keys]' private key as a PKCS#8 PEM file, the way an operator would hand one over. */
internal fun writeIdentity(
    keys: KeyPair,
    file: Path,
): Path {
    val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keys.private.encoded)
    return Files.writeString(file, "-----BEGIN PRIVATE KEY-----\n$body\n-----END PRIVATE KEY-----\n")
}

/** A known-hosts line vouching for [server]'s host key at [port] on the loopback address. */
internal fun knownHostLine(
    server: KeyPair,
    port: Int,
): String = "[127.0.0.1]:$port ${PublicKeyEntry.toString(server.public)}"

/**
 * A SLURM head node in process: an SSH server with SFTP over a temporary directory and a command
 * table that plays sbatch, squeue, scancel, sinfo, rm and `java -version`. Nothing is ever run; a
 * case moves a job along with [start], [finish], [vanish] and [pend], and plays the launcher with
 * [certify].
 */
internal class FakeSlurm(
    private val root: Path,
    private val user: String,
    authorized: KeyPair,
    private val java: String,
) : AutoCloseable {
    val hostKey: KeyPair = keyPair()

    /** What sbatch was called with, in order. */
    val submissions = CopyOnWriteArrayList<List<String>>()

    /** The executions scancel was asked to stop. */
    val cancelled = CopyOnWriteArrayList<UUID>()

    @Volatile
    var javaVersion = "21.0.2"

    @Volatile
    var nodes = "defq*|node001|0/16/0/16|56000|56000|idle|15:00"

    private val jobs = ConcurrentHashMap<UUID, Job>()
    private val refusals = ConcurrentLinkedQueue<String>()
    private val ids = AtomicInteger(1000)

    private val server: SshServer =
        SshServer.setUpDefaultServer().apply {
            host = "127.0.0.1"
            port = 0
            keyPairProvider = KeyPairProvider.wrap(hostKey)
            publickeyAuthenticator =
                PublickeyAuthenticator { name, key, _ ->
                    name == user && KeyUtils.compareKeys(key, authorized.public)
                }
            subsystemFactories = listOf(SftpSubsystemFactory())
            fileSystemFactory = VirtualFileSystemFactory(root)
            commandFactory = CommandFactory { _, command -> Exec(arguments(command)) }
            forwardingFilter = AcceptAllForwardingFilter.INSTANCE
            start()
        }

    val port: Int get() = server.port

    /** Where an execution's directory lies, under a remote root of `opendc`. */
    fun directory(id: UUID): Path = root.resolve("opendc/executions/$id")

    fun queued(id: UUID): Boolean = jobs.containsKey(id)

    /** The wrapper has started the launcher. */
    fun start(id: UUID) {
        record(id, Record.STARTED, "at=${Instant.now().epochSecond}")
        jobs.computeIfPresent(id) { _, job -> job.copy(state = "RUNNING", reason = "None") }
    }

    /** The job left the queue with the wrapper's [exit] record, and SIGTERM if [terminated]. */
    fun finish(
        id: UUID,
        exit: Int,
        terminated: Boolean = false,
    ) {
        if (terminated) record(id, Record.TERMINATED, "")
        record(id, Record.EXIT, "code=$exit\nended=${Instant.now().epochSecond}")
        jobs.remove(id)
    }

    /** The job left the queue without a word. */
    fun vanish(id: UUID) {
        jobs.remove(id)
    }

    fun pend(
        id: UUID,
        reason: String,
    ) {
        jobs.computeIfPresent(id) { _, job -> job.copy(state = "PENDING", reason = reason) }
    }

    /** The next sbatch fails with [stderr]. */
    fun refuseNextSubmit(stderr: String) {
        refusals += stderr
    }

    /** Plays the launcher of unit [index]: writes its [files] and then its [outcome] marker. */
    fun certify(
        id: UUID,
        index: Int,
        outcome: String,
        files: Map<String, String> = emptyMap(),
    ) {
        val directory = directory(id)
        for ((name, content) in files) {
            Files.createDirectories(directory.resolve("outputs/$index"))
            Files.writeString(directory.resolve("outputs/$index/$name"), content)
        }
        Files.createDirectories(directory.resolve("units"))
        Files.writeString(directory.resolve("units/$index.json"), outcome)
    }

    override fun close() {
        server.stop(true)
    }

    private fun record(
        id: UUID,
        name: String,
        text: String,
    ) {
        Files.createDirectories(directory(id))
        Files.writeString(directory(id).resolve(name), "$text\n")
    }

    private fun run(argv: List<String>): CommandResult =
        when {
            argv.first() == java && argv.getOrNull(1) == "-version" -> CommandResult(0, "", "openjdk version \"$javaVersion\" 2024-01-16\n")
            argv.first() == "sbatch" -> sbatch(argv)
            argv.first() == "squeue" ->
                CommandResult(0, jobs.entries.joinToString("\n") { (id, job) -> "opendc-$id|${job.id}|${job.state}|${job.reason}" }, "")
            argv.first() == "scancel" -> {
                val id = UUID.fromString(argv.first { it.startsWith("--name=") }.removePrefix("--name=opendc-"))
                cancelled += id
                jobs.remove(id)
                CommandResult(0, "", "")
            }
            argv.first() == "sinfo" -> CommandResult(0, nodes, "")
            argv.first() == "rm" -> {
                root.resolve(argv.last().removePrefix("/")).toFile().deleteRecursively()
                CommandResult(0, "", "")
            }
            else -> CommandResult(127, "", "${argv.first()}: command not found")
        }

    private fun sbatch(argv: List<String>): CommandResult {
        refusals.poll()?.let { return CommandResult(1, "", it) }
        submissions += argv
        val id = UUID.fromString(argv.first { it.startsWith("--job-name=") }.removePrefix("--job-name=opendc-"))
        val job = Job("${ids.incrementAndGet()}", "PENDING", "Priority")
        jobs[id] = job
        return CommandResult(0, "${job.id}\n", "")
    }

    /** Splits a command line whose every argument is single-quoted, as the dispatcher sends them. */
    private fun arguments(command: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var started = false
        var index = 0
        while (index < command.length) {
            val char = command[index]
            when {
                char == '\'' -> {
                    quoted = !quoted
                    started = true
                }
                char == '\\' && !quoted && index + 1 < command.length -> {
                    current.append(command[++index])
                    started = true
                }
                char == ' ' && !quoted -> {
                    if (started) result += current.toString()
                    current.clear()
                    started = false
                }
                else -> {
                    current.append(char)
                    started = true
                }
            }
            index++
        }
        if (started) result += current.toString()
        return result
    }

    private data class Job(
        val id: String,
        val state: String,
        val reason: String,
    )

    private inner class Exec(private val argv: List<String>) : Command {
        private lateinit var out: OutputStream
        private lateinit var err: OutputStream
        private lateinit var exit: ExitCallback

        override fun setInputStream(input: InputStream) {}

        override fun setOutputStream(out: OutputStream) {
            this.out = out
        }

        override fun setErrorStream(err: OutputStream) {
            this.err = err
        }

        override fun setExitCallback(callback: ExitCallback) {
            exit = callback
        }

        override fun start(
            channel: ChannelSession,
            env: Environment,
        ) {
            Thread {
                val result = run(argv)
                out.write(result.stdout.toByteArray())
                out.flush()
                err.write(result.stderr.toByteArray())
                err.flush()
                exit.onExit(result.exitCode)
            }.start()
        }

        override fun destroy(channel: ChannelSession) {}
    }
}
