/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.experiments.m3sa

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.opendc.experiments.m3sa.runner.main
import java.io.File

class M3SARunnerTest {
    @Test
    fun `Run Kotlin main and expect no errors`() {
        // clean up any previous outputs
        // print the working directory
//        println("Current working directory: ${File(".").absolutePath}")

        val scenarioJson = "src/test/resources/scenarios/experiment1/scenario-metamodel.json"

        // 2) Clean up any old outputs.
        //    The 'outputFolder' property in scenario-metamodel.json points to this path:
        val outDir = "src/test/resources/outputs/experiment1"
        File(outDir).deleteRecursively()

        // 3) Call your Kotlin CLI WITHOUT -m or -e
        assertDoesNotThrow {
            main(arrayOf("--experiment-path", scenarioJson))
        }

        // 4) Verify the simulation created the folder your JSON configured
        assertTrue(File(outDir).exists(), "Expected simulation to create $outDir")
    }
}
