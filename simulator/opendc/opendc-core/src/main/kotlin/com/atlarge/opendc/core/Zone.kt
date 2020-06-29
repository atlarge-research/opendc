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

package com.atlarge.opendc.core

import com.atlarge.opendc.core.services.ServiceRegistry
import java.util.UUID

/**
 * An isolated location within a datacenter region from which public cloud services operate, roughly equivalent to a
 * single datacenter. Zones contain one or more clusters and secondary storage.
 *
 * This class models *only* the static information of a zone, with dynamic information being contained within the zone's
 * actor. During runtime, it's actor acts as a registry for all the cloud services provided by the zone.
 *
 * @property uid The unique identifier of this availability zone.
 * @property name The name of the zone within its platform.
 * @property services The service registry containing the services of the zone.
 */
data class Zone(
    override val uid: UUID,
    override val name: String,
    val services: ServiceRegistry
) : Identity {
    override fun equals(other: Any?): Boolean = other is Zone && uid == other.uid
    override fun hashCode(): Int = uid.hashCode()
}
