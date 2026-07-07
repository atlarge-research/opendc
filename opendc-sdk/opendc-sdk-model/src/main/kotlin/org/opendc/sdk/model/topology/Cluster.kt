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

package org.opendc.sdk.model.topology

import kotlinx.serialization.Serializable
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.validateEach

/**
 * A cluster of hosts sharing a power source and optional battery.
 *
 * @property name Human-readable identifier.
 * @property count Number of identical clusters to instantiate.
 * @property hosts Hosts contained in the cluster.
 * @property powerSource Power source feeding the cluster.
 * @property battery Optional battery buffering the power source.
 */
@Serializable
public data class Cluster(
    public val name: String = "Cluster",
    public val count: Int = 1,
    public val hosts: List<Host>,
    public val powerSource: PowerSource = PowerSource(),
    public val battery: Battery? = null,
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (hosts.isEmpty()) add(ValidationIssue("hosts", "must not be empty"))
            addAll(hosts.validateEach("hosts"))
        }
}
