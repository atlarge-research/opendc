/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.simulator.compute.model;

import java.util.Objects;

/**
 * A single logical compute unit of processor node, either virtual or physical.
 */
public final class ProcessingUnit {
    private final ProcessingNode node;
    private final int id;
    private final double frequency;

    /**
     * Construct a {@link ProcessingUnit} instance.
     *
     * @param node The processing node containing the CPU core.
     * @param id The identifier of the CPU core within the processing node.
     * @param frequency The clock rate of the CPU in MHz.
     */
    public ProcessingUnit(ProcessingNode node, int id, double frequency) {
        this.node = node;
        this.id = id;
        this.frequency = frequency;
    }

    /**
     * Return the processing node containing the CPU core.
     */
    public ProcessingNode getNode() {
        return node;
    }

    /**
     * Return the identifier of the CPU core within the processing node.
     */
    public int getId() {
        return id;
    }

    /**
     * Return the clock rate of the CPU in MHz.
     */
    public double getFrequency() {
        return frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingUnit that = (ProcessingUnit) o;
        return id == that.id && Double.compare(that.frequency, frequency) == 0 && Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, id, frequency);
    }

    @Override
    public String toString() {
        return "ProcessingUnit[node=" + node + ",id=" + id + ",frequency=" + frequency + "]";
    }
}
