/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20.runner.internal

import com.atlarge.opendc.experiments.sc20.runner.ExperimentDescriptor
import com.atlarge.opendc.experiments.sc20.runner.ExperimentRunner
import com.atlarge.opendc.experiments.sc20.runner.execution.ExperimentExecutionContext
import com.atlarge.opendc.experiments.sc20.runner.execution.ExperimentExecutionListener
import com.atlarge.opendc.experiments.sc20.runner.execution.ExperimentExecutionResult
import com.atlarge.opendc.experiments.sc20.runner.execution.ExperimentScheduler
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

/**
 * The default implementation of the [ExperimentRunner] interface.
 *
 * @param scheduler The scheduler to use.
 */
public class DefaultExperimentRunner(val scheduler: ExperimentScheduler) : ExperimentRunner {
    override val id: String = "default"

    override val version: String? = "1.0"

    override fun execute(root: ExperimentDescriptor, listener: ExperimentExecutionListener) = runBlocking {
        val context = object : ExperimentExecutionContext {
            override val listener: ExperimentExecutionListener = listener
            override val scheduler: ExperimentScheduler = this@DefaultExperimentRunner.scheduler
            override val cache: MutableMap<Any?, Any?> = ConcurrentHashMap()
        }

        listener.descriptorRegistered(root)
        context.listener.executionStarted(root)
        try {
            root(context)
            context.listener.executionFinished(root, ExperimentExecutionResult.Success)
        } catch (e: Throwable) {
            context.listener.executionFinished(root, ExperimentExecutionResult.Failed(e))
        }
    }
}
