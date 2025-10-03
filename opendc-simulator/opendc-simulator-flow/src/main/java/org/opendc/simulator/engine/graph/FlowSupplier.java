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

package org.opendc.simulator.engine.graph;

import org.opendc.common.ResourceType;

public interface FlowSupplier {

    void handleIncomingDemand(FlowEdge consumerEdge, double newDemand);

    default void handleIncomingDemand(FlowEdge consumerEdge, double newDemand, ResourceType resourceType) {
        handleIncomingDemand(consumerEdge, newDemand);
    }

    default void handleIncomingDemand(
            FlowEdge consumerEdge, double newDemand, ResourceType resourceType, int consumerCount) {
        handleIncomingDemand(consumerEdge, newDemand);
    }
    ;

    void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply);

    default void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply, ResourceType resourceType) {
        pushOutgoingSupply(consumerEdge, newSupply);
    }
    ;

    void addConsumerEdge(FlowEdge consumerEdge);

    void removeConsumerEdge(FlowEdge consumerEdge);

    double getCapacity();

    default double getCapacity(ResourceType resourceType) {
        return getCapacity();
    }

    default ResourceType getSupplierResourceType() {
        return ResourceType.AUXILIARY;
    }
    ;
}
