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

import org.junit.jupiter.api.Test
import org.opendc.experiments.base.runner.ExperimentCommand
import java.io.File

/**
 * An integration test suite for the Experiment Runner.
 */
class ExperimentCliTest {
    /**
     * ExperimentCli test 1
     * This test runs the experiment defined in the experiment_1.json file.
     *
     * In this test, the bitbrains-small workload is executed with and without a carbon trace.
     */
    @Test
    fun testExperimentCli1() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/test/resources/experiments/experiment_1.json"))

        val someDir = File("output")
        someDir.deleteRecursively()
    }

    /**
     * ExperimentCli test 2
     * This test runs the experiment defined in the experiment_2.json file.
     *
     * In this test, parts of the Marconi 100 workload is executed  . This trace contains GPU tasks.
     */
    @Test
    fun testExperimentCli2() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/test/resources/experiments/experiment_2.json"))

        val someDir = File("output")
        someDir.deleteRecursively()
    }

    /**
     * ExperimentCli test 3
     * This test runs the experiment defined in the experiment_3.json file.
     *
     * In this test, a small workflow is executed.
     */
    @Test
    fun testExperimentCli3() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/test/resources/experiments/experiment_3.json"))

        val someDir = File("output")
        someDir.deleteRecursively()
    }
}
