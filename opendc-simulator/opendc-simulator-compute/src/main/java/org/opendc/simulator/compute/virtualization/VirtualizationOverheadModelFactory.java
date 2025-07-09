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

package org.opendc.simulator.compute.virtualization;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendc.simulator.compute.virtualization.OverheadModels.ConstantVirtualizationOverhead;
import org.opendc.simulator.compute.virtualization.OverheadModels.NoVirtualizationOverHead;
import org.opendc.simulator.compute.virtualization.OverheadModels.ShareBasedVirtualizationOverhead;

/**
 * A factory class for creating instances of VirtualizationOverheadModel based on the specified type.
 * This factory supports different virtualization overhead models, including no overhead, constant overhead,
 * and share-based overhead.
 */
public class VirtualizationOverheadModelFactory {

    public enum VirtualizationOverheadModelEnum {
        NONE,
        // General virtualization models -> Passthrough vs Full/Para virtualization
        CONSTANT,
        // Hardware assisted virtualization models
        SHARE_BASED;

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

    /**
     * Factory method to create a VirtualizationOverheadModel based on the specified type.
     *
     * @param virtualizationOverheadModelType The type of virtualization overhead model to create.
     * @return An instance of the specified VirtualizationOverheadModel.
     */
    public static VirtualizationOverheadModel getVirtualizationOverheadModel(
            VirtualizationOverheadModelEnum virtualizationOverheadModelType) {
        return switch (virtualizationOverheadModelType) {
            case NONE -> new NoVirtualizationOverHead();
            case CONSTANT -> {
                double percentageOverhead = -1.0; // Default value if not set
                if (virtualizationOverheadModelType.getPropertyNames().contains("percentageOverhead")) {
                    percentageOverhead =
                            virtualizationOverheadModelType.getProperty("percentageOverhead", Double.class);
                }
                yield new ConstantVirtualizationOverhead(percentageOverhead);
            }
            case SHARE_BASED -> new ShareBasedVirtualizationOverhead();
            default -> throw new IllegalArgumentException(
                    "Unknown virtualization overhead model type: " + virtualizationOverheadModelType);
        };
    }
}
