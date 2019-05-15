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

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.ReceivingBehavior
import com.atlarge.odcsim.Signal
import com.atlarge.odcsim.Terminated
import com.atlarge.odcsim.coroutines.actorContext
import com.atlarge.odcsim.coroutines.dsl.ask
import com.atlarge.odcsim.same
import com.atlarge.odcsim.setup
import com.atlarge.odcsim.unhandled
import com.atlarge.opendc.model.services.Service
import com.atlarge.opendc.model.services.ServiceProvider
import java.util.ArrayDeque
import java.util.UUID

/**
 * An isolated location within a datacenter region from which public cloud services operate, roughly equivalent to a
 * single datacenter. Zones contain one or more clusters and secondary storage.
 *
 * This class models *only* the static information of a zone, with dynamic information being contained within the zone's
 * actor. During runtime, it's actor acts as a registry for all the cloud services provided by the zone.
 *
 * @property uid The unique identifier of this availability zone
 * @property name The name of the zone within its platform.
 * @property services The initial set of services provided by the zone.
 * @property clusters The clusters of machines in this zone.
 */
data class Zone(
    override val uid: UUID,
    override val name: String,
    val services: Set<ServiceProvider>,
    val clusters: List<Cluster>
) : Identity {
    /**
     * Build the runtime [Behavior] of this datacenter.
     */
    operator fun invoke(): Behavior<ZoneMessage> = setup { ctx ->
        ctx.log.info("Starting zone {} [{}]", name, uid)

        // Launch all services of the zone
        val instances: MutableMap<Service<*>, ActorRef<*>> = mutableMapOf()
        validateDependencies(services)

        for (provider in services) {
            val ref = ctx.spawn(provider(this, ctx.self), name = "${provider.name}-${provider.uid}")
            ctx.watch(ref)
            provider.provides.forEach { instances[it] = ref }
        }

        object : ReceivingBehavior<ZoneMessage>() {
            override fun receive(ctx: ActorContext<ZoneMessage>, msg: ZoneMessage): Behavior<ZoneMessage> {
                return when (msg) {
                    is ZoneMessage.Find -> {
                        ctx.send(msg.replyTo, ZoneResponse.Listing(ctx.self, msg.key, instances[msg.key]))
                        same()
                    }
                }
            }

            override fun receiveSignal(ctx: ActorContext<ZoneMessage>, signal: Signal): Behavior<ZoneMessage> {
                return when (signal) {
                    is Terminated -> {
                        instances.values.remove(signal.ref)
                        same()
                    }
                    else ->
                        unhandled()
                }
            }
        }
    }

    /**
     * Validate the service for unsatisfiable dependencies.
     */
    private fun validateDependencies(providers: Set<ServiceProvider>) {
        val providersByKey = HashMap<Service<*>, ServiceProvider>()
        for (provider in providers) {
            if (provider.provides.isEmpty()) {
                throw IllegalArgumentException(("Service provider $provider does not provide any service."))
            }
            for (key in provider.provides) {
                if (key in providersByKey) {
                    throw IllegalArgumentException("Multiple providers for service $key")
                }
                providersByKey[key] = provider
            }
        }

        val visited = HashSet<ServiceProvider>()
        val queue = ArrayDeque(providers)
        while (queue.isNotEmpty()) {
            val service = queue.poll()
            visited.add(service)

            for (dependencyKey in service.dependencies) {
                val dependency = providersByKey[dependencyKey]
                    ?: throw IllegalArgumentException("Dependency $dependencyKey not satisfied for service $service")
                if (dependency !in visited) {
                    queue.add(dependency)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is Zone && uid == other.uid
    override fun hashCode(): Int = uid.hashCode()
}

/**
 * A reference to the actor managing the zone.
 */
typealias ZoneRef = ActorRef<ZoneMessage>

/**
 * A message protocol for communicating with the service registry
 */
sealed class ZoneMessage {
    /**
     * Lookup the specified service in this availability zone.
     *
     * @property key The key of the service to lookup.
     * @property replyTo The address to reply to.
     */
    data class Find(
        val key: Service<*>,
        val replyTo: ActorRef<ZoneResponse.Listing>
    ) : ZoneMessage()
}

/**
 * A message protocol used by service registry actors to respond to [ZoneMessage]s.
 */
sealed class ZoneResponse {
    /**
     * The response sent when looking up services in a zone.
     *
     * @property zone The zone from which the response originates.
     * @property key The key of the service that was looked up.
     * @property ref The reference to the service or `null` if it is not present in the zone.
     */
    data class Listing(
        val zone: ZoneRef,
        val key: Service<*>,
        private val ref: ActorRef<*>?
    ) : ZoneResponse() {
        /**
         * A flag to indicate whether the service is present.
         */
        val isPresent: Boolean
            get() = ref != null

        /**
         * Determine whether this listing is for the specified key.
         *
         * @param key The key to check for.
         * @return `true` if the listing is for this key, `false` otherwise.
         */
        fun isForKey(key: Service<*>): Boolean = key == this.key

        /**
         * Extract the result from the service lookup.
         *
         * @param key The key of the lookup.
         * @return The reference to the service or `null` if it is not present in the zone.
         */
        operator fun <T : Any> invoke(key: Service<T>): ActorRef<T>? {
            require(this.key == key) { "Invalid key" }
            @Suppress("UNCHECKED_CAST")
            return ref as? ActorRef<T>
        }
    }
}

/**
 * Find the reference to the specified [ServiceProvider].
 *
 * @param key The key of the service to find.
 * @throws IllegalArgumentException if the service is not found.
 */
suspend fun <T : Any> ZoneRef.find(key: Service<T>): ActorRef<T> {
    val ctx = actorContext<Any>()
    val listing: ZoneResponse.Listing = ctx.ask(this) { ZoneMessage.Find(key, it) }
    return listing(key) ?: throw IllegalArgumentException("Unknown key $key")
}
