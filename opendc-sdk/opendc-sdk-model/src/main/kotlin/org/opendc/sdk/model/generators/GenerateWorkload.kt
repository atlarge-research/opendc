package org.opendc.sdk.model.generators


import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import java.time.LocalDateTime
import java.time.ZoneOffset

public fun generateWorkload(numTasks: Int) : List<TaskSpec> {
    val submissionTime = "2022-01-01T00:00:00"
    val submitMs = LocalDateTime.parse(submissionTime).toInstant(ZoneOffset.UTC).toEpochMilli()
    val taskDuration = TimeDelta.ofHours(1)
    val cpuUsage = Frequency.ofMHz(1000)
    val gpuUsage = Frequency.ofMHz(0.0)

    val workload = mutableListOf<TaskSpec>()
    for (i in 0 until numTasks) {
        val fragments = ArrayList<TaskFragmentSpec>()
        fragments += TaskFragmentSpec(taskDuration, cpuUsage, gpuUsage)
        workload.add(
            TaskSpec(
                id = i,
                name = "$i",
                submissionTime = TimeDelta.ofMillis(submitMs) + (TimeDelta.ofHours(1)*i),
                duration = taskDuration,
                cpuCoreCount = 16,
                cpuCapacity = Frequency.ofMHz(1000),
                memory = DataSize.ofMiB(10000.0),
                fragments = fragments,
        ))
    }

    return workload
}
