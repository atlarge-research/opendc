/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.model

import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.coroutines.actorContext
import com.atlarge.odcsim.coroutines.dsl.ask
import com.atlarge.odcsim.receiveMessage
import com.atlarge.odcsim.same
import com.atlarge.odcsim.setup
import java.util.UUID

/**
 * A representation of a cloud platform such as Amazon Web Services (AWS), Microsoft Azure or Google Cloud.
 *
 * @property uid The unique identifier of this datacenter.
 * @property name the name of the platform.
 * @property zones The availability zones available on this platform.
 */
data class Platform(override val uid: UUID, override val name: String, val zones: List<Zone>) : Identity {
    /**
     * Build the runtime [Behavior] of this cloud platform.
     */
    operator fun invoke(): Behavior<PlatformMessage> = setup { ctx ->
        ctx.log.info("Starting cloud platform {} [{}] with {} zones", name, uid, zones.size)

        // Launch all zones of the cloud platform
        val zoneInstances = zones.associateWith { zone ->
            ctx.spawn(zone(), name = zone.name)
        }

        receiveMessage { msg ->
            when (msg) {
                is PlatformMessage.ListZones -> {
                    ctx.send(msg.replyTo, PlatformResponse.Zones(ctx.self, zoneInstances.mapKeys { it.key.name }))
                    same()
                }
            }
        }
    }
}

/**
 * A reference to the actor managing the zone.
 */
typealias PlatformRef = ActorRef<PlatformMessage>

/**
 * A message protocol for communicating with a cloud platform.
 */
sealed class PlatformMessage {
    /**
     * Request the available zones on this platform.
     *
     * @property replyTo The actor address to send the response to.
     */
    data class ListZones(val replyTo: ActorRef<PlatformResponse.Zones>) : PlatformMessage()
}

/**
 * A message protocol used by platform actors to respond to [PlatformMessage]s.
 */
sealed class PlatformResponse {
    /**
     * The zones available on this cloud platform.
     *
     * @property platform The reference to the cloud platform these are the zones of.
     * @property zones The zones in this cloud platform.
     */
    data class Zones(val platform: PlatformRef, val zones: Map<String, ZoneRef>) : PlatformResponse()
}

/**
 * Retrieve the available zones of a platform.
 */
suspend fun PlatformRef.zones(): Map<String, ZoneRef> {
    val ctx = actorContext<Any>()
    val zones: PlatformResponse.Zones = ctx.ask(this) { PlatformMessage.ListZones(it) }
    return zones.zones
}
