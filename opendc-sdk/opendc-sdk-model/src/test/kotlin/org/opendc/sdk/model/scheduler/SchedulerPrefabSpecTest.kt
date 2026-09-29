/*
 * Copyright (c) 2026 AtLarge Research
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

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchedulerPrefabSpecTest {
    @Test
    fun `every prefab stands for a valid, non-prefab policy`() {
        for (prefab in SchedulerPrefabSpec.entries) {
            val policy = prefab.policy

            assertFalse(policy is PrefabAllocationPolicySpec, "$prefab refers to another prefab")
            assertTrue(policy.validate().isEmpty(), "$prefab is invalid: ${policy.validate()}")
        }
    }

    @Test
    fun `memorizing prefabs place tasks on the host with the fewest tasks`() {
        for (prefab in listOf(SchedulerPrefabSpec.TaskNumMemorizing, SchedulerPrefabSpec.GpuTaskMemorizing)) {
            val policy = prefab.policy as FilterAllocationPolicySpec
            assertEquals(listOf(InstanceCountWeigherSpec(-1.0)), policy.weighers, "$prefab weighers")
        }
    }

    @Test
    fun `prefabs keep their definitions`() {
        val cpuRam = listOf(ComputeHostFilterSpec, VCpuFilterSpec(1.0), RamFilterSpec(1.0))
        val cpuGpuRam = listOf(ComputeHostFilterSpec, VCpuFilterSpec(1.0), VGpuFilterSpec(1.0), RamFilterSpec(1.0))

        assertEquals(FilterAllocationPolicySpec(cpuRam, listOf(RamWeigherSpec(1.0))), SchedulerPrefabSpec.Mem.policy)
        assertEquals(FilterAllocationPolicySpec(cpuRam, listOf(InstanceCountWeigherSpec(-1.0))), SchedulerPrefabSpec.ActiveServers.policy)
        assertEquals(FilterAllocationPolicySpec(cpuRam, listOf(RamWeigherSpec(-1.0))), SchedulerPrefabSpec.MemInv.policy)
        assertEquals(
            FilterAllocationPolicySpec(cpuRam, listOf(RamWeigherSpec(1.0)), timeshift = TimeshiftSpec()),
            SchedulerPrefabSpec.Timeshift.policy,
        )
        assertEquals(
            FilterAllocationPolicySpec(cpuGpuRam, listOf(VCpuWeigherSpec(-1.0), VGpuWeigherSpec(-1.0))),
            SchedulerPrefabSpec.ProvisionedCpuGpuCoresInv.policy,
        )
        assertEquals(
            FilterAllocationPolicySpec(cpuGpuRam, listOf(InstanceCountWeigherSpec(-1.0))),
            SchedulerPrefabSpec.GpuTaskMemorizing.policy,
        )
    }

    @Test
    fun `prefabs are serialized by name only`() {
        val encoded = Json.encodeToString(AllocationPolicySpec.serializer(), PrefabAllocationPolicySpec(SchedulerPrefabSpec.CoreMem))

        assertEquals("""{"type":"prefab","prefabName":"CoreMem"}""", encoded)
    }
}
