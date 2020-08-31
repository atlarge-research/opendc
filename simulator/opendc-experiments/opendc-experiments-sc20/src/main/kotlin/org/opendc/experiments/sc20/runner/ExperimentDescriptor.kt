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

import org.opendc.experiments.sc20.runner.execution.ExperimentExecutionContext
import java.io.Serializable

/**
 * An immutable description of an experiment in the **odcsim* simulation framework, which may be a single atomic trial
 * or a composition of multiple trials.
 *
 * This class represents a dynamic tree-like structure where the children of the nodes are not known at instantiation
 * since they might be generated dynamically.
 */
public abstract class ExperimentDescriptor : Serializable {
    /**
     * The parent of this descriptor, or `null` if it has no parent.
     */
    public abstract val parent: ExperimentDescriptor?

    /**
     * The type of descriptor.
     */
    public abstract val type: Type

    /**
     * A flag to indicate that this descriptor is a root descriptor.
     */
    public open val isRoot: Boolean
        get() = parent == null

    /**
     * A flag to indicate that this descriptor describes an experiment trial.
     */
    public val isTrial: Boolean
        get() = type == Type.TRIAL

    /**
     * Execute this [ExperimentDescriptor].
     *
     * @param context The context to execute the descriptor in.
     */
    public abstract suspend operator fun invoke(context: ExperimentExecutionContext)

    /**
     * The types of experiment descriptors.
     */
    public enum class Type {
        /**
         * A composition of multiple experiment descriptions whose invocation happens on a single thread.
         */
        CONTAINER,

        /**
         * An invocation of a single scenario of an experiment whose invocation may happen on different threads.
         */
        TRIAL
    }
}
