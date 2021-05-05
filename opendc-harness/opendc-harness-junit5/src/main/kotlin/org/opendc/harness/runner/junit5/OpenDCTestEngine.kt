/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.harness.runner.junit5

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mu.KotlinLogging
import org.junit.platform.commons.util.ClassLoaderUtils
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassNameFilter
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.MethodSelector
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.opendc.harness.api.ExperimentDefinition
import org.opendc.harness.engine.ExperimentEngineLauncher
import org.opendc.harness.engine.discovery.DiscoveryFilter
import org.opendc.harness.engine.discovery.DiscoveryProvider
import org.opendc.harness.engine.discovery.DiscoveryRequest
import org.opendc.harness.engine.discovery.DiscoverySelector
import java.util.*

/**
 * A [TestEngine] implementation that is able to run experiments defined using the harness.
 */
public class OpenDCTestEngine : TestEngine {
    /**
     * The logging instance for this engine.
     */
    private val logger = KotlinLogging.logger {}

    override fun getId(): String = "opendc-harness"

    override fun getGroupId(): Optional<String> = Optional.of("org.opendc")

    override fun getArtifactId(): Optional<String> = Optional.of("opendc-harness")

    override fun discover(request: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        // IntelliJ will pass a [MethodSelector] to run just a single method inside a file. In that
        // case, no experiments should be discovered, since we support only experiments by class.
        if (request.getSelectorsByType(MethodSelector::class.java).isNotEmpty()) {
            return ExperimentEngineDescriptor(uniqueId, emptyFlow())
        }

        val classNames = request.getSelectorsByType(ClassSelector::class.java).map { DiscoverySelector.Meta("class.name", it.className) }
        val classNameFilters = request.getFiltersByType(ClassNameFilter::class.java).map { DiscoveryFilter.Name(it.toPredicate()) }

        val discovery = DiscoveryProvider.createComposite(ClassLoaderUtils.getDefaultClassLoader())
        val definitions = discovery.discover(DiscoveryRequest(classNames, classNameFilters))

        return ExperimentEngineDescriptor(uniqueId, definitions)
    }

    override fun execute(request: ExecutionRequest) {
        logger.debug { "JUnit ExecutionRequest[${request::class.java.name}] [configurationParameters=${request.configurationParameters}; rootTestDescriptor=${request.rootTestDescriptor}]" }
        val root = request.rootTestDescriptor as ExperimentEngineDescriptor
        val listener = request.engineExecutionListener

        listener.executionStarted(root)

        try {
            ExperimentEngineLauncher()
                .withListener(JUnitExperimentExecutionListener(listener, root))
                .runBlocking(root.experiments)
            listener.executionFinished(root, TestExecutionResult.successful())
        } catch (e: Throwable) {
            listener.executionFinished(root, TestExecutionResult.failed(e))
        }
    }

    private class ExperimentEngineDescriptor(id: UniqueId, val experiments: Flow<ExperimentDefinition>) : EngineDescriptor(id, "opendc") {
        override fun mayRegisterTests(): Boolean = true
    }
}
