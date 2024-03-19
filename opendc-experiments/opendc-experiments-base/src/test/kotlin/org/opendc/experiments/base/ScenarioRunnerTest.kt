/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.experiments.base

import java.io.File

/**
 * Test suite for [ScenarioRunner].
 */
class ScenarioRunnerTest {
    /**
     * The path to the environments.
     */
    private val envPath = File("src/test/resources/env")

    /**
     * The path to the traces.
     */
    private val tracePath = File("src/test/resources/trace")

    /**
     * Smoke test with output.
     * fixme: Fix failures and enable
     *
     fun testSmoke() {
     val outputPath = Files.createTempDirectory("output").toFile()

     try {
     val runner = ScenarioRunner(envPath, tracePath, outputPath)
     val scenario = Scenario(
     Topology("topology"),
     Workload("bitbrains-small", trace("bitbrains-small")),
     OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true),
     "active-servers"
     )

     assertDoesNotThrow { runner.runScenario(scenario, seed = 0L) }
     } finally {
     outputPath.delete()
     }
     }

     /**
     * Smoke test without output.
     * fixme: Fix failures and enable
     */
     fun testSmokeNoOutput() {
     val runner = ScenarioRunner(envPath, tracePath, null)
     val scenario = Scenario(
     Topology("topology"),
     Workload("bitbrains-small", trace("bitbrains-small")),
     OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true),
     "active-servers"
     )

     assertDoesNotThrow { runner.runScenario(scenario, seed = 0L) }
     }
     */
}
