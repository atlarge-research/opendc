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

import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.client.config.hosts.HostConfigEntry
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.keyprovider.KeyIdentityProvider
import org.apache.sshd.common.util.security.SecurityUtils
import org.apache.sshd.core.CoreModuleProperties
import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.client.SftpClientFactory
import org.apache.sshd.sftp.common.SftpException
import org.opendc.web.dispatcher.NO_EXIT_CODE
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.EnumSet

private val CONNECT_TIMEOUT = Duration.ofSeconds(20)
private val AUTH_TIMEOUT = Duration.ofSeconds(20)
private val COMMAND_TIMEOUT = Duration.ofMinutes(2)
private val HEARTBEAT = Duration.ofSeconds(30)
private const val DEFAULT_PORT = 22

/** How the head node is reached. */
data class SshTarget(
    val host: String,
    val port: Int,
    val user: String,
    val identity: Path,
    val knownHosts: Path,
    val jump: JumpHost,
)

/** A bastion the head node is reached through, if any. */
sealed interface JumpHost {
    data object Direct : JumpHost

    data class Via(
        val user: String,
        val host: String,
        val port: Int,
    ) : JumpHost

    companion object {
        /** Reads `[user@]host[:port]`, with [defaultUser] where no user is named. */
        fun parse(
            spec: String,
            defaultUser: String,
        ): JumpHost {
            val user = spec.substringBefore('@', defaultUser)
            val address = spec.substringAfter('@')
            val host = address.substringBefore(':')
            val port = address.substringAfter(':', "$DEFAULT_PORT").toInt()
            return Via(user, host, port)
        }
    }
}

/** What a remote command printed and exited with. */
data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

/**
 * One SSH session to a head node, opened when first needed and opened again after it breaks.
 *
 * Host keys are verified strictly against the configured known-hosts file, for the jump host as well
 * as the head node, and nothing is read from this machine's own SSH configuration, so what connects
 * is decided by the deployment's settings alone. Commands are run with every argument single-quoted:
 * nothing a scenario or a path contains is ever interpreted by the remote shell.
 */
class SshConnection(private val target: SshTarget) : AutoCloseable {
    private val client: SshClient =
        SshClient.setUpDefaultClient().apply {
            keyIdentityProvider = KeyIdentityProvider.wrapKeyPairs(identity(target.identity))
            serverKeyVerifier = KnownHostsServerKeyVerifier(RejectAllServerKeyVerifier.INSTANCE, target.knownHosts)
            hostConfigEntryResolver = HostConfigEntryResolver.EMPTY
            CoreModuleProperties.HEARTBEAT_INTERVAL.set(this, HEARTBEAT)
            start()
        }

    private var link: Link = Link.Closed

    /** Runs [command] remotely and waits for it to exit. A broken session is replaced on the next call. */
    @Synchronized
    fun exec(command: List<String>): CommandResult {
        val session = session()
        try {
            val out = ByteArrayOutputStream()
            val err = ByteArrayOutputStream()
            session.createExecChannel(command.joinToString(" ") { quote(it) }).use { channel ->
                channel.out = out
                channel.err = err
                channel.open().verify(COMMAND_TIMEOUT)
                channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), COMMAND_TIMEOUT)
                return CommandResult(channel.exitStatus ?: NO_EXIT_CODE, out.toString(Charsets.UTF_8), err.toString(Charsets.UTF_8))
            }
        } catch (e: IOException) {
            drop()
            throw e
        }
    }

    /**
     * Runs [block] with an SFTP channel of its own, closed afterwards.
     *
     * An SFTP status error, such as a missing file, is the server answering, not the connection
     * failing, so only a failure of the transport itself replaces the session other channels share.
     */
    fun <T> sftp(block: (SftpClient) -> T): T {
        val session = synchronized(this) { session() }
        try {
            return SftpClientFactory.instance().createSftpClient(session).use(block)
        } catch (e: SftpException) {
            throw e
        } catch (e: IOException) {
            synchronized(this) { drop() }
            throw e
        }
    }

    override fun close() {
        synchronized(this) { drop() }
        client.stop()
    }

    private fun session(): ClientSession {
        when (val current = link) {
            is Link.Open -> if (current.session.isOpen) return current.session
            Link.Closed -> {}
        }
        val jump =
            when (val via = target.jump) {
                JumpHost.Direct -> null
                is JumpHost.Via -> "${via.user}@${via.host}:${via.port}"
            }
        val opened =
            client
                .connect(HostConfigEntry("", target.host, target.port, target.user, jump), null, null)
                .verify(CONNECT_TIMEOUT)
                .session
        try {
            opened.auth().verify(AUTH_TIMEOUT)
        } catch (e: IOException) {
            opened.close()
            throw e
        }
        link = Link.Open(opened)
        return opened
    }

    private fun drop() {
        when (val current = link) {
            is Link.Open -> current.session.close()
            Link.Closed -> {}
        }
        link = Link.Closed
    }

    /** Whether a session is held. */
    private sealed interface Link {
        data object Closed : Link

        data class Open(val session: ClientSession) : Link
    }

    private companion object {
        fun identity(file: Path) =
            Files.newInputStream(file).use { input ->
                SecurityUtils.loadKeyPairIdentities(null, NamedResource.ofName(file.toString()), input, null)
            }

        /** One argument as the remote shell reads it back, whatever it contains. */
        fun quote(argument: String): String = "'" + argument.replace("'", "'\\''") + "'"
    }
}
