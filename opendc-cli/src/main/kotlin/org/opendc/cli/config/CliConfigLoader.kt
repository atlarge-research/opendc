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

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/**
 * Loads the [CliConfig] from the bundled `opendc-ui.properties`, overlaid by an optional external
 * override file. The override path comes from the `-Dopendc.ui.config` system property or the
 * `OPENDC_UI_CONFIG` environment variable; overlaid keys win, absent keys keep the bundled value.
 *
 * Every field is parsed defensively: a missing or malformed value falls back to [CliConfig.DEFAULTS]
 * for that field alone, so a partial or corrupt override yields a working CLI (with default
 * appearance for the broken keys) instead of a crash.
 */
internal object CliConfigLoader {
    private const val BUNDLED = "/opendc-ui.properties"
    private const val ENV_OVERRIDE = "OPENDC_UI_CONFIG"
    private const val SYSPROP_OVERRIDE = "opendc.ui.config"

    /** Loads the effective configuration, applying any external override. */
    fun load(): CliConfig = load(overridePath())

    /** Loads the bundled configuration, overlaid by [override] when non-null and readable. */
    fun load(override: Path?): CliConfig {
        // Read as UTF-8: the files contain non-Latin-1 symbols (—, ✓, █, …) that Properties.load's
        // default ISO-8859-1 stream decoding would mangle.
        val props = Properties()
        CliConfigLoader::class.java.getResourceAsStream(BUNDLED)?.use { stream ->
            stream.reader(Charsets.UTF_8).use { props.load(it) }
        }
        if (override != null && Files.isReadable(override)) {
            runCatching { Files.newBufferedReader(override, Charsets.UTF_8).use { props.load(it) } }
        }
        return props.toCliConfig()
    }

    private fun overridePath(): Path? {
        val raw = System.getProperty(SYSPROP_OVERRIDE) ?: System.getenv(ENV_OVERRIDE)
        return raw?.takeIf { it.isNotBlank() }?.let { Path.of(it) }
    }

    private fun Properties.toCliConfig(): CliConfig {
        val d = CliConfig.DEFAULTS
        return CliConfig(
            theme =
                ThemeColors(
                    info = str("theme.info", d.theme.info),
                    muted = str("theme.muted", d.theme.muted),
                    success = str("theme.success", d.theme.success),
                    danger = str("theme.danger", d.theme.danger),
                ),
            symbols =
                Symbols(
                    success = str("symbol.success", d.symbols.success),
                    failure = str("symbol.failure", d.symbols.failure),
                    bullet = str("symbol.bullet", d.symbols.bullet),
                    dash = str("symbol.dash", d.symbols.dash),
                    times = str("symbol.times", d.symbols.times),
                    barComplete = str("bar.complete", d.symbols.barComplete),
                    barPending = str("bar.pending", d.symbols.barPending),
                ),
            bar = Bar(completedSuffix = str("bar.completedSuffix", d.bar.completedSuffix)),
            panels =
                Panels(
                    simulationTitle = str("panel.simulation.title", d.panels.simulationTitle),
                    progressTitle = str("panel.progress.title", d.panels.progressTitle),
                    logsTitle = str("panel.logs.title", d.panels.logsTitle),
                    labels =
                        FactLabels(
                            experiment = str("label.experiment", d.panels.labels.experiment),
                            scenarios = str("label.scenarios", d.panels.labels.scenarios),
                            runs = str("label.runs", d.panels.labels.runs),
                            topologies = str("label.topologies", d.panels.labels.topologies),
                            workloads = str("label.workloads", d.panels.labels.workloads),
                            parallelism = str("label.parallelism", d.panels.labels.parallelism),
                            policies = str("label.policies", d.panels.labels.policies),
                            tasks = str("label.tasks", d.panels.labels.tasks),
                            output = str("label.output", d.panels.labels.output),
                            input = str("label.input", d.panels.labels.input),
                        ),
                ),
            headers =
                Headers(
                    summary = list("header.summary", d.headers.summary),
                    topology = list("header.topology", d.headers.topology),
                ),
            timing = Timing(pollIntervalMs = long("timing.pollIntervalMs", d.timing.pollIntervalMs)),
            logging =
                Logging(
                    ringCapacity = int("logging.ringCapacity", d.logging.ringCapacity),
                    captureLevel = str("logging.captureLevel", d.logging.captureLevel),
                ),
        )
    }

    /** Present (even blank, so theme roles can inherit) → the value; absent → [fallback]. */
    private fun Properties.str(
        key: String,
        fallback: String,
    ): String = getProperty(key) ?: fallback

    private fun Properties.long(
        key: String,
        fallback: Long,
    ): Long = getProperty(key)?.trim()?.toLongOrNull() ?: fallback

    private fun Properties.int(
        key: String,
        fallback: Int,
    ): Int = getProperty(key)?.trim()?.toIntOrNull() ?: fallback

    /** Comma-separated list; absent or blank → [fallback]. */
    private fun Properties.list(
        key: String,
        fallback: List<String>,
    ): List<String> {
        val raw = getProperty(key)
        if (raw.isNullOrBlank()) return fallback
        return raw.split(",").map { it.trim() }
    }
}
