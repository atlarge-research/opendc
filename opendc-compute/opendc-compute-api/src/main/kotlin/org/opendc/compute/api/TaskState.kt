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

package org.opendc.compute.api

/**
 * An enumeration describing the possible states of a task.
 */
public enum class TaskState {
    /**
     * A static task is created
     *
     */
    CREATED,

    /**
     * Resources are being allocated for the instance. The instance is not running yet.
     */
    PROVISIONING,

    /**
     * The task instance is booting up or running.
     */
    RUNNING,

    /**
     * The task is in a failed state.
     */
    FAILED,

    /**
     * The task has been terminated due to too many failures
     *
     */
    TERMINATED,

    /**
     * The task has been completed successfully
     *
     */
    COMPLETED,

    /**
     * Task has been deleted
     *
     * @constructor Create empty Deleted
     */
    DELETED,
}
