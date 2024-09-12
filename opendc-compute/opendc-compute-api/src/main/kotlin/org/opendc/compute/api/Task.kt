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

import java.time.Instant

/**
 * A stateful object representing a task instance that is running on some physical or virtual machine.
 */
public interface Task : Resource {
    /**
     * The flavor of the task.
     */
    public val flavor: Flavor

    /**
     * The image of the task.
     */
    public val image: Image

    /**
     * The last known state of the task.
     */
    public val state: TaskState

    /**
     * The number of times a Task has been stopped due to failures
     */
    public val numFailures: Int

    /**
     * The most recent moment in time when the task was launched.
     */
    public val launchedAt: Instant?

    /**
     * Request the task to be started.
     */
    public fun start()

    /**
     * Request the task to be stopped.
     */
    public fun stop()

    /**
     * Register the specified [TaskWatcher] to watch the state of the task.
     *
     * @param watcher The watcher to register for the task.
     */
    public fun watch(watcher: TaskWatcher)

    /**
     * De-register the specified [TaskWatcher] from the task to stop it from receiving events.
     *
     * @param watcher The watcher to de-register from the task.
     */
    public fun unwatch(watcher: TaskWatcher)
}
