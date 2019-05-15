/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.model.workload.workflow

import com.atlarge.opendc.model.Identity
import com.atlarge.opendc.model.workload.application.Application
import java.util.UUID

/**
 * A stage of a [Job].
 *
 * @property uid A unique identified of this task.
 * @property name The name of this task.
 * @property application The application to run as part of this workflow task.
 * @property dependencies The dependencies of this task in order for it to execute.
 */
data class Task(
    override val uid: UUID,
    override val name: String,
    val application: Application,
    val dependencies: Set<Task>
) : Identity {
    override fun equals(other: Any?): Boolean = other is Task && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()
}
