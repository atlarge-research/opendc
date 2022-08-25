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

package org.opendc.simulator.network;

/**
 * A physical bidirectional communication link between two [SimNetworkPort]s.
 */
public final class SimNetworkLink {
    private final SimNetworkPort left;
    private final SimNetworkPort right;

    SimNetworkLink(SimNetworkPort left, SimNetworkPort right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Determine whether the specified <code>port</code> participates in this network link.
     *
     * @return <code>true</code> if the port participates in this link, <code>false</code> otherwise.
     */
    public boolean contains(SimNetworkPort port) {
        return port == left || port == right;
    }

    /**
     * Obtain the opposite port to which the specified <code>port</code> is connected through this link.
     */
    public SimNetworkPort opposite(SimNetworkPort port) {
        if (port == left) {
            return right;
        } else if (port == right) {
            return left;
        }

        throw new IllegalArgumentException("Invalid port given");
    }

    /**
     * Return the first port of the link.
     */
    public SimNetworkPort getLeft() {
        return left;
    }

    /**
     * Return the second port of the link.
     */
    public SimNetworkPort getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "SimNetworkLink[left=" + left + ",right=" + right + "]";
    }
}
