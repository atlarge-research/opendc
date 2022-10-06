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

@file:JvmName("ComputeWorkloads")

package org.opendc.experiments.compute

import org.opendc.experiments.compute.internal.CompositeComputeWorkload
import org.opendc.experiments.compute.internal.HpcSampledComputeWorkload
import org.opendc.experiments.compute.internal.LoadSampledComputeWorkload
import org.opendc.experiments.compute.internal.TraceComputeWorkload

/**
 * Construct a workload from a trace.
 */
public fun trace(name: String, format: String = "opendc-vm"): ComputeWorkload = TraceComputeWorkload(name, format)

/**
 * Construct a composite workload with the specified fractions.
 */
public fun composite(vararg pairs: Pair<ComputeWorkload, Double>): ComputeWorkload {
    return CompositeComputeWorkload(pairs.toMap())
}

/**
 * Sample a workload by a [fraction] of the total load.
 */
public fun ComputeWorkload.sampleByLoad(fraction: Double): ComputeWorkload {
    return LoadSampledComputeWorkload(this, fraction)
}

/**
 * Sample a workload by a [fraction] of the HPC VMs (count)
 */
public fun ComputeWorkload.sampleByHpc(fraction: Double): ComputeWorkload {
    return HpcSampledComputeWorkload(this, fraction)
}

/**
 * Sample a workload by a [fraction] of the HPC load
 */
public fun ComputeWorkload.sampleByHpcLoad(fraction: Double): ComputeWorkload {
    return HpcSampledComputeWorkload(this, fraction, sampleLoad = true)
}
