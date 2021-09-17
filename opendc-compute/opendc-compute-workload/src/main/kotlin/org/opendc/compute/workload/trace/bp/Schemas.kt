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

package org.opendc.compute.workload.trace.bp

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder

/**
 * Schema for the resources table in the trace.
 */
public val BP_RESOURCES_SCHEMA: Schema = SchemaBuilder
    .record("meta")
    .namespace("org.opendc.trace.capelin")
    .fields()
    .requiredString("id")
    .requiredLong("submissionTime")
    .requiredLong("endTime")
    .requiredInt("maxCores")
    .requiredLong("requiredMemory")
    .endRecord()

/**
 * Schema for the resource states table in the trace.
 */
public val BP_RESOURCE_STATES_SCHEMA: Schema = SchemaBuilder
    .record("meta")
    .namespace("org.opendc.trace.capelin")
    .fields()
    .requiredString("id")
    .requiredLong("time")
    .requiredLong("duration")
    .requiredInt("cores")
    .requiredDouble("cpuUsage")
    .requiredLong("flops")
    .endRecord()
