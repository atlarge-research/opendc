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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * The wrapper is the only witness of how a SLURM job ended on a cluster that keeps no accounting, so
 * what it records has to be right: the exit code, a SIGTERM, and readings from this job's own cgroup
 * and no other. It only ever runs on cluster nodes, so it is tested where bash is.
 */
@EnabledOnOs(OS.LINUX, OS.MAC)
class WrapperScriptTest {
    @TempDir
    lateinit var dir: Path

    private fun wrapper(): Path {
        val script = dir.resolve("wrapper.sh")
        Files.write(script, checkNotNull(javaClass.getResourceAsStream("/org/opendc/web/dispatcher/slurm/wrapper.sh")).readBytes())
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"))
        return script
    }

    /** A `java` that writes down how it was called, then sleeps for [sleep] seconds and exits with [exit]. */
    private fun java(
        exit: Int,
        sleep: Int = 0,
    ): Path {
        val stub = dir.resolve("java")
        Files.writeString(stub, "#!/bin/bash\necho \"MANIFEST_URL=\$MANIFEST_URL \$*\" > called\nsleep $sleep\nexit $exit\n")
        Files.setPosixFilePermissions(stub, PosixFilePermissions.fromString("rwxr-xr-x"))
        return stub
    }

    /** A cgroup tree in which job 42 peaked at 1 GiB and was hit by the out-of-memory killer once. */
    private fun cgroups(): Pair<Path, Path> {
        val root = Files.createDirectories(dir.resolve("cgroup"))
        val job = Files.createDirectories(root.resolve("slurm/uid_1000/job_42/step_batch"))
        Files.writeString(job.resolve("memory.peak"), "1073741824\n")
        Files.writeString(job.resolve("memory.events"), "low 0\nhigh 0\nmax 3\noom 1\noom_kill 1\n")
        val foreign = Files.createDirectories(root.resolve("system.slice/slurmd.service"))
        Files.writeString(foreign.resolve("memory.peak"), "999\n")
        val self = Files.writeString(dir.resolve("self-cgroup"), "0::/slurm/uid_1000/job_42/step_batch\n")
        return root to self
    }

    private fun run(
        java: Path,
        job: String,
    ): Process {
        val (root, self) = cgroups()
        val work = Files.createDirectories(dir.resolve("work"))
        val launcher = dir.resolve("launcher").toString()
        return ProcessBuilder(wrapper().toString(), launcher, java.toString(), "1536", "org.opendc.web.launcher.MainKt")
            .directory(work.toFile())
            .redirectErrorStream(true)
            .also {
                it.environment()["SLURM_JOB_ID"] = job
                it.environment()["OPENDC_CGROUP_ROOT"] = root.toString()
                it.environment()["OPENDC_SELF_CGROUP"] = self.toString()
            }.start()
    }

    private fun fields(file: Path): Map<String, String> =
        file.readText().lines().filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }

    @Test
    fun `passes the launcher's exit code through and records when it started and ended`() {
        val process = run(java(exit = 23), job = "42")

        assertEquals(23, process.waitFor().also { process.waitFor(10, TimeUnit.SECONDS) })
        val work = dir.resolve("work")
        assertTrue(work.resolve("started").exists())
        val exit = fields(work.resolve("exit"))
        assertEquals("23", exit["code"])
        assertEquals("1073741824", exit["memory_peak"], "the reading comes from job 42's cgroup, not slurmd's")
        assertEquals("1", exit["oom_kill"])
        val called = work.resolve("called").readText()
        assertTrue("-Xmx1536m" in called && "-XX:+ExitOnOutOfMemoryError" in called, called)
        assertTrue("-cp ${dir.resolve("launcher")}/lib/*" in called, called)
        assertTrue("MANIFEST_URL=file://$work/manifest.json" in called, called)
    }

    @Test
    fun `reads nothing from a cgroup that is not this job's`() {
        val process = run(java(exit = 0), job = "7")

        assertEquals(0, process.waitFor())
        val exit = fields(dir.resolve("work/exit"))
        assertTrue("memory_peak" !in exit && "oom_kill" !in exit, "$exit")
    }

    @Test
    fun `records a SIGTERM and passes it on to the launcher`() {
        val process = run(java(exit = 0, sleep = 30), job = "42")
        val work = dir.resolve("work")
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!work.resolve("called").exists() && System.nanoTime() < deadline) Thread.sleep(50)

        ProcessBuilder("kill", "-TERM", process.pid().toString()).start().waitFor()

        assertTrue(process.waitFor(20, TimeUnit.SECONDS), "the wrapper exits once the launcher has")
        assertTrue(work.resolve("terminated").exists())
        assertEquals("143", fields(work.resolve("exit"))["code"])
    }
}
