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

package org.opendc.experiments.sc20.runner

import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.opendc.experiments.sc20.runner.execution.ExperimentExecutionContext
import org.opendc.experiments.sc20.runner.execution.ExperimentExecutionResult

/**
 * An abstract [ExperimentDescriptor] specifically for containers.
 */
public abstract class ContainerExperimentDescriptor : ExperimentDescriptor() {
    /**
     * The child descriptors of this container.
     */
    public abstract val children: Sequence<ExperimentDescriptor>

    override val type: Type = Type.CONTAINER

    override suspend fun invoke(context: ExperimentExecutionContext) {
        val materializedChildren = children.toList()
        for (child in materializedChildren) {
            context.listener.descriptorRegistered(child)
        }

        supervisorScope {
            for (child in materializedChildren) {
                if (child.isTrial) {
                    launch {
                        val worker = context.scheduler.allocate()
                        context.listener.executionStarted(child)
                        try {
                            worker(child, context)
                            context.listener.executionFinished(child, ExperimentExecutionResult.Success)
                        } catch (e: Throwable) {
                            context.listener.executionFinished(child, ExperimentExecutionResult.Failed(e))
                        }
                    }
                } else {
                    launch { child(context) }
                }
            }
        }
    }
}
