/*
 * Copyright (c) 2020 AtLarge Research
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.workload.Task
import org.opendc.simulator.compute.workload.trace.TraceFragment
import java.util.ArrayList

/**
 * Testing suite containing tests that specifically test the FlowDistributor
 */
class CarbonTest {
    /**
     * Carbon test 1: One static task running on 4 different carbon traces
     * In this test, a single task is scheduled that takes 120 min to complete
     *
     * Four different carbon traces are used to calculate the carbon emissions.
     *
     * We check if the energy is the same for all four carbon traces, while the carbon emissions are different.
     */
    @Test
    fun testCarbon1() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(120 * 60 * 1000, 1000.0, 1),
                        ),
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val topologyBe = createTopology("single_1_2000_BE.json")
        val monitorBe = runTest(topologyBe, workload)

        val topologyDe = createTopology("single_1_2000_DE.json")
        val monitorDe = runTest(topologyDe, workload)

        val topologyFr = createTopology("single_1_2000_FR.json")
        val monitorFr = runTest(topologyFr, workload)

        val topologyNl = createTopology("single_1_2000_NL.json")
        val monitorNl = runTest(topologyNl, workload)

        assertAll(
            { assertEquals(120 * 60 * 150.0, monitorBe.energyUsages.sum()) { "The total power usage is not correct" } },
            { assertEquals(120 * 60 * 150.0, monitorDe.energyUsages.sum()) { "The total power usage is not correct" } },
            { assertEquals(120 * 60 * 150.0, monitorFr.energyUsages.sum()) { "The total power usage is not correct" } },
            { assertEquals(120 * 60 * 150.0, monitorNl.energyUsages.sum()) { "The total power usage is not correct" } },
            { assertEquals(8.6798, monitorBe.carbonEmissions.sum(), 1e-3) { "The total power usage is not correct" } },
            { assertEquals(31.8332, monitorDe.carbonEmissions.sum(), 1e-3) { "The total power usage is not correct" } },
            { assertEquals(4.5813, monitorFr.carbonEmissions.sum(), 1e-3) { "The total power usage is not correct" } },
            { assertEquals(49.7641, monitorNl.carbonEmissions.sum(), 1e-3) { "The total power usage is not correct" } },
        )
    }

    /**
     * Carbon test 2: One changing task running on 4 different carbon traces
     * In this test, a single task is scheduled that takes 320 min to complete.
     * The demanded cpu is changing every 40 minutes.
     *
     * Four different carbon traces are used to calculate the carbon emissions.
     *
     * We check if the energy is the same for all four carbon traces, while the carbon emissions are different.
     */
    @Test
    fun testCarbon2() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(40 * 60 * 1000, 1000.0, 1),
                            TraceFragment(40 * 60 * 1000, 2000.0, 1),
                            TraceFragment(40 * 60 * 1000, 1000.0, 1),
                            TraceFragment(40 * 60 * 1000, 2000.0, 1),
                            TraceFragment(40 * 60 * 1000, 1000.0, 1),
                            TraceFragment(40 * 60 * 1000, 2000.0, 1),
                            TraceFragment(40 * 60 * 1000, 1000.0, 1),
                            TraceFragment(40 * 60 * 1000, 2000.0, 1),
                        ),
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val topologyBe = createTopology("single_1_2000_BE.json")
        val monitorBe = runTest(topologyBe, workload)

        val topologyDe = createTopology("single_1_2000_DE.json")
        val monitorDe = runTest(topologyDe, workload)

        val topologyFr = createTopology("single_1_2000_FR.json")
        val monitorFr = runTest(topologyFr, workload)

        val topologyNl = createTopology("single_1_2000_NL.json")
        val monitorNl = runTest(topologyNl, workload)

        assertAll(
            {
                assertEquals(
                    (160 * 60 * 150.0) + (160 * 60 * 200.0),
                    monitorBe.energyUsages.sum(),
                ) { "The total power usage is not correct" }
            },
            {
                assertEquals(
                    (160 * 60 * 150.0) + (160 * 60 * 200.0),
                    monitorDe.energyUsages.sum(),
                ) { "The total power usage is not correct" }
            },
            {
                assertEquals(
                    (160 * 60 * 150.0) + (160 * 60 * 200.0),
                    monitorFr.energyUsages.sum(),
                ) { "The total power usage is not correct" }
            },
            {
                assertEquals(
                    (160 * 60 * 150.0) + (160 * 60 * 200.0),
                    monitorNl.energyUsages.sum(),
                ) { "The total power usage is not correct" }
            },
        )
    }

    /**
     * Carbon test 3: A single task on the NL carbon trace
     * In this test, a single task is scheduled with a carbon trace from the Netherlands
     *
     *
     * We check if the carbon intensity and carbon emission change at the correct moments.
     * We also check the total energy usage, and total carbon emissions.
     */
    @Test
    fun testCarbon3() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(60 * 60 * 1000, 1000.0, 1),
                        ),
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val topologyNl = createTopology("single_1_2000_NL.json")
        val monitorNl = runTest(topologyNl, workload)

        assertAll(
            { assertEquals(164.5177, monitorNl.carbonIntensities.get(0), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(164.5177, monitorNl.carbonIntensities.get(13), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(162.9489, monitorNl.carbonIntensities.get(14), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(162.9489, monitorNl.carbonIntensities.get(28), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(164.3010, monitorNl.carbonIntensities.get(29), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(164.3010, monitorNl.carbonIntensities.get(43), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(167.5809, monitorNl.carbonIntensities.get(44), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(167.5809, monitorNl.carbonIntensities.get(58), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(0.411294, monitorNl.carbonEmissions.get(0), 1e-3) { "The Carbon Emissions are incorrect" } },
            { assertEquals(0.411294, monitorNl.carbonEmissions.get(14), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(0.407372, monitorNl.carbonEmissions.get(15), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(0.407372, monitorNl.carbonEmissions.get(29), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(0.411382, monitorNl.carbonEmissions.get(30), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(0.411382, monitorNl.carbonEmissions.get(44), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(0.418734, monitorNl.carbonEmissions.get(45), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(0.418734, monitorNl.carbonEmissions.get(59), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals(0.0, monitorNl.carbonEmissions.get(60), 1e-3) { "The Carbon Intensity is incorrect" } },
            { assertEquals((60 * 60 * 150.0), monitorNl.energyUsages.sum()) { "The total energy usage is incorrect" } },
            {
                assertEquals(
                    (0.411294 * 15) + (0.407372 * 15) +
                        (0.411382 * 15) + (0.418734 * 15),
                    monitorNl.carbonEmissions.sum(),
                    1e-1,
                ) { "The total carbon emission is incorrect" }
            },
        )
    }
}
