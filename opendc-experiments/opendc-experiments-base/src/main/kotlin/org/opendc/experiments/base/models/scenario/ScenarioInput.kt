//package org.opendc.experiments.base.models.scenario
//
//import org.opendc.compute.simulator.failure.FailureModel
//import org.opendc.compute.topology.specs.HostSpec
//
//public data class ScenarioInput(
//    val topology: List<HostSpec>,
//    val workload: List<WorkloadSpec>,
//    val allocationPolicy: List<AllocationPolicySpec>,
//    val energyModel: List<PowerModelSpec>,
//    val failureModel: List<FailureModel>?,
//    val exportModel: ExportSpec = ExportSpec(),
//    val outputFolder: String = "output",
//    val name: String = "",
//    val runs: Int = 1,
//    val initialSeed: Int = 0,
//)
