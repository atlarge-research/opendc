/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.simulator.compute.thermal;

public class ThermalModels {

    public static ThermalModel rcmodel(
            double rHS, double rCase, double minLeakageCurrent, double maxLeakageCurrent, double supplyVoltage, double ambientTemperature) {
        return new RCModel(rHS, rCase, minLeakageCurrent, maxLeakageCurrent, supplyVoltage, ambientTemperature);
    }

    public static ThermalModel manufacturerModel(double slope, double intercept) {
        return new ManufacturerModel(slope, intercept);
    }

    private static class RCModel implements ThermalModel {
        protected final double rHS;
        protected final double rCase;
        protected final double minLeakageCurrent;
        protected final double maxLeakageCurrent;
        protected final double ambientTemperature;
        protected final double supplyVoltage;

        private double currentStaticPower;
        private double totalPowerDissipated;

        private RCModel(
                double rHS,
                double rCase,
                double minLeakageCurrent,
                double maxLeakageCurrent,
                double supplyVoltage,
                double ambientTemperature) {
            this.rHS = rHS;
            this.rCase = rCase;
            this.minLeakageCurrent = minLeakageCurrent;
            this.maxLeakageCurrent = maxLeakageCurrent;
            this.supplyVoltage = supplyVoltage;
            this.ambientTemperature = ambientTemperature;
        }

        private void setStaticPower(double dynamicPower, double minPower, double maxPower) {
            currentStaticPower =
                ((maxLeakageCurrent - minLeakageCurrent) / (maxPower - minPower) * (dynamicPower - minPower))
                    + minLeakageCurrent;
        }

        private void setThermalPower(double dynamicPower, double minPower) {
            totalPowerDissipated = dynamicPower + minPower + currentStaticPower;
        }

        @Override
        public double setTemperature(double dynamicPower, double minPower, double maxPower) {
            setStaticPower(dynamicPower, minPower, maxPower);
            setThermalPower(dynamicPower, minPower);

           return ambientTemperature + (totalPowerDissipated * (rHS + rCase));
        }
    }

    private static class ManufacturerModel implements ThermalModel {
        protected final double slope;
        protected final double intercept;

        private ManufacturerModel(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
        }

        @Override
        public double setTemperature(double dynamicPower, double minPower, double maxPower) {
            return slope * dynamicPower + intercept;
        }
    }
}
