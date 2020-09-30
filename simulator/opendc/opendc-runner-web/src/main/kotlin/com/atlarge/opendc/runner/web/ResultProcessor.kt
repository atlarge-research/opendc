package com.atlarge.opendc.runner.web

import org.apache.spark.sql.Column
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.*
import java.io.File

/**
 * A helper class for processing the experiment results using Apache Spark.
 */
class ResultProcessor(private val master: String, private val outputPath: File) {
    /**
     * Process the results of the scenario with the given [id].
     */
    fun process(id: String): Result {
        val spark = SparkSession.builder()
            .master(master)
            .appName("opendc-simulator-$id")
            .config("spark.driver.bindAddress", "0.0.0.0") // Needed to allow the worker to connect to driver
            .orCreate

        try {
            val hostMetrics = spark.read().parquet(File(outputPath, "host-metrics/scenario_id=$id").path)
            val provisionerMetrics = spark.read().parquet(File(outputPath, "provisioner-metrics/scenario_id=$id").path)
            val res = aggregate(hostMetrics, provisionerMetrics).first()

            return Result(
                res.getList<Long>(1),
                res.getList<Long>(2),
                res.getList<Long>(3),
                res.getList<Long>(4),
                res.getList<Double>(5),
                res.getList<Double>(6),
                res.getList<Double>(7),
                res.getList<Int>(8),
                res.getList<Long>(9),
                res.getList<Long>(10),
                res.getList<Long>(11),
                res.getList<Int>(12),
                res.getList<Int>(13),
                res.getList<Int>(14),
                res.getList<Int>(15)
            )
        } finally {
            spark.close()
        }
    }

    data class Result(
        val totalRequestedBurst: List<Long>,
        val totalGrantedBurst: List<Long>,
        val totalOvercommittedBurst: List<Long>,
        val totalInterferedBurst: List<Long>,
        val meanCpuUsage: List<Double>,
        val meanCpuDemand: List<Double>,
        val meanNumDeployedImages: List<Double>,
        val maxNumDeployedImages: List<Int>,
        val totalPowerDraw: List<Long>,
        val totalFailureSlices: List<Long>,
        val totalFailureVmSlices: List<Long>,
        val totalVmsSubmitted: List<Int>,
        val totalVmsQueued: List<Int>,
        val totalVmsFinished: List<Int>,
        val totalVmsFailed: List<Int>
    )

    /**
     * Perform aggregation of the experiment results.
     */
    private fun aggregate(hostMetrics: Dataset<Row>, provisionerMetrics: Dataset<Row>): Dataset<Row> {
        // Extrapolate the duration of the entries to span the entire trace
        val hostMetricsExtra = hostMetrics
            .withColumn("slice_counts", floor(col("duration") / lit(sliceLength)))
            .withColumn("power_draw", col("power_draw") * col("slice_counts"))
            .withColumn("state_int", states[col("state")])
            .withColumn("state_opposite_int", oppositeStates[col("state")])
            .withColumn("cpu_usage", col("cpu_usage") * col("slice_counts") * col("state_opposite_int"))
            .withColumn("cpu_demand", col("cpu_demand") * col("slice_counts"))
            .withColumn("failure_slice_count", col("slice_counts") * col("state_int"))
            .withColumn("failure_vm_slice_count", col("slice_counts") * col("state_int") * col("vm_count"))

        // Process all data in a single run
        val hostMetricsGrouped = hostMetricsExtra.groupBy("run_id")

        // Aggregate the summed total metrics
        val systemMetrics = hostMetricsGrouped.agg(
            sum("requested_burst").alias("total_requested_burst"),
            sum("granted_burst").alias("total_granted_burst"),
            sum("overcommissioned_burst").alias("total_overcommitted_burst"),
            sum("interfered_burst").alias("total_interfered_burst"),
            sum("power_draw").alias("total_power_draw"),
            sum("failure_slice_count").alias("total_failure_slices"),
            sum("failure_vm_slice_count").alias("total_failure_vm_slices")
        )

        // Aggregate metrics per host
        val hvMetrics = hostMetrics
            .groupBy("run_id", "host_id")
            .agg(
                sum("cpu_usage").alias("mean_cpu_usage"),
                sum("cpu_demand").alias("mean_cpu_demand"),
                avg("vm_count").alias("mean_num_deployed_images"),
                count(lit(1)).alias("num_rows")
            )
            .withColumn("mean_cpu_usage", col("mean_cpu_usage") / col("num_rows"))
            .withColumn("mean_cpu_demand", col("mean_cpu_demand") / col("num_rows"))
            .groupBy("run_id")
            .agg(
                avg("mean_cpu_usage").alias("mean_cpu_usage"),
                avg("mean_cpu_demand").alias("mean_cpu_demand"),
                avg("mean_num_deployed_images").alias("mean_num_deployed_images"),
                max("mean_num_deployed_images").alias("max_num_deployed_images")
            )

        // Group the provisioner metrics per run
        val provisionerMetricsGrouped = provisionerMetrics.groupBy("run_id")

        // Aggregate the provisioner metrics
        val provisionerMetricsAggregated = provisionerMetricsGrouped.agg(
            max("vm_total_count").alias("total_vms_submitted"),
            max("vm_waiting_count").alias("total_vms_queued"),
            max("vm_active_count").alias("total_vms_running"),
            max("vm_inactive_count").alias("total_vms_finished"),
            max("vm_failed_count").alias("total_vms_failed")
        )

        // Join the results into a single data frame
        return systemMetrics
            .join(hvMetrics, "run_id")
            .join(provisionerMetricsAggregated, "run_id")
            .select(
                col("total_requested_burst"),
                col("total_granted_burst"),
                col("total_overcommitted_burst"),
                col("total_interfered_burst"),
                col("mean_cpu_usage"),
                col("mean_cpu_demand"),
                col("mean_num_deployed_images"),
                col("max_num_deployed_images"),
                col("total_power_draw"),
                col("total_failure_slices"),
                col("total_failure_vm_slices"),
                col("total_vms_submitted"),
                col("total_vms_queued"),
                col("total_vms_finished"),
                col("total_vms_failed")
            )
            .groupBy(lit(1))
            .agg(
                // TODO Check if order of values is correct
                collect_list(col("total_requested_burst")).alias("total_requested_burst"),
                collect_list(col("total_granted_burst")).alias("total_granted_burst"),
                collect_list(col("total_overcommitted_burst")).alias("total_overcommitted_burst"),
                collect_list(col("total_interfered_burst")).alias("total_interfered_burst"),
                collect_list(col("mean_cpu_usage")).alias("mean_cpu_usage"),
                collect_list(col("mean_cpu_demand")).alias("mean_cpu_demand"),
                collect_list(col("mean_num_deployed_images")).alias("mean_num_deployed_images"),
                collect_list(col("max_num_deployed_images")).alias("max_num_deployed_images"),
                collect_list(col("total_power_draw")).alias("total_power_draw"),
                collect_list(col("total_failure_slices")).alias("total_failure_slices"),
                collect_list(col("total_failure_vm_slices")).alias("total_failure_vm_slices"),
                collect_list(col("total_vms_submitted")).alias("total_vms_submitted"),
                collect_list(col("total_vms_queued")).alias("total_vms_queued"),
                collect_list(col("total_vms_finished")).alias("total_vms_finished"),
                collect_list(col("total_vms_failed")).alias("total_vms_failed")
            )
    }

    // Spark helper functions
    operator fun Column.times(other: Column): Column = `$times`(other)
    operator fun Column.div(other: Column): Column = `$div`(other)
    operator fun Column.get(other: Column): Column = this.apply(other)

    val sliceLength = 5 * 60 * 1000
    val states = map(
        lit("ERROR"),
        lit(1),
        lit("ACTIVE"),
        lit(0),
        lit("SHUTOFF"),
        lit(0)
    )
    val oppositeStates = map(
        lit("ERROR"),
        lit(0),
        lit("ACTIVE"),
        lit(1),
        lit("SHUTOFF"),
        lit(1)
    )
}
