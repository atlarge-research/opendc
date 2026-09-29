/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.scheduler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.prefixed
import org.opendc.sdk.model.validation.validateEach

/**
 * Describes how tasks are placed onto hosts.
 */
@Serializable
public sealed interface AllocationPolicySpec : Validatable {
    override fun validate(): List<ValidationIssue> = emptyList()
}

/**
 * Selects a named, prefabricated scheduler.
 *
 * @property prefabName The prefabricated scheduler to use.
 */
@Serializable
@SerialName("prefab")
public data class PrefabAllocationPolicySpec(public val prefabName: SchedulerPrefabSpec = SchedulerPrefabSpec.Mem) : AllocationPolicySpec

/**
 * Builds a scheduler from a filter-then-weigh pipeline.
 *
 * @property filters The eligibility predicates applied to candidate hosts.
 * @property weighers The scorers used to rank the remaining candidates; each task is placed on the best-ranked host.
 * @property timeshift Delays deferrable tasks while the carbon intensity is high, or null to place every task right
 *   away.
 */
@Serializable
@SerialName("filter")
public data class FilterAllocationPolicySpec(
    public val filters: List<HostFilterSpec> = listOf(ComputeHostFilterSpec),
    public val weighers: List<HostWeigherSpec> = emptyList(),
    public val timeshift: TimeshiftSpec? = null,
) : AllocationPolicySpec {
    override fun validate(): List<ValidationIssue> =
        buildList {
            addAll(filters.validateEach("filters"))
            addAll(weighers.validateEach("weighers"))
            addAll(timeshift?.validate().orEmpty().prefixed("timeshift"))
        }
}
