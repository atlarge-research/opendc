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

package org.opendc.simulator.engine.graph.distributionPolicies;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowDistributor;

public class FlowDistributorFactory {

    public enum DistributionPolicy {
        BEST_EFFORT,
        EQUAL_SHARE,
        FIRST_FIT,
        FIXED_SHARE,
        MAX_MIN_FAIRNESS;

        private final Map<String, Object> properties = new HashMap<>();

        public void setProperty(String key, Object value) {
            properties.put(key, value);
        }

        public Object getProperty(String key) {
            return properties.get(key);
        }

        public <T> T getProperty(String key, Class<T> type) {
            return type.cast(properties.get(key));
        }

        public Set<String> getPropertyNames() {
            return properties.keySet();
        }
    }

    public static FlowDistributor getFlowDistributor(FlowEngine flowEngine, DistributionPolicy distributionPolicyType) {

        return switch (distributionPolicyType) {
            case BEST_EFFORT -> new BestEffortFlowDistributor(
                    flowEngine, distributionPolicyType.getProperty("updateIntervalLength", Long.class));
            case EQUAL_SHARE -> new EqualShareFlowDistributor(flowEngine);
            case FIRST_FIT -> new FirstFitPolicyFlowDistributor(flowEngine);
            case FIXED_SHARE -> {
                if (!distributionPolicyType.getPropertyNames().contains("shareRatio")) {
                    throw new IllegalArgumentException(
                            "FixedShare distribution policy requires a 'shareRatio' property to be set.");
                }
                yield new FixedShareFlowDistributor(
                        flowEngine, distributionPolicyType.getProperty("shareRatio", Double.class));
            }
            default -> new MaxMinFairnessFlowDistributor(flowEngine);
        };
    }
}
