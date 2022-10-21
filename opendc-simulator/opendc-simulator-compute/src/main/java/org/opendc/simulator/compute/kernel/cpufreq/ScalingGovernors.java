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

package org.opendc.simulator.compute.kernel.cpufreq;

/**
 * Collection of common {@link ScalingGovernor} implementations.
 */
public class ScalingGovernors {
    private ScalingGovernors() {}

    /**
     * Return a {@link ScalingGovernorFactory} for the <code>performance</code> scaling governor.
     *
     * <p>
     * This governor causes the highest possible frequency to be requested from the CPUs.
     */
    public static ScalingGovernorFactory performance() {
        return PerformanceScalingGovernor.FACTORY;
    }

    /**
     * Return a {@link ScalingGovernorFactory} for the <code>powersave</code> scaling governor.
     *
     * <p>
     * This governor causes the lowest possible frequency to be requested from the CPUs.
     */
    public static ScalingGovernorFactory powerSave() {
        return PowerSaveScalingGovernor.FACTORY;
    }

    /**
     * Return a {@link ScalingGovernorFactory} for the <code>conservative</code> scaling governor from the Linux kernel.
     *
     * @param threshold The threshold before scaling.
     * @param stepSize The size of the frequency steps (use negative value for automatic).
     */
    public static ScalingGovernorFactory conservative(double threshold, double stepSize) {
        return (policy) -> new ConservativeScalingGovernor(policy, threshold, stepSize);
    }

    /**
     * Return a {@link ScalingGovernorFactory} for the <code>conservative</code> scaling governor from the Linux kernel.
     *
     * @param threshold The threshold before scaling.
     */
    public static ScalingGovernorFactory conservative(double threshold) {
        return conservative(threshold, -1.0);
    }

    /**
     * Return a {@link ScalingGovernorFactory} for the <code>ondemand</code> scaling governor from the Linux kernel.
     *
     * @param threshold The threshold before scaling.
     */
    public static ScalingGovernorFactory ondemand(double threshold) {
        return (policy) -> new OnDemandScalingGovernor(policy, threshold);
    }

    private abstract static class AbstractScalingGovernor implements ScalingGovernor {
        protected final ScalingPolicy policy;

        AbstractScalingGovernor(ScalingPolicy policy) {
            this.policy = policy;
        }
    }

    private static class PerformanceScalingGovernor extends AbstractScalingGovernor {
        static final ScalingGovernorFactory FACTORY = PerformanceScalingGovernor::new;

        private PerformanceScalingGovernor(ScalingPolicy policy) {
            super(policy);
        }

        @Override
        public void onStart() {
            policy.setTarget(policy.getMax());
        }
    }

    private static class PowerSaveScalingGovernor extends AbstractScalingGovernor {
        static final ScalingGovernorFactory FACTORY = PowerSaveScalingGovernor::new;

        private PowerSaveScalingGovernor(ScalingPolicy policy) {
            super(policy);
        }

        @Override
        public void onStart() {
            policy.setTarget(policy.getMin());
        }
    }

    private static class ConservativeScalingGovernor extends AbstractScalingGovernor {
        private final double threshold;
        private final double stepSize;
        private double previousLoad;

        private ConservativeScalingGovernor(ScalingPolicy policy, double threshold, double stepSize) {
            super(policy);

            this.threshold = threshold;
            this.previousLoad = threshold;

            if (stepSize < 0) {
                // https://github.com/torvalds/linux/blob/master/drivers/cpufreq/cpufreq_conservative.c#L33
                this.stepSize = policy.getMax() * 0.05;
            } else {
                this.stepSize = Math.min(stepSize, policy.getMax());
            }
        }

        @Override
        public void onStart() {
            policy.setTarget(policy.getMin());
        }

        @Override
        public void onLimit(double load) {
            final ScalingPolicy policy = this.policy;
            double currentTarget = policy.getTarget();
            if (load > threshold) {
                // Check for load increase (see:
                // https://github.com/torvalds/linux/blob/master/drivers/cpufreq/cpufreq_conservative.c#L102)
                double step = 0.0;

                if (load > previousLoad) {
                    step = stepSize;
                } else if (load < previousLoad) {
                    step = -stepSize;
                }

                double target = Math.min(Math.max(currentTarget + step, policy.getMin()), policy.getMax());
                policy.setTarget(target);
            }
            previousLoad = load;
        }
    }

    private static class OnDemandScalingGovernor extends AbstractScalingGovernor {
        private final double threshold;
        private final double multiplier;

        private OnDemandScalingGovernor(ScalingPolicy policy, double threshold) {
            super(policy);

            this.threshold = threshold;
            this.multiplier = (policy.getMax() - policy.getMin()) / 100;
        }

        @Override
        public void onStart() {
            policy.setTarget(policy.getMin());
        }

        @Override
        public void onLimit(double load) {
            final ScalingPolicy policy = this.policy;
            double target;

            if (load < threshold) {
                /* Proportional scaling (see: https://github.com/torvalds/linux/blob/master/drivers/cpufreq/cpufreq_ondemand.c#L151). */
                target = policy.getMin() + load * multiplier;
            } else {
                target = policy.getMax();
            }

            policy.setTarget(target);
        }
    }
}
