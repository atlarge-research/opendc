/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.web.server.rest

import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.opendc.sdk.model.failure.FailurePrefabSpec
import org.opendc.sdk.model.scheduler.SchedulerPrefabSpec
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.model.topology.BatteryPolicySpec
import org.opendc.sdk.model.topology.PowerModelSpec
import org.opendc.web.server.service.SpecCodec

private const val SCHEDULERS = "schedulers"
private const val FAILURE_PREFABS = "failure-prefabs"
private const val POWER_MODELS = "power-models"
private const val BATTERY_POLICIES = "battery-policies"
private const val EXPORT_COLUMNS = "export-columns"
private const val HOST_TEMPLATES = "host-templates"

/** One entry of a catalog reflected from the sdk-model types. */
@Serializable
data class CatalogEntry(
    val id: String,
    val label: String,
    val group: String,
    val description: String? = null,
)

/**
 * A curated hardware profile for the topology builder. The host body is an sdk-model HostSpec
 * document kept opaque here and served verbatim from the bundled host-templates.json.
 */
@Serializable
data class HostTemplate(
    val id: String,
    val label: String,
    val group: String,
    val host: JsonElement,
)

@Path("catalogs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class CatalogsResource(
    private val reflection: CatalogReflection,
    private val codec: SpecCodec,
) {
    private val hostTemplates: List<HostTemplate> =
        codec.json.decodeFromString(
            ListSerializer(HostTemplate.serializer()),
            checkNotNull(CatalogsResource::class.java.getResourceAsStream("/host-templates.json")) {
                "host-templates.json is bundled with the server"
            }.readBytes().decodeToString(),
        )

    @GET
    fun names(): List<String> = reflection.names() + HOST_TEMPLATES

    @GET
    @Path("{name}")
    fun catalog(
        @PathParam("name") name: String,
    ): JsonElement =
        when (name) {
            HOST_TEMPLATES -> codec.json.encodeToJsonElement(hostTemplates)
            else -> codec.json.encodeToJsonElement(reflection.entries(name) ?: throw notFound("Catalog $name"))
        }
}

/**
 * Reflects the option catalogs the draft and topology editors offer out of the sdk-model types
 * themselves, via their serializer descriptors, so the catalogs cannot drift from what the
 * simulator accepts. Ids are the exact wire values a document must carry.
 */
@OptIn(ExperimentalSerializationApi::class)
@Singleton
class CatalogReflection {
    fun names(): List<String> = listOf(SCHEDULERS, FAILURE_PREFABS, POWER_MODELS, BATTERY_POLICIES, EXPORT_COLUMNS)

    fun entries(name: String): List<CatalogEntry>? =
        when (name) {
            SCHEDULERS -> SchedulerPrefabSpec.entries.map { entry(name, it.name) }
            FAILURE_PREFABS -> FailurePrefabSpec.entries.map { entry(name, it.name) }
            POWER_MODELS -> sealedSubtypeNames(PowerModelSpec.serializer().descriptor).map { entry(name, it) }
            BATTERY_POLICIES -> sealedSubtypeNames(BatteryPolicySpec.serializer().descriptor).map { entry(name, it) }
            EXPORT_COLUMNS -> OutputFileSpec.serializer().descriptor.elementNames.map { entry(name, it) }
            else -> null
        }

    private fun entry(
        group: String,
        id: String,
    ): CatalogEntry = CatalogEntry(id = id, label = id, group = group)

    // A sealed serializer descriptor holds the discriminator element and a value union whose
    // element names are the discriminator values of the subtypes.
    private fun sealedSubtypeNames(descriptor: SerialDescriptor): List<String> = descriptor.getElementDescriptor(1).elementNames.toList()
}
