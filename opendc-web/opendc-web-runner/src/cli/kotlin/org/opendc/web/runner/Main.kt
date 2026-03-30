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

package org.opendc.web.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import mu.KotlinLogging
import org.opendc.web.client.auth.OpenIdAuthController
import org.opendc.web.client.runner.OpenDCRunnerClient
import java.io.File
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * Represents the CLI command for starting the OpenDC web runner.
 */
class RunnerCli : CliktCommand(name = "opendc-runner") {
    /**
     * The URL to the OpenDC API.
     */
    private val apiUrl by option(
        "--api-url",
        help = "url to the OpenDC API",
        envvar = "OPENDC_API_URL",
    )
        .convert { URI(it) }
        .default(URI("https://api.opendc.org/v2"))

    /**
     * Flag to disable authentication (for local development).
     */
    private val noAuth by option(
        "--no-auth",
        help = "disable authentication (for local development)",
    )
        .flag()

    /**
     * The auth domain to use.
     */
    private val authDomain by option(
        "--auth-domain",
        help = "auth domain of the OpenDC API",
        envvar = "AUTH0_DOMAIN",
    )

    /**
     * The auth audience to use.
     */
    private val authAudience by option(
        "--auth-audience",
        help = "auth audience of the OpenDC API",
        envvar = "AUTH0_AUDIENCE",
    )

    /**
     * The auth client ID to use.
     */
    private val authClientId by option(
        "--auth-id",
        help = "auth client id of the OpenDC API",
        envvar = "AUTH0_CLIENT_ID",
    )

    /**
     * The auth client secret to use.
     */
    private val authClientSecret by option(
        "--auth-secret",
        help = "auth client secret of the OpenDC API",
        envvar = "AUTH0_CLIENT_SECRET",
    )

    /**
     * The path to the traces directory.
     */
    private val tracePath by option(
        "--traces",
        help = "path to the directory containing the traces",
        envvar = "OPENDC_TRACES",
    )
        .file(canBeFile = false)
        .defaultLazy { File("traces/") }

    /**
     * The number of threads used for simulations..
     */
    private val parallelism by option(
        "--parallelism",
        help = "maximum number of threads for simulations",
    )
        .int()
        .default(Runtime.getRuntime().availableProcessors() - 1)

    override fun run() {
        logger.info { "Starting OpenDC web runner" }

        // Validate auth parameters if authentication is enabled
        if (!noAuth) {
            require(
                authDomain != null,
            ) { "Auth domain is required when authentication is enabled. Use --no-auth to disable authentication." }
            require(
                authAudience != null,
            ) { "Auth audience is required when authentication is enabled. Use --no-auth to disable authentication." }
            require(
                authClientId != null,
            ) { "Auth client ID is required when authentication is enabled. Use --no-auth to disable authentication." }
            require(authClientSecret != null) {
                "Auth client secret is required when authentication is enabled. Use --no-auth to disable authentication."
            }
        }

        val authController =
            if (noAuth) {
                logger.info { "Running without authentication" }
                null
            } else {
                OpenIdAuthController(authDomain!!, authClientId!!, authClientSecret!!, authAudience!!)
            }

        val client = OpenDCRunnerClient(baseUrl = apiUrl, authController)
        val manager = JobManager(client)
        val runner = OpenDCRunner(manager, tracePath, parallelism = parallelism)

        logger.info { "Watching for queued scenarios" }
        runner.run()
    }
}

/**
 * Main entry point of the runner.
 */
fun main(args: Array<String>): Unit = RunnerCli().main(args)
