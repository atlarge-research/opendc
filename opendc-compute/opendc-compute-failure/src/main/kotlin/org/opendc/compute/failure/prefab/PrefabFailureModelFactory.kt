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

@file:JvmName("FailureModels")

package org.opendc.compute.failure.prefab

import org.opendc.compute.failure.models.SampleBasedFailureModel
import org.opendc.compute.service.ComputeService
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

public enum class FailurePrefab {
    G5k06Exp,
    G5k06Wbl,
    G5k06LogN,
    G5k06Gam,
    Lanl05Exp,
    Lanl05Wbl,
    Lanl05LogN,
    Lanl05Gam,
    Ldns04Exp,
    Ldns04Wbl,
    Ldns04LogN,
    Ldns04Gam,
    Microsoft99Exp,
    Microsoft99Wbl,
    Microsoft99LogN,
    Microsoft99Gam,
    Nd07cpuExp,
    Nd07cpuWbl,
    Nd07cpuLogN,
    Nd07cpuGam,
    Overnet03Exp,
    Overnet03Wbl,
    Overnet03LogN,
    Overnet03Gam,
    Pl05Exp,
    Pl05Wbl,
    Pl05LogN,
    Pl05Gam,
    Skype06Exp,
    Skype06Wbl,
    Skype06LogN,
    Skype06Gam,
    Websites02Exp,
    Websites02Wbl,
    Websites02LogN,
    Websites02Gam,
}

/**
 * Get a [SampleBasedFailureModel] based on the provided prefab
 *
 * @param context
 * @param clock
 * @param service
 * @param random
 * @param prefab The name of the failure model prefab
 * @return
 */
public fun createFailureModelPrefab(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: RandomGenerator,
    prefab: FailurePrefab,
): SampleBasedFailureModel {
    when (prefab) {
        FailurePrefab.G5k06Exp -> return createG5k06Exp(context, clock, service, random)
        FailurePrefab.G5k06Wbl -> return createG5k06Wbl(context, clock, service, random)
        FailurePrefab.G5k06LogN -> return createG5k06LogN(context, clock, service, random)
        FailurePrefab.G5k06Gam -> return createG5k06Gam(context, clock, service, random)

        FailurePrefab.Lanl05Exp -> return createLanl05Exp(context, clock, service, random)
        FailurePrefab.Lanl05Wbl -> return createLanl05Wbl(context, clock, service, random)
        FailurePrefab.Lanl05LogN -> return createLanl05LogN(context, clock, service, random)
        FailurePrefab.Lanl05Gam -> return createLanl05Gam(context, clock, service, random)

        FailurePrefab.Ldns04Exp -> return createLdns04Exp(context, clock, service, random)
        FailurePrefab.Ldns04Wbl -> return createLdns04Wbl(context, clock, service, random)
        FailurePrefab.Ldns04LogN -> return createLdns04LogN(context, clock, service, random)
        FailurePrefab.Ldns04Gam -> return createLdns04Gam(context, clock, service, random)

        FailurePrefab.Microsoft99Exp -> return createMicrosoft99Exp(context, clock, service, random)
        FailurePrefab.Microsoft99Wbl -> return createMicrosoft99Wbl(context, clock, service, random)
        FailurePrefab.Microsoft99LogN -> return createMicrosoft99LogN(context, clock, service, random)
        FailurePrefab.Microsoft99Gam -> return createMicrosoft99Gam(context, clock, service, random)

        FailurePrefab.Nd07cpuExp -> return createNd07cpuExp(context, clock, service, random)
        FailurePrefab.Nd07cpuWbl -> return createNd07cpuWbl(context, clock, service, random)
        FailurePrefab.Nd07cpuLogN -> return createNd07cpuLogN(context, clock, service, random)
        FailurePrefab.Nd07cpuGam -> return createNd07cpuGam(context, clock, service, random)

        FailurePrefab.Overnet03Exp -> return createOvernet03Exp(context, clock, service, random)
        FailurePrefab.Overnet03Wbl -> return createOvernet03Wbl(context, clock, service, random)
        FailurePrefab.Overnet03LogN -> return createOvernet03LogN(context, clock, service, random)
        FailurePrefab.Overnet03Gam -> return createOvernet03Gam(context, clock, service, random)

        FailurePrefab.Pl05Exp -> return createPl05Exp(context, clock, service, random)
        FailurePrefab.Pl05Wbl -> return createPl05Wbl(context, clock, service, random)
        FailurePrefab.Pl05LogN -> return createPl05LogN(context, clock, service, random)
        FailurePrefab.Pl05Gam -> return createPl05Gam(context, clock, service, random)

        FailurePrefab.Skype06Exp -> return createSkype06Exp(context, clock, service, random)
        FailurePrefab.Skype06Wbl -> return createSkype06Wbl(context, clock, service, random)
        FailurePrefab.Skype06LogN -> return createSkype06LogN(context, clock, service, random)
        FailurePrefab.Skype06Gam -> return createSkype06Gam(context, clock, service, random)

        FailurePrefab.Websites02Exp -> return createWebsites02Exp(context, clock, service, random)
        FailurePrefab.Websites02Wbl -> return createWebsites02Wbl(context, clock, service, random)
        FailurePrefab.Websites02LogN -> return createWebsites02LogN(context, clock, service, random)
        FailurePrefab.Websites02Gam -> return createWebsites02Gam(context, clock, service, random)

        else -> error("Unknown failure prefab: $prefab")
    }
}
