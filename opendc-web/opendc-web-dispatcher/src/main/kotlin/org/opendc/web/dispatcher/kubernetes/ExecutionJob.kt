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

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder
import org.opendc.web.dispatcher.EXIT_ON_OUT_OF_MEMORY
import org.opendc.web.dispatcher.LaunchRequest
import org.opendc.web.dispatcher.jobName
import org.opendc.web.launcher.MANIFEST_URL_VARIABLE
import org.opendc.web.launcher.PEAK_MEMORY_FILE
import java.util.UUID

/** Marks every object this server creates, which is what its informers and listings select on. */
const val MANAGED_BY = "opendc-web"

const val MANAGED_BY_LABEL = "app.kubernetes.io/managed-by"

/** Names the execution an object belongs to. The public id is the only handle there is. */
const val EXECUTION_LABEL = "opendc.org/execution"

/** Why this server stopped a Job, written onto it before deletion so any replica reads the same verdict. */
const val STOP_REASON = "opendc.org/stop-reason"

const val STOP_MESSAGE = "opendc.org/stop-message"

/** The one writable volume: staging, output and the peak report all live there. */
private const val SCRATCH = "/tmp"

private const val SCRATCH_VOLUME = "scratch"

private const val CONTAINER = "launcher"

/** How long a stopped launcher gets to exit by itself. */
private const val GRACE_SECONDS = 10L

/** What the image's start script reads its JVM options from. */
private const val LAUNCHER_OPTS = "OPENDC_LAUNCHER_OPTS"

/** The execution [this] belongs to. Only objects selected by [MANAGED_BY] are ever asked. */
fun HasMetadata.executionId(): UUID = UUID.fromString(metadata.labels.getValue(EXECUTION_LABEL))

/**
 * The Job that runs [request]'s execution.
 *
 * One pod, never restarted and never retried by the Job controller: every retry is a new execution
 * with a fresh manifest. Requests equal limits, so the pod is Guaranteed and the JVM sees exactly the
 * cores its units are parallel over. The deadline is the pod's, so time spent queued does not count
 * against it. The pod talks to no API, so it carries no token and runs locked down.
 */
fun executionJob(
    request: LaunchRequest,
    config: KubernetesDispatcherConfig,
): Job {
    val labels =
        mapOf(
            "app.kubernetes.io/name" to "opendc-launcher",
            MANAGED_BY_LABEL to MANAGED_BY,
            EXECUTION_LABEL to request.executionId.toString(),
        )
    val grant = request.grant
    val scratch = Quantity("${config.scratchMb}Mi")
    return JobBuilder()
        .withNewMetadata()
        .withName(jobName(request.executionId))
        .withLabels<String, String>(labels)
        .endMetadata()
        .withNewSpec()
        .withBackoffLimit(0)
        .withTtlSecondsAfterFinished(config.ttl.seconds.toInt())
        .withNewTemplate()
        .withNewMetadata()
        .withLabels<String, String>(labels)
        .endMetadata()
        .withNewSpec()
        .withRestartPolicy("Never")
        .withActiveDeadlineSeconds(grant.timeLimitSeconds.toLong())
        .withTerminationGracePeriodSeconds(GRACE_SECONDS)
        .withAutomountServiceAccountToken(false)
        .withEnableServiceLinks(false)
        .withServiceAccountName(config.serviceAccount)
        .withPriorityClassName(
            when (val priority = config.priorityClass) {
                PriorityClass.ClusterDefault -> null
                is PriorityClass.Named -> priority.name
            },
        ).withNewSecurityContext()
        .withRunAsNonRoot(true)
        .withNewSeccompProfile()
        .withType("RuntimeDefault")
        .endSeccompProfile()
        .endSecurityContext()
        .addNewContainer()
        .withName(CONTAINER)
        .withImage(config.image)
        .withImagePullPolicy(config.pullPolicy.wire)
        .withWorkingDir(SCRATCH)
        .withTerminationMessagePath("$SCRATCH/$PEAK_MEMORY_FILE")
        .withTerminationMessagePolicy("File")
        .addNewEnv()
        .withName(MANIFEST_URL_VARIABLE)
        .withValue(request.manifestUrl)
        .endEnv()
        .addNewEnv()
        .withName(LAUNCHER_OPTS)
        .withValue("-Xmx${grant.heapMb}m $EXIT_ON_OUT_OF_MEMORY")
        .endEnv()
        .withNewResources()
        .addToRequests("cpu", Quantity("${grant.parallelism}"))
        .addToRequests("memory", Quantity("${grant.memoryRequestMb}Mi"))
        .addToRequests("ephemeral-storage", scratch)
        .addToLimits("cpu", Quantity("${grant.parallelism}"))
        .addToLimits("memory", Quantity("${grant.memoryRequestMb}Mi"))
        .endResources()
        .withNewSecurityContext()
        .withAllowPrivilegeEscalation(false)
        .withReadOnlyRootFilesystem(true)
        .withNewCapabilities()
        .addToDrop("ALL")
        .endCapabilities()
        .endSecurityContext()
        .addNewVolumeMount()
        .withName(SCRATCH_VOLUME)
        .withMountPath(SCRATCH)
        .endVolumeMount()
        .endContainer()
        .addNewVolume()
        .withName(SCRATCH_VOLUME)
        .withNewEmptyDir()
        .withSizeLimit(scratch)
        .endEmptyDir()
        .endVolume()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build()
}
