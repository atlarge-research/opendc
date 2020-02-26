package com.atlarge.opendc.format.environment.sc20

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
 * @property cpus The CPUs in the machine represented as integer identifiers.
 * @property memories The memories in the machine represented as integer identifiers.
 */
internal data class Machine(val cpus: List<Int>, val memories: List<Int>)
