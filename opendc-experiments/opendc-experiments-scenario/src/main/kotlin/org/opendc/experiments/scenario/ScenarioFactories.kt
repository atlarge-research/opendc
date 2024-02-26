package org.opendc.experiments.scenario

import org.opendc.experiments.base.portfolio.model.Scenario
import java.io.File

private val reader = ScenarioReader();

public fun getScenario(file: File): Scenario {
    val inputScenario = reader.read(file);
}
