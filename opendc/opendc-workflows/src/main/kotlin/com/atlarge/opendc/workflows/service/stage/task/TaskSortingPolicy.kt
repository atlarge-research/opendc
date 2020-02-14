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

package com.atlarge.opendc.workflows.service.stage.task

import com.atlarge.opendc.workflows.service.StageWorkflowService

/**
 * This interface represents the **T2** stage of the Reference Architecture for Datacenter Schedulers and provides the
 * scheduler with a sorted list of tasks to schedule.
 */
interface TaskSortingPolicy {
    /**
     * Sort the given list of tasks on a given criterion.
     *
     * @param scheduler The scheduler that is sorting the tasks.
     * @param tasks The collection of tasks that should be sorted.
     * @return The sorted list of tasks.
     */
    operator fun invoke(
        scheduler: StageWorkflowService,
        tasks: Collection<StageWorkflowService.TaskView>
    ): List<StageWorkflowService.TaskView>
}
