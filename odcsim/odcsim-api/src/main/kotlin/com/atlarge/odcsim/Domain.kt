/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

import kotlinx.coroutines.CoroutineScope

/**
 * An isolated execution unit that runs concurrently in simulation to the other simulation domains. A domain defines a
 * logical boundary between processes in simulation.
 */
public interface Domain : CoroutineScope {
    /**
     * The name of this domain.
     */
    public val name: String

    /**
     * The parent domain to which the lifecycle of this domain is bound. In case this is a root domain, this refers to
     * itself.
     */
    public val parent: Domain

    /**
     * Construct an anonymous simulation sub-domain that is bound to the lifecycle of this domain.
     */
    public fun newDomain(): Domain

    /**
     * Construct a new simulation sub-domain with the specified [name].
     */
    public fun newDomain(name: String): Domain
}
