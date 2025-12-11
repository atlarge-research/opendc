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

package org.opendc.simulator.compute.models;

public final class ComponentDegradationModel {
    private final double baseLineWearRate;
    private final double utilizationWearCoefficient;
    private final double powerWearCoefficient;

    public ComponentDegradationModel(
            double baseLineWearRate, double utilizationWearCoefficient, double powerWearCoefficient) {
        this.baseLineWearRate = baseLineWearRate;
        this.utilizationWearCoefficient = utilizationWearCoefficient;
        this.powerWearCoefficient = powerWearCoefficient;
    }

    public double getBaseLineWearRate() {
        return baseLineWearRate;
    }

    public double getUtilizationWearCoefficient() {
        return utilizationWearCoefficient;
    }

    public double getPowerWearCoefficient() {
        return powerWearCoefficient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentDegradationModel that = (ComponentDegradationModel) o;
        return Double.compare(that.baseLineWearRate, baseLineWearRate) == 0
                && Double.compare(that.utilizationWearCoefficient, utilizationWearCoefficient) == 0
                && Double.compare(that.powerWearCoefficient, powerWearCoefficient) == 0;
    }

    @Override
    public String toString() {
        return "DegradationCoeffecients[" + "baseline wear= " + baseLineWearRate
                + ", utilization wear= " + utilizationWearCoefficient
                + ", power wear = " + powerWearCoefficient
                + "]";
    }
}
