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

package org.opendc.sdk.model.failure

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FailurePrefabSpecTest {
    @Test
    fun `every prefab converts to a valid custom failure model`() {
        for (prefab in FailurePrefabSpec.entries) {
            val custom = prefab.toCustomSpec()

            assertEquals(prefab.interArrival, custom.interArrival, "$prefab interArrival")
            assertEquals(prefab.duration, custom.duration, "$prefab duration")
            assertEquals(UniformDistributionSpec(lower = 0.0, upper = 1.0), custom.hostFraction, "$prefab hostFraction")
            assertTrue(custom.validate().isEmpty(), "$prefab is invalid: ${custom.validate()}")
        }
    }

    @Test
    fun `interArrival and duration use the distribution family named by the prefab`() {
        val families =
            mapOf(
                "Exp" to ExponentialDistributionSpec::class,
                "Wbl" to WeibullDistributionSpec::class,
                "LogN" to LogNormalDistributionSpec::class,
                "Gam" to GammaDistributionSpec::class,
            )

        for (prefab in FailurePrefabSpec.entries) {
            val family = families.entries.single { prefab.name.endsWith(it.key) }.value

            assertEquals(family, prefab.interArrival::class, "$prefab interArrival")
            assertEquals(family, prefab.duration::class, "$prefab duration")
        }
    }

    @Test
    fun `prefabs keep their parameters`() {
        assertEquals(ExponentialDistributionSpec(32.41), FailurePrefabSpec.G5k06Exp.interArrival)
        assertEquals(ExponentialDistributionSpec(7.41), FailurePrefabSpec.G5k06Exp.duration)
        assertEquals(WeibullDistributionSpec(alpha = 0.48, beta = 816.60), FailurePrefabSpec.Lanl05Wbl.interArrival)
        assertEquals(WeibullDistributionSpec(alpha = 0.58, beta = 2.18), FailurePrefabSpec.Lanl05Wbl.duration)
        assertEquals(LogNormalDistributionSpec(scale = 0.30, shape = 2.20), FailurePrefabSpec.Nd07cpuLogN.interArrival)
        assertEquals(LogNormalDistributionSpec(scale = -1.02, shape = 1.27), FailurePrefabSpec.Nd07cpuLogN.duration)
        assertEquals(GammaDistributionSpec(shape = 0.50, scale = 28.53), FailurePrefabSpec.Skype06Gam.duration)
    }

    @Test
    fun `prefabs are serialized by name only`() {
        val encoded = Json.encodeToString(FailureModelSpec.serializer(), PrefabFailureSpec(FailurePrefabSpec.G5k06Exp))

        assertEquals("""{"type":"prefab","prefabName":"G5k06Exp"}""", encoded)
    }
}
