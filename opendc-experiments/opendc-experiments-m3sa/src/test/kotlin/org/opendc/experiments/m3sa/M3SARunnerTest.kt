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

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.experiments.m3sa.runner.main
import java.io.File

class M3SARunnerTest {
    @Test
    fun `Run M3SA-OpenDC full integration 1`() {
        val scenarioJson = "src/test/resources/scenarios/experiment1/scenario_metamodel.json"
        val outDir = "src/test/resources/outputs/"
        val m3saPath = "src/test/resources/m3saSetups/experiment1/m3saSetup.json"
        val m3saExecPath = "src/main/python"
        File(outDir).deleteRecursively()

        assertDoesNotThrow {
            main(
                arrayOf("--experiment-path", scenarioJson, "--m3sa-setup-path", m3saPath, "--m3sa-exec-path", m3saExecPath),
            )
        }

        assertTrue(File(outDir).exists(), "Expected simulation to create $outDir")
        assertTrue(File("$outDir/trackr.json").exists(), "Expected trackr.json to be created in $outDir")
        assertTrue(File("$outDir/trackr.json").readText().isNotEmpty(), "Expected trackr.json to contain data")
    }
}
