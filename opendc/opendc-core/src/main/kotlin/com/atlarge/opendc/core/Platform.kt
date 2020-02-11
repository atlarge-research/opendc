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

package com.atlarge.opendc.core

import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.ReceiveRef
import com.atlarge.odcsim.SendRef
import com.atlarge.odcsim.ask
import com.atlarge.odcsim.sendOnce
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

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
    suspend operator fun invoke(ctx: ProcessContext, main: ReceiveRef<PlatformMessage>) {
        println("Starting cloud platform $name [$uid] with ${zones.size} zones")

        // Launch all zones of the cloud platform
        val zoneInstances = zones.associateWith { zone ->
            val channel = ctx.open<ZoneMessage>()
            ctx.spawn({ zone(it, channel) }, name = zone.name)
            channel.send
        }

        val inlet = ctx.listen(main)
        coroutineScope {
            while (isActive) {
                when (val msg = inlet.receive()) {
                    is PlatformMessage.ListZones -> {
                        msg.replyTo.sendOnce(PlatformResponse.Zones(this@Platform, zoneInstances.mapKeys { it.key.name }))
                    }
                }
            }
        }
        inlet.close()
    }
}

/**
 * A message protocol for communicating with a cloud platform.
 */
sealed class PlatformMessage {
    /**
     * Request the available zones on this platform.
     *
     * @property replyTo The actor address to send the response to.
     */
    data class ListZones(val replyTo: SendRef<PlatformResponse.Zones>) : PlatformMessage()
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
    data class Zones(val platform: Platform, val zones: Map<String, SendRef<ZoneMessage>>) : PlatformResponse()
}

/**
 * Retrieve the available zones of a platform.
 */
suspend fun SendRef<PlatformMessage>.zones(): Map<String, SendRef<ZoneMessage>> {
    val zones: PlatformResponse.Zones = ask { PlatformMessage.ListZones(it) }
    return zones.zones
}
