/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.cli.config

import com.github.ajalt.mordant.rendering.Theme
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Verifies the typed config loader: bundled defaults, external overrides, and per-key fallbacks. */
class CliConfigLoaderTest {
    @Test
    fun `bundled properties reproduce the compiled defaults exactly`() {
        // Guards against the bundled opendc-ui.properties drifting from CliConfig.DEFAULTS.
        assertEquals(CliConfig.DEFAULTS, CliConfigLoader.load(null))
    }

    @Test
    fun `bundled defaults carry the expected values`() {
        val config = CliConfigLoader.load(null)
        assertEquals(100L, config.timing.pollIntervalMs)
        assertEquals(500, config.logging.ringCapacity)
        assertEquals(8, config.headers.summary.size)
        assertEquals("Scenario", config.headers.summary.first())
        assertEquals("—", config.symbols.dash)
        assertEquals(" tasks", config.bar.completedSuffix) // escaped leading space survives
    }

    @Test
    fun `an external override wins per key and leaves the rest at defaults`() {
        val override =
            tempProperties(
                """
                timing.pollIntervalMs=250
                theme.info=#ff0000
                symbol.success=OK
                """.trimIndent(),
            )
        try {
            val config = CliConfigLoader.load(override)
            assertEquals(250L, config.timing.pollIntervalMs)
            assertEquals("#ff0000", config.theme.info)
            assertEquals("OK", config.symbols.success)
            // untouched keys keep the bundled/default value
            assertEquals(CliConfig.DEFAULTS.logging.ringCapacity, config.logging.ringCapacity)
            assertEquals(CliConfig.DEFAULTS.headers.summary, config.headers.summary)
        } finally {
            override.deleteIfExists()
        }
    }

    @Test
    fun `malformed numeric values fall back to defaults without throwing`() {
        val override =
            tempProperties(
                """
                timing.pollIntervalMs=abc
                logging.ringCapacity=notanumber
                """.trimIndent(),
            )
        try {
            val config = CliConfigLoader.load(override)
            assertEquals(CliConfig.DEFAULTS.timing.pollIntervalMs, config.timing.pollIntervalMs)
            assertEquals(CliConfig.DEFAULTS.logging.ringCapacity, config.logging.ringCapacity)
        } finally {
            override.deleteIfExists()
        }
    }

    @Test
    fun `toMordantTheme inherits by default, applies valid colours, and ignores invalid ones`() {
        val sample = "x"
        val defaultInfo = Theme.Default.info(sample)

        assertEquals(defaultInfo, CliConfig.DEFAULTS.toMordantTheme().info(sample))

        val red = CliConfig.DEFAULTS.copy(theme = CliConfig.DEFAULTS.theme.copy(info = "#ff0000")).toMordantTheme()
        assertNotEquals(defaultInfo, red.info(sample))

        val bad = CliConfig.DEFAULTS.copy(theme = CliConfig.DEFAULTS.theme.copy(info = "notacolor")).toMordantTheme()
        assertEquals(defaultInfo, bad.info(sample))
    }

    private fun tempProperties(content: String) = createTempFile("opendc-ui", ".properties").apply { writeText(content) }
}
