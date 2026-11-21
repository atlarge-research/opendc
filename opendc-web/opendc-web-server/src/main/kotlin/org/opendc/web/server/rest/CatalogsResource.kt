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

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.opendc.web.server.service.CatalogReflection
import org.opendc.web.server.service.SpecCodec

private const val HOST_TEMPLATES = "host-templates"

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
