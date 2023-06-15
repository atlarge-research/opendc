package org.opendc.experiments.cloudGaming

import java.io.File
import java.time.Instant

/**
 * Helper class for generating trace and meta CSV files.
 *
 * @param hours The number of hours the trace should have.
 * @param usersPerHour A list of the number of players per hour, should have the same number of entries as 'hours'.
 * @param cpuCount The number of CPUs that is provisioned for each VM.
 * @param cpuUsage The cpu usage of every vm (TODO: make it more flexiable later?).
 * @param cpuCap The cpu energy capacity in Mhz
 * @param memCap The cpu memory capacity in MiB
 * @param outputDir Where to output the traces.
 */
object CloudGamingTraceGenerator {

    val baseDir: File = File("src/test/resources/trace")

    private val traceEntries = mutableListOf<TraceEntry>()

    fun generateTraceCsv(hours: Int, usersPerHour: List<Int>, cpuCount: Int, cpuUsage: Double, cpuCap: Double, memCap: Long, outputDir: String) {
        //TODO: validate number of users is same as number of hours

        // Generate trace entries for each hour
        for (hour in 0 until hours) {
            val timestamp = Instant.now().toEpochMilli() + (hour * (3600000))
            val usersCount = usersPerHour[hour]

            // Generate trace entries for each user in the current hour
            for (userCount in 0 until usersCount) {
                val id = "${userCount + 1}"
                val entry = TraceEntry(id, timestamp, 3600000, cpuCount, cpuUsage)
                traceEntries.add(entry)
            }
        }

        // Write trace entries to CSV file
        val file = baseDir.resolve("$outputDir/trace.csv")
        file.bufferedWriter().use { writer ->
            writer.write("id,timestamp,duration,cpuCount,cpuUsage\n")
            for (entry in traceEntries) {
                writer.write("${entry.id},${entry.timestamp},${entry.duration},${entry.cpuCount},${entry.cpuUsage}\n")
            }
        }
        val maxPlayers = usersPerHour.maxOrNull() ?: 0

        generateMetaCsv(maxPlayers, cpuCount, cpuCap, memCap, outputDir)
    }
    private fun generateMetaCsv(numVMs: Int, cpuCount: Int, cpuCap: Double, memCap: Long, outputDir: String) {

        val metaRows = mutableListOf<MetaRow>()

        // Generate metadata rows for each VM
        for (i in 1..numVMs) {
            val id = i.toString()
            val startTime = traceEntries.first {it.id == id }.timestamp
            val stopTime = traceEntries.last {it.id == id}.timestamp.plus(3600000) // Currently the scope is by the hour, might be changed later
            val cpuCapacity = cpuCap
            val memCapacity = memCap

                metaRows.add(MetaRow(id, startTime, stopTime, cpuCount, cpuCapacity, memCapacity))
        }

        // Write meta rows to CSV file
        val file = baseDir.resolve("$outputDir/meta.csv")
        file.bufferedWriter().use { writer ->
            writer.write("id,start_time,stop_time,cpu_count,cpu_capacity,mem_capacity")
            writer.newLine()

            metaRows.forEach { row ->
                writer.write("${row.id},${row.startTime},${row.stopTime},${row.cpuCount},${row.cpuCapacity},${row.memCapacity}")
                writer.newLine()
            }
        }
    }
    private data class TraceEntry(val id: String, val timestamp: Long, val duration: Long, val cpuCount: Int, val cpuUsage: Double)
    private data class MetaRow(
        val id: String,
        val startTime: Long,
        val stopTime: Long,
        val cpuCount: Int,
        val cpuCapacity: Double,
        val memCapacity: Long
    )
}
