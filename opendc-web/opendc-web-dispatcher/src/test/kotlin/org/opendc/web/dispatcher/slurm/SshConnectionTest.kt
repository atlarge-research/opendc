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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator

/**
 * The head node is reached with a key and verified against known hosts, often through a bastion.
 * A connection that trusted any host would hand the cluster account to whoever answered.
 */
class SshConnectionTest {
    @TempDir
    lateinit var dir: Path

    private val userKey = keyPair()
    private lateinit var head: FakeSlurm
    private lateinit var bastion: FakeSlurm
    private val connections = mutableListOf<SshConnection>()

    @BeforeEach
    fun start() {
        head = FakeSlurm(Files.createDirectories(dir.resolve("head")), USER, userKey, JAVA)
        bastion = FakeSlurm(Files.createDirectories(dir.resolve("bastion")), USER, userKey, JAVA)
    }

    @AfterEach
    fun stop() {
        connections.forEach { it.close() }
        head.close()
        bastion.close()
    }

    private fun connection(
        knownHosts: String,
        jump: JumpHost = JumpHost.Direct,
    ): SshConnection {
        val target =
            SshTarget(
                host = "127.0.0.1",
                port = head.port,
                user = USER,
                identity = writeIdentity(userKey, dir.resolve("id")),
                knownHosts = Files.writeString(dir.resolve("known_hosts"), knownHosts),
                jump = jump,
            )
        return SshConnection(target).also { connections += it }
    }

    @Test
    fun `refuses a head node whose key it was not told to trust`() {
        val stranger = keyPair()

        assertThrows<IOException> { connection(knownHostLine(stranger, head.port) + "\n").exec(listOf("squeue")) }
    }

    @Test
    fun `runs a command on the head node through a bastion`() {
        val trusted = knownHostLine(head.hostKey, head.port) + "\n" + knownHostLine(bastion.hostKey, bastion.port) + "\n"

        val result = connection(trusted, JumpHost.Via(USER, "127.0.0.1", bastion.port)).exec(listOf(JAVA, "-version"))

        assertEquals(0, result.exitCode)
        assertEquals(21, javaVersionOf(result.stderr))
    }

    // ssh-keygen makes Ed25519 keys by default, so an operator's deploy key is most likely one.
    @Test
    fun `signs in with an Ed25519 key`() {
        val ed25519 = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val cluster = FakeSlurm(Files.createDirectories(dir.resolve("ed25519")), USER, ed25519, JAVA)
        try {
            val target =
                SshTarget(
                    host = "127.0.0.1",
                    port = cluster.port,
                    user = USER,
                    identity = writeIdentity(ed25519, dir.resolve("id_ed25519")),
                    knownHosts = Files.writeString(dir.resolve("known_hosts"), knownHostLine(cluster.hostKey, cluster.port) + "\n"),
                    jump = JumpHost.Direct,
                )

            val result = SshConnection(target).also { connections += it }.exec(listOf(JAVA, "-version"))

            assertEquals(0, result.exitCode)
        } finally {
            cluster.close()
        }
    }

    @Test
    fun `reads a bastion written the way an operator writes one`() {
        assertEquals(JumpHost.Via("vunet", "ssh.data.vu.nl", 22), JumpHost.parse("vunet@ssh.data.vu.nl", "someone"))
        assertEquals(JumpHost.Via("someone", "bastion", 2222), JumpHost.parse("bastion:2222", "someone"))
    }

    private companion object {
        const val USER = "opendc"
        const val JAVA = "/opt/jdk-21/bin/java"
    }
}
