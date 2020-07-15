package com.atlarge.opendc.runner.web

import com.github.ajalt.clikt.core.CliktCommand
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Represents the CLI command for starting the OpenDC web runner.
 */
class RunnerCli : CliktCommand(name = "runner") {
    override fun run() {
        logger.info { "Starting OpenDC web runner" }
    }
}

/**
 * Main entry point of the runner.
 */
fun main(args: Array<String>) = RunnerCli().main(args)
