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

import java.util.ArrayList;
import java.util.HashMap;
import org.opendc.simulator.engine.engine.FlowEngine;

public class FlowGraph {
    private final FlowEngine engine;
    private final ArrayList<FlowNode> nodes = new ArrayList<>();
    private final ArrayList<FlowEdge> edges = new ArrayList<>();
    private final HashMap<FlowNode, ArrayList<FlowEdge>> nodeToEdge = new HashMap<>();

    public FlowGraph(FlowEngine engine) {
        this.engine = engine;
    }

    /**
     * Return the {@link FlowEngine} driving the simulation of the graph.
     */
    public FlowEngine getEngine() {
        return engine;
    }

    /**
     * Create a new {@link FlowNode} representing a node in the flow network.
     */
    public void addNode(FlowNode node) {
        if (nodes.contains(node)) {
            System.out.println("Node already exists");
        }
        nodes.add(node);
        nodeToEdge.put(node, new ArrayList<>());
        long now = this.engine.getClock().millis();
        node.invalidate(now);
    }

    /**
     * Internal method to remove the specified {@link FlowNode} from the graph.
     */
    public void removeNode(FlowNode node) {

        // Remove all edges connected to node
        final ArrayList<FlowEdge> connectedEdges = nodeToEdge.get(node);
        while (!connectedEdges.isEmpty()) {
            removeEdge(connectedEdges.get(0));
        }

        nodeToEdge.remove(node);

        // remove the node
        nodes.remove(node);
    }

    /**
     * Add an edge between the specified consumer and supplier in this graph.
     */
    public FlowEdge addEdge(FlowConsumer flowConsumer, FlowSupplier flowSupplier) {
        // Check if the consumer and supplier are both FlowNodes
        if (!(flowConsumer instanceof FlowNode)) {
            throw new IllegalArgumentException("Flow consumer is not a FlowNode");
        }
        if (!(flowSupplier instanceof FlowNode)) {
            throw new IllegalArgumentException("Flow consumer is not a FlowNode");
        }

        // Check of the consumer and supplier are present in this graph
        if (!(this.nodes.contains((FlowNode) flowConsumer))) {
            throw new IllegalArgumentException("The consumer is not a node in this graph");
        }
        if (!(this.nodes.contains((FlowNode) flowSupplier))) {
            throw new IllegalArgumentException("The supplier is not a node in this graph");
        }

        final FlowEdge flowEdge = new FlowEdge(flowConsumer, flowSupplier);

        edges.add(flowEdge);

        nodeToEdge.get((FlowNode) flowConsumer).add(flowEdge);
        nodeToEdge.get((FlowNode) flowSupplier).add(flowEdge);

        return flowEdge;
    }

    public void removeEdge(FlowEdge flowEdge) {
        final FlowConsumer consumer = flowEdge.getConsumer();
        final FlowSupplier supplier = flowEdge.getSupplier();
        nodeToEdge.get((FlowNode) consumer).remove(flowEdge);
        nodeToEdge.get((FlowNode) supplier).remove(flowEdge);

        edges.remove(flowEdge);
        flowEdge.close();
    }
}
