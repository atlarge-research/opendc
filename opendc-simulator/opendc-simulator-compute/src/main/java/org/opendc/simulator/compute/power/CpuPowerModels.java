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

package org.opendc.simulator.compute.power;

import java.util.Arrays;

/**
 * A collection {@link CpuPowerModel} implementations.
 */
public class CpuPowerModels {
    private CpuPowerModels() {}

    /**
     * Construct a constant {@link CpuPowerModel}.
     *
     * @param power The power consumption fo the server at all times (in W).
     */
    public static CpuPowerModel constant(double power) {
        return new ConstantPowerModel(power);
    }

    /**
     * Construct a square root {@link CpuPowerModel} that is adapted from CloudSim.
     *
     * @param maxPower The maximum power draw of the server in W.
     * @param idlePower The power draw of the server at its lowest utilization level in W.
     */
    public static CpuPowerModel sqrt(double maxPower, double idlePower) {
        return new SqrtPowerModel(maxPower, idlePower);
    }

    /**
     * Construct a linear {@link CpuPowerModel} that is adapted from CloudSim.
     *
     * @param maxPower The maximum power draw of the server in W.
     * @param idlePower The power draw of the server at its lowest utilization level in W.
     */
    public static CpuPowerModel linear(double maxPower, double idlePower) {
        return new LinearPowerModel(maxPower, idlePower);
    }

    /**
     * Construct a square {@link CpuPowerModel} that is adapted from CloudSim.
     *
     * @param maxPower The maximum power draw of the server in W.
     * @param idlePower The power draw of the server at its lowest utilization level in W.
     */
    public static CpuPowerModel square(double maxPower, double idlePower) {
        return new SquarePowerModel(maxPower, idlePower);
    }

    /**
     * Construct a cubic {@link CpuPowerModel} that is adapted from CloudSim.
     *
     * @param maxPower The maximum power draw of the server in W.
     * @param idlePower The power draw of the server at its lowest utilization level in W.
     */
    public static CpuPowerModel cubic(double maxPower, double idlePower) {
        return new CubicPowerModel(maxPower, idlePower);
    }

    /**
     * Construct a {@link CpuPowerModel} that minimizes the mean squared error (MSE)
     * to the actual power measurement by tuning the calibration parameter.
     *
     * @param maxPower The maximum power draw of the server in W.
     * @param idlePower The power draw of the server at its lowest utilization level in W.
     * @param calibrationFactor The parameter set to minimize the MSE.
     * @see <a href="https://dl.acm.org/doi/abs/10.1145/1273440.1250665">
     *     Fan et al., Power provisioning for a warehouse-sized computer, ACM SIGARCH'07</a>
     */
    public static CpuPowerModel mse(double maxPower, double idlePower, double calibrationFactor) {
        return new MsePowerModel(maxPower, idlePower, calibrationFactor);
    }

    /**
     * Construct an asymptotic {@link CpuPowerModel} adapted from GreenCloud.
     *
     * @param maxPower The maximum power draw of the server in W.
     * @param idlePower The power draw of the server at its lowest utilization level in W.
     * @param asymUtil A utilization level at which the server attains asymptotic,
     *              i.e., close to linear power consumption versus the offered load.
     *              For most of the CPUs,a is in [0.2, 0.5].
     * @param dvfs A flag indicates whether DVFS is enabled.
     */
    public static CpuPowerModel asymptotic(double maxPower, double idlePower, double asymUtil, boolean dvfs) {
        return new AsymptoticPowerModel(maxPower, idlePower, asymUtil, dvfs);
    }

    /**
     * Construct a linear interpolation model {@link CpuPowerModel} that is adapted from CloudSim.
     *
     * <p>
     * The power consumption is linearly interpolated over the given power levels. In case of two values, the first
     * represents 0% utilization, while the last value represent 100% utilization.
     *
     * @param powerLevels An array of power consumption steps (in W) for a specific CPU utilization.
     * @see <a href="http://www.spec.org/power_ssj2008/results/res2011q1/">Machines used in the SPEC benchmark</a>
     */
    public static CpuPowerModel interpolate(double... powerLevels) {
        return new InterpolationPowerModel(powerLevels.clone());
    }

    /**
     * Decorate an existing {@link CpuPowerModel} to ensure that zero power consumption is reported when there is no
     * utilization.
     *
     * @param delegate The existing {@link CpuPowerModel} to decorate.
     */
    public static CpuPowerModel zeroIdle(CpuPowerModel delegate) {
        return new ZeroIdlePowerDecorator(delegate);
    }

    private static final class ConstantPowerModel implements CpuPowerModel {
        private final double power;

        ConstantPowerModel(double power) {
            this.power = power;
        }

        @Override
        public double computePower(double utilization) {
            return power;
        }

        @Override
        public String toString() {
            return "ConstantPowerModel[power=" + power + "]";
        }
    }

    private abstract static class MaxIdlePowerModel implements CpuPowerModel {
        protected final double maxPower;
        protected final double idlePower;

        MaxIdlePowerModel(double maxPower, double idlePower) {
            this.maxPower = maxPower;
            this.idlePower = idlePower;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[max=" + maxPower + ",idle=" + idlePower + "]";
        }
    }

    private static final class SqrtPowerModel extends MaxIdlePowerModel {
        private final double factor;

        SqrtPowerModel(double maxPower, double idlePower) {
            super(maxPower, idlePower);
            this.factor = (maxPower - idlePower) / Math.sqrt(100);
        }

        @Override
        public double computePower(double utilization) {
            return idlePower + factor * Math.sqrt(utilization * 100);
        }
    }

    private static final class LinearPowerModel extends MaxIdlePowerModel {
        private final double factor;

        LinearPowerModel(double maxPower, double idlePower) {
            super(maxPower, idlePower);
            this.factor = (maxPower - idlePower) / 100;
        }

        @Override
        public double computePower(double utilization) {
            return idlePower + factor * utilization * 100;
        }
    }

    private static final class SquarePowerModel extends MaxIdlePowerModel {
        private final double factor;

        SquarePowerModel(double maxPower, double idlePower) {
            super(maxPower, idlePower);
            this.factor = (maxPower - idlePower) / Math.pow(100, 2);
        }

        @Override
        public double computePower(double utilization) {
            return idlePower + factor * Math.pow(utilization * 100, 2);
        }
    }

    private static final class CubicPowerModel extends MaxIdlePowerModel {
        private final double factor;

        CubicPowerModel(double maxPower, double idlePower) {
            super(maxPower, idlePower);
            this.factor = (maxPower - idlePower) / Math.pow(100, 3);
        }

        @Override
        public double computePower(double utilization) {
            return idlePower + factor * Math.pow(utilization * 100, 3);
        }
    }

    private static final class MsePowerModel extends MaxIdlePowerModel {
        private final double calibrationFactor;
        private final double factor;

        MsePowerModel(double maxPower, double idlePower, double calibrationFactor) {
            super(maxPower, idlePower);
            this.calibrationFactor = calibrationFactor;
            this.factor = (maxPower - idlePower) / 100;
        }

        @Override
        public double computePower(double utilization) {
            return idlePower + factor * (2 * utilization - Math.pow(utilization, calibrationFactor)) * 100;
        }

        @Override
        public String toString() {
            return "MsePowerModel[max=" + maxPower + ",idle=" + idlePower + ",calibrationFactor=" + calibrationFactor
                    + "]";
        }
    }

    private static final class AsymptoticPowerModel extends MaxIdlePowerModel {
        private final double asymUtil;
        private final boolean dvfs;
        private final double factor;

        AsymptoticPowerModel(double maxPower, double idlePower, double asymUtil, boolean dvfs) {
            super(maxPower, idlePower);
            this.asymUtil = asymUtil;
            this.dvfs = dvfs;
            this.factor = (maxPower - idlePower) / 100;
        }

        @Override
        public double computePower(double utilization) {
            if (dvfs) {
                return idlePower
                        + (factor * 100)
                                / 2
                                * (1
                                        + Math.pow(utilization, 3)
                                        - Math.pow(Math.E, -Math.pow(utilization, 3) / asymUtil));
            } else {
                return idlePower + (factor * 100) / 2 * (1 + utilization - Math.pow(Math.E, -utilization / asymUtil));
            }
        }

        @Override
        public String toString() {
            return "AsymptoticPowerModel[max=" + maxPower + ",idle=" + idlePower + ",asymUtil=" + asymUtil + ",dvfs="
                    + dvfs + "]";
        }
    }

    private static final class InterpolationPowerModel implements CpuPowerModel {
        private final double[] powerLevels;

        InterpolationPowerModel(double[] powerLevels) {
            this.powerLevels = powerLevels;
        }

        @Override
        public double computePower(double utilization) {
            final double[] powerLevels = this.powerLevels;
            double clampedUtilization = Math.min(1.0, Math.max(0.0, utilization));

            if (utilization % 0.1 == 0.0) {
                return powerLevels[(int) (clampedUtilization * 10)];
            }

            int utilizationFlr = (int) Math.floor(clampedUtilization * 10);
            int utilizationCil = (int) Math.ceil(clampedUtilization * 10);
            double powerFlr = powerLevels[utilizationFlr];
            double powerCil = powerLevels[utilizationCil];
            double delta = (powerCil - powerFlr) / 10;

            return powerFlr + delta * (clampedUtilization - utilizationFlr / 10.0) * 100;
        }

        @Override
        public String toString() {
            return "InterpolationPowerModel[levels=" + Arrays.toString(powerLevels) + "]";
        }
    }

    private static final class ZeroIdlePowerDecorator implements CpuPowerModel {
        private final CpuPowerModel delegate;

        ZeroIdlePowerDecorator(CpuPowerModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public double computePower(double utilization) {
            if (utilization == 0.0) {
                return 0.0;
            }

            return delegate.computePower(utilization);
        }

        @Override
        public String toString() {
            return "ZeroIdlePowerDecorator[delegate=" + delegate + "]";
        }
    }
}
