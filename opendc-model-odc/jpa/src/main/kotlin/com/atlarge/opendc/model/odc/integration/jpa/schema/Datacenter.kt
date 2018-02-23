/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.model.odc.integration.jpa.schema

import com.atlarge.opendc.model.odc.platform.scheduler.Scheduler
import com.atlarge.opendc.model.odc.topology.container.Datacenter
import com.atlarge.opendc.simulator.Duration
import javax.persistence.Entity

/**
 * A datacenter entity in the persistent schema.
 *
 * @property id The unique identifier of the datacenter.
 * @property rooms The rooms in the datacenter.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
@Entity
data class Datacenter(
    val id: Int,
    val rooms: Set<Room>
) : Datacenter {
    /**
     * Construct a datacenter. We need this useless constructor in order for Kotlin correctly initialise the
     * constant fields of the class.
     */
    private constructor() : this(-1, emptySet())

    /**
     * The task scheduler the datacenter uses.
     */
    override lateinit var scheduler: Scheduler
        internal set

    /**
     * The interval at which task will be (re)scheduled.
     * We set this to a fixed constant since the database provides no way of configuring this.
     */
    override val interval: Duration = 10

    /**
     * The initial state of the datacenter.
     */
    override val initialState = Unit
}
