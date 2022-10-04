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

package org.opendc.faas.service.deployer

import org.opendc.faas.service.FunctionObject

/**
 * A [FunctionDeployer] is responsible for ensuring that an instance of an arbitrary function, a [FunctionInstance],
 * is deployed.
 *
 * The function deployer should combine the configuration stored in the function registry, the parameters supplied by
 * the requester, and other factors into a decision of how the function should be deployed, including how many and
 * what kind of resources it should receive.
 *
 * Though it decides how the function instance should be deployed, the deployment of the function instance itself is
 * delegated to the Resource Orchestration Layer.
 */
public interface FunctionDeployer {
    /**
     * Deploy the specified [function].
     */
    public fun deploy(function: FunctionObject, listener: FunctionInstanceListener): FunctionInstance
}
