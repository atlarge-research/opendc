/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.simulator.network

/**
 * A physical bi-directional communication link between two [SimNetworkPort]s.
 *
 * @param left The first port of the link.
 * @param right The second port of the link.
 */
public class SimNetworkLink(public val left: SimNetworkPort, public val right: SimNetworkPort) {
    /**
     * Determine whether the specified [port] participates in this network link.
     */
    public operator fun contains(port: SimNetworkPort): Boolean = port == left || port == right

    /**
     * Obtain the opposite port to which the specified [port] is connected through this link.
     */
    public fun opposite(port: SimNetworkPort): SimNetworkPort {
        return when (port) {
            left -> right
            right -> left
            else -> throw IllegalArgumentException("Invalid port given")
        }
    }

    override fun toString(): String = "SimNetworkLink[left=$left,right=$right]"
}
