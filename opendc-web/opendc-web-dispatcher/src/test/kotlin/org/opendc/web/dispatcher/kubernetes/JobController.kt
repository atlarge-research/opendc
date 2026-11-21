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

import io.fabric8.kubernetes.api.model.NodeBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.TaintBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.opendc.web.dispatcher.jobName
import java.time.Instant
import java.util.UUID

/**
 * Plays the Job controller and the kubelet against the mock API: registers the nodes, makes an
 * execution's pod and moves it through the states a real node would report.
 */
internal class JobController(
    private val client: KubernetesClient,
    private val namespace: String,
) {
    fun podName(id: UUID): String = "${jobName(id)}-pod"

    fun pending(
        id: UUID,
        cause: String,
    ) = write(
        pod(id)
            .withNewStatus()
            .withPhase("Pending")
            .addNewCondition()
            .withType("PodScheduled")
            .withStatus("False")
            .withMessage(cause)
            .endCondition()
            .endStatus()
            .build(),
    )

    fun waiting(
        id: UUID,
        reason: String,
        message: String,
    ) = write(
        pod(id)
            .withNewStatus()
            .withPhase("Pending")
            .addNewContainerStatus()
            .withName("launcher")
            .withNewState()
            .withNewWaiting()
            .withReason(reason)
            .withMessage(message)
            .endWaiting()
            .endState()
            .endContainerStatus()
            .endStatus()
            .build(),
    )

    fun running(
        id: UUID,
        at: Instant,
    ) = write(
        pod(id)
            .withNewStatus()
            .withPhase("Running")
            .addNewContainerStatus()
            .withName("launcher")
            .withNewState()
            .withNewRunning()
            .withStartedAt(at.toString())
            .endRunning()
            .endState()
            .endContainerStatus()
            .endStatus()
            .build(),
    )

    fun terminated(
        id: UUID,
        code: Int,
        reason: String,
        message: String,
        start: Instant,
        end: Instant,
        podReason: String? = null,
    ) = write(
        pod(id)
            .withNewStatus()
            .withPhase(if (code == 0) "Succeeded" else "Failed")
            .withReason(podReason)
            .addNewContainerStatus()
            .withName("launcher")
            .withNewState()
            .withNewTerminated()
            .withExitCode(code)
            .withReason(reason)
            .withMessage(message)
            .withStartedAt(start.toString())
            .withFinishedAt(end.toString())
            .endTerminated()
            .endState()
            .endContainerStatus()
            .endStatus()
            .build(),
    )

    fun removePod(id: UUID) {
        client.pods().inNamespace(namespace).withName(podName(id)).delete()
    }

    /** Registers a Ready node that offers pods [cpu] and [memory], with a taint of each effect in [taints]. */
    fun node(
        name: String,
        cpu: String,
        memory: String,
        cordoned: Boolean = false,
        taints: List<String> = emptyList(),
    ) {
        val node =
            NodeBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withUnschedulable(cordoned)
                .withTaints(taints.map { TaintBuilder().withKey("example.org/reserved").withEffect(it).build() })
                .endSpec()
                .withNewStatus()
                .addToAllocatable("cpu", Quantity(cpu))
                .addToAllocatable("memory", Quantity(memory))
                .addNewCondition()
                .withType("Ready")
                .withStatus("True")
                .endCondition()
                .endStatus()
                .build()
        client.nodes().resource(node).createOr { it.update() }
    }

    private fun pod(id: UUID): PodBuilder =
        PodBuilder()
            .withNewMetadata()
            .withName(podName(id))
            .withNamespace(namespace)
            .withCreationTimestamp(Instant.now().toString())
            .addToLabels(MANAGED_BY_LABEL, MANAGED_BY)
            .addToLabels(EXECUTION_LABEL, id.toString())
            .endMetadata()

    private fun write(pod: Pod) {
        client.pods().inNamespace(namespace).resource(pod).createOr { it.update() }
    }
}
