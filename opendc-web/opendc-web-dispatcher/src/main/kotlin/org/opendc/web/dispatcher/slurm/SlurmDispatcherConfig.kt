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

import org.opendc.web.dispatcher.ExecutionSlot
import java.nio.file.Path
import java.time.Duration

/** The partition jobs are submitted to. */
sealed interface Partition {
    data object ClusterDefault : Partition

    data class Named(val name: String) : Partition
}

/**
 * How executions are run on a SLURM cluster reached over SSH.
 *
 * @property remoteRoot Where everything is kept on the cluster's shared filesystem.
 * @property java The JRE the compute nodes run the launcher with, Java 21 or newer.
 * @property launcherLib The `lib` directory of the launcher distribution this server ships.
 * @property sbatchOptions Site flags, such as an account or a QOS, passed to every submission.
 * @property slot One node's cores and memory, and the longest a job may run here.
 * @property maxJobs How many jobs may be pending or running at once.
 * @property pendingTimeout How long a job may wait to be accepted or started before it is withdrawn.
 * @property pollInterval How often the queue is asked about. Mind the site's etiquette.
 * @property recordGrace How long a job gone from the queue is given for its exit record to show up
 *           on the shared filesystem, whose attribute caching can hide a fresh file for a while.
 * @property cacheRetention How long a cached trace or an old launcher is kept after its last use.
 */
data class SlurmDispatcherConfig(
    val ssh: SshTarget,
    val remoteRoot: String,
    val java: String,
    val launcherLib: Path,
    val partition: Partition,
    val sbatchOptions: List<String>,
    val slot: ExecutionSlot,
    val maxJobs: Int,
    val pendingTimeout: Duration,
    val pollInterval: Duration,
    val recordGrace: Duration,
    val cacheRetention: Duration,
)
