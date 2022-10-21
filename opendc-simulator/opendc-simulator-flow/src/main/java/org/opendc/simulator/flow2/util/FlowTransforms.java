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

package org.opendc.simulator.flow2.util;

/**
 * A collection of common {@link FlowTransform} implementations.
 */
public class FlowTransforms {
    /**
     * Prevent construction of this class.
     */
    private FlowTransforms() {}

    /**
     * Return a {@link FlowTransform} that forwards the flow rate unmodified.
     */
    public static FlowTransform noop() {
        return NoopFlowTransform.INSTANCE;
    }

    /**
     * No-op implementation of a {@link FlowTransform}.
     */
    private static final class NoopFlowTransform implements FlowTransform {
        static final NoopFlowTransform INSTANCE = new NoopFlowTransform();

        @Override
        public float apply(float value) {
            return value;
        }

        @Override
        public float applyInverse(float value) {
            return value;
        }
    }
}
