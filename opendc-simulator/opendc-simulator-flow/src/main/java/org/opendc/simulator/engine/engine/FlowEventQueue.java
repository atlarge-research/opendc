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

package org.opendc.simulator.engine.engine;

import java.util.Arrays;
import org.opendc.simulator.engine.graph.FlowNode;

/**
 * A specialized priority queue for future event of {@link FlowNode}s sorted on time.
 * The queue is based on a min heap binary tree (https://www.digitalocean.com/community/tutorials/min-heap-binary-tree)
 * The nodes keep a timerIndex which indicates their placement in the tree.
 * The timerIndex is -1 when a node is not in the tree.
 *
 * <p>
 * By using a specialized priority queue, we reduce the overhead caused by the default priority queue implementation
 * being generic.
 */
public final class FlowEventQueue {
    /**
     * Array representation of binary heap of {@link FlowNode} instances.
     */
    private FlowNode[] queue;

    /**
     * The number of elements in the priority queue.
     */
    private int size = 0;

    /**
     * Construct a {@link FlowEventQueue} with the specified initial capacity.
     *
     * @param initialCapacity The initial capacity of the queue.
     */
    public FlowEventQueue(int initialCapacity) {
        this.queue = new FlowNode[initialCapacity];
    }

    /**
     * Enqueue a timer for the specified node or update the existing timer.
     *
     * When Long.MAX_VALUE is given as a deadline, the node is removed from the queue
     * @param node node to queue
     */
    public void enqueue(FlowNode node) {
        // The timerIndex indicates whether a node is already in the queue
        int timerIndex = node.getTimerIndex();

        if (node.getDeadline() != Long.MAX_VALUE) {
            if (timerIndex >= 0) {
                update(this.queue, node, timerIndex);
            } else {
                add(this.queue, node);
            }
        } else if (timerIndex >= 0) {
            delete(this.queue, timerIndex);
            node.setTimerIndex(-1);
        }
    }

    /**
     * Retrieve the head of the queue if its deadline does not exceed <code>now</code>.
     *
     * @param now The timestamp that the deadline of the head of the queue should not exceed.
     * @return The head of the queue if its deadline does not exceed <code>now</code>, otherwise <code>null</code>.
     */
    public FlowNode poll(long now) {
        if (this.size == 0) {
            return null;
        }

        final FlowNode head = this.queue[0];

        if (now < head.getDeadline()) {
            return null;
        }

        // Move the last element of the queue to the front
        this.size--;
        final FlowNode next = this.queue[this.size];
        this.queue[this.size] = null; // Clear the last element of the queue

        // Sift down the new head.
        if (this.size > 0) {
            siftDown(0, next, this.queue, this.size);
        }

        // Set the index of the head to -1 indicating it is not scheduled anymore
        head.setTimerIndex(-1);
        return head;
    }

    /**
     * Find the earliest deadline in the queue.
     */
    public long peekDeadline() {
        if (this.size > 0) {
            return this.queue[0].getDeadline();
        }

        return Long.MAX_VALUE;
    }

    /**
     * Add a new entry to the queue.
     */
    private void add(FlowNode[] es, FlowNode node) {
        if (this.size >= this.queue.length) {
            // Re-fetch the resized array
            this.grow();
        }

        siftUp(this.size, node, this.queue);

        this.size++;
    }

    /**
     * Update the deadline of an existing entry in the queue.
     */
    private void update(FlowNode[] eventList, FlowNode node, int timerIndex) {
        if (timerIndex > 0) {
            int parentIndex = (timerIndex - 1) >>> 1;
            if (eventList[parentIndex].getDeadline() > node.getDeadline()) {
                siftUp(timerIndex, node, eventList);
                return;
            }
        }

        siftDown(timerIndex, node, eventList, this.size);
    }

    /**
     * The move a node from the queue
     *
     * @param eventList all scheduled events
     * @param timerIndex the index of the node to remove
     */
    private void delete(FlowNode[] eventList, int timerIndex) {
        this.size--;

        // If the element is the last element, simply remove it
        if (timerIndex == this.size) {
            eventList[timerIndex] = null;
        }

        // Else, swap the node to remove with the last node and sift it up or down to get the moved node in the correct
        // position.
        else {

            // swap the node with the last element
            FlowNode moved = eventList[this.size];
            eventList[this.size] = null;

            siftDown(timerIndex, moved, eventList, this.size);

            // SiftUp, if siftDown did not move the node
            if (eventList[timerIndex] == moved) {
                siftUp(timerIndex, moved, eventList);
            }
        }
    }

    /**
     * Increases the capacity of the array.
     */
    private FlowNode[] grow() {
        FlowNode[] queue = this.queue;
        int oldCapacity = queue.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);

        queue = Arrays.copyOf(queue, newCapacity);
        this.queue = queue;
        return queue;
    }

    /**
     * Iteratively swap the node at the given timerIndex with its parents until the node is at the correct
     * position in the binary tree (ie, both children of the node are bigger than the node itself).
     *
     * @param timerIndex the index of the node that needs to be sifted up
     * @param node the node that needs to be sifted up
     * @param eventList the list of nodes that are scheduled
     */
    private static void siftUp(int timerIndex, FlowNode node, FlowNode[] eventList) {
        while (timerIndex > 0) {
            // Find parent Node
            int parentIndex = (timerIndex - 1) >>> 1;
            FlowNode parentNode = eventList[parentIndex];

            // Break if the deadline of the node is bigger than the parent node
            if (node.getDeadline() >= parentNode.getDeadline()) break;

            // Otherwise, swap node with the parentNode
            eventList[timerIndex] = parentNode;
            parentNode.setTimerIndex(timerIndex);
            timerIndex = parentIndex;
        }

        eventList[timerIndex] = node;
        node.setTimerIndex(timerIndex);
    }

    /**
     * Iteratively swap the node at the given timerIndex with its smallest child until the node is at the correct
     * position in the binary tree (ie, both children of the node are bigger than the node itself).
     *
     * @param timerIndex the index of the node that needs to be sifted down
     * @param node the node that needs to be sifted down
     * @param eventList the list of nodes that are scheduled
     * @param queueSize the current size of the queue
     */
    private static void siftDown(int timerIndex, FlowNode node, FlowNode[] eventList, int queueSize) {
        int half = queueSize >>> 1; // loop while a non-leaf
        while (timerIndex < half) {

            // Get the index of the smallest child
            int smallestChildIndex = getSmallestChildIndex(timerIndex, eventList, queueSize);

            // Get the smallest child
            FlowNode smallestChildNode = eventList[smallestChildIndex];

            // If the node is smaller than the smallest child, break
            if (node.getDeadline() <= smallestChildNode.getDeadline()) break;

            // Otherwise, swap the node with its smallest child
            eventList[timerIndex] = smallestChildNode;
            smallestChildNode.setTimerIndex(timerIndex);
            timerIndex = smallestChildIndex;
        }

        eventList[timerIndex] = node;
        node.setTimerIndex(timerIndex);
    }

    /**
     * Return the index of the child with the smallest deadline time of the node at the given timerIndex
     *
     * @param timerIndex the index of the parent node
     * @param eventList the list of all scheduled events
     * @param queueSize the current size of the queue
     * @return the timerIndex of the smallest child
     */
    private static int getSmallestChildIndex(int timerIndex, FlowNode[] eventList, int queueSize) {
        // Calculate the index of the left child
        int leftChildIndex = (timerIndex << 1) + 1;

        // If the left child is at the end of the queue, there is no right child.
        // Thus, the left child is always the smallest
        if (leftChildIndex + 1 >= queueSize) return leftChildIndex;

        FlowNode leftChildNode = eventList[leftChildIndex];

        // Get right child
        int rightChildIndex = leftChildIndex + 1;
        FlowNode rightChildNode = eventList[rightChildIndex];

        // If the rightChild is smaller, return its index
        // otherwise, return the index of the left child
        if (rightChildNode.getDeadline() < leftChildNode.getDeadline()) {
            return rightChildIndex;
        }
        return leftChildIndex;
    }
}
