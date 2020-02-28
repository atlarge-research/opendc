/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
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

package com.atlarge.odcsim

/**
 * An engine for managing logical processes represented as [Behavior] during simulation.
 *
 * An implementation of this interface should be provided by an engine. See for example *odcsim-engine-omega*,
 * which is the reference implementation of the *odcsim* API.
 */
public interface SimulationEngine {
    /**
     * The name of this engine instance, used to distinguish between multiple engines running within the same JVM.
     */
    public val name: String

    /**
     * Construct an anonymous root simulation domain.
     */
    public fun newDomain(): Domain

    /**
     * Construct a new root simulation domain with the specified [name].
     */
    public fun newDomain(name: String): Domain

    /**
     * Run the simulation.
     */
    public suspend fun run()

    /**
     * Terminates this engine in an asynchronous fashion.
     */
    public suspend fun terminate()
}
