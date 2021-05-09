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

package org.opendc.experiments.tf20.util

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * A datacenter setup.
 *
 * @property name The name of the setup.
 * @property rooms The rooms in the datacenter.
 */
internal data class Setup(val name: String, val rooms: List<Room>)

/**
 * A room in a datacenter.
 *
 * @property type The type of room in the datacenter.
 * @property objects The objects in the room.
 */
internal data class Room(val type: String, val objects: List<RoomObject>)

/**
 * An object in a [Room].
 *
 * @property type The type of the room object.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(value = [JsonSubTypes.Type(name = "RACK", value = RoomObject.Rack::class)])
internal sealed class RoomObject(val type: String) {
    /**
     * A rack in a server room.
     *
     * @property machines The machines in the rack.
     */
    internal data class Rack(val machines: List<Machine>) : RoomObject("RACK")
}

/**
 * A machine in the setup that consists of the specified CPU's represented as
 * integer identifiers and ethernet speed.
 *
 * @property cpus The Processing Units(CPUs/GPUs) in the machine represented as integer identifiers.
 * @property memories The memories in the machine represented as integer identifiers.
 */
internal data class Machine(val cpus: List<Int>, val memories: List<Int>)
