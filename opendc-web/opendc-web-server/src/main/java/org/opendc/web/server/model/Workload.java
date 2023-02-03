/*
 * Copyright (c) 2023 AtLarge Research
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

package org.opendc.web.server.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

/**
 * Specification of the workload for a {@link Scenario}
 */
@Embeddable
public class Workload {
    /**
     * The {@link Trace} that the workload runs.
     */
    @ManyToOne(optional = false)
    public Trace trace;

    /**
     * The percentage of the trace that should be sampled.
     */
    @Column(name = "sampling_fraction", nullable = false, updatable = false)
    public double samplingFraction;

    /**
     * Construct a {@link Workload} object.
     *
     * @param trace The {@link Trace} to run as workload.
     * @param samplingFraction The percentage of the workload to sample.
     */
    public Workload(Trace trace, double samplingFraction) {
        this.trace = trace;
        this.samplingFraction = samplingFraction;
    }

    /**
     * JPA constructor.
     */
    protected Workload() {}
}
