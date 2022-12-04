package org.opendc.experiments.workflow

import org.opendc.workflow.api.Job
import org.opendc.workflow.api.Task
import org.opendc.workflow.api.WORKFLOW_TASK_DEADLINE
import org.opendc.workflow.api.WORKFLOW_TASK_EARLIEST_START_TIME
import org.opendc.workflow.api.WORKFLOW_TASK_SLACK
import java.util.UUID
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.max

/*
    This is a real-time reimplementation of the Slack algorithm found in the paper:
    TaskFlow: An Energy- and Makespan-Aware Task Placement Policy for Workflow Scheduling through Delay Management
    Laurens Versluis, Alexandru Iosup at ICPE '22
    See Algorithm 1 and Algorithm 2
 */

public fun Job.computeSlack() {
    this.computeMinimalStartTimes()
    for (t in this.tasks) {
        if (t.dependencies.isEmpty())  {
            t.metadata[WORKFLOW_TASK_SLACK] = 0L
        } else {
            val cmin = t.dependencies.minWith(Comparator.comparingLong {it.metadata[WORKFLOW_TASK_SLACK] as Long})
            t.metadata[WORKFLOW_TASK_SLACK] = cmin.metadata[WORKFLOW_TASK_SLACK] as Long -
                t.metadata[WORKFLOW_TASK_EARLIEST_START_TIME] as Long + t.metadata[WORKFLOW_TASK_DEADLINE] as Long
        }
    }
}

private fun Job.computeMinimalStartTimes() {
    val tasks = LinkedHashMap<UUID, Task>()
    for (t in this.tasks) {
        tasks[t.uid] = t
    }

    // Toposort to compute the optimal start time
    val waves = hashSetOf<HashSet<UUID>>()

    val depCount = HashMap<UUID, Int>()
    val children = HashMap<UUID, HashSet<UUID>>()
    tasks.forEach { (_, t) ->
        depCount[t.uid] = t.dependencies.size
        t.dependencies.forEach {
            children.getOrPut(it.uid) { HashSet() }.add(t.uid)
        }
    }

    while (true) {
        val wave = HashSet<UUID>()
        for ((k, v) in depCount) {
            if (v == 0) {
                wave.add(k)
            }
        }

        if (wave.isEmpty()) {
            break
        }

        for (t in wave) {
            depCount.remove(t)
            children[t]?.forEach { c ->
                if (depCount.containsKey(c)) {
                    depCount[c] = depCount[c]!! - 1
                }
            }
        }
        waves.add(wave)
    }

    for (wave in waves) {
        // Update all children
        for (t in wave) {
            val curTask = tasks[t]!!
            children[t]?.forEach { c ->
                val childTask = tasks[c]
                childTask?.metadata?.set(WORKFLOW_TASK_EARLIEST_START_TIME,
                    max(childTask.metadata[WORKFLOW_TASK_EARLIEST_START_TIME] as Long,
                        (curTask.metadata[WORKFLOW_TASK_EARLIEST_START_TIME] as Long) + (curTask.metadata[WORKFLOW_TASK_DEADLINE]) as Long))
            }
        }
    }
}
