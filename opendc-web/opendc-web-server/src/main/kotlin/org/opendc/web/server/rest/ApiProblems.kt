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

import io.quarkus.security.ForbiddenException
import io.quarkus.security.UnauthorizedException
import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.web.server.auth.BEARER_SCHEME
import java.util.UUID

/**
 * The error envelope every endpoint returns, mirroring the frontend's ApiError contract. Each
 * [DocumentIssue] points into the submitted document using sdk-model validation paths.
 */
@Serializable
data class ApiProblem(
    val status: Int,
    val title: String,
    val detail: String? = null,
    val issues: List<DocumentIssue> = emptyList(),
)

@Serializable
data class DocumentIssue(
    val path: String,
    val message: String,
)

fun List<ValidationIssue>.toWire(): List<DocumentIssue> = map { DocumentIssue(it.path, it.message) }

/** A user document failed to parse or validate; carries the full problem to return. */
class InvalidDocumentException(val problem: ApiProblem) : RuntimeException(problem.title)

fun invalidDocument(
    title: String,
    issues: List<DocumentIssue>,
): InvalidDocumentException = InvalidDocumentException(ApiProblem(status = 400, title = title, issues = issues))

/**
 * Reads a public identifier from a request. A malformed id is a 404, exactly like a real id belonging
 * to someone else, so callers learn nothing from the difference.
 */
fun publicId(
    raw: String,
    what: String,
): UUID =
    try {
        UUID.fromString(raw)
    } catch (e: IllegalArgumentException) {
        throw notFound(what)
    }

/** Every name column in the schema is varchar(255); a longer name is the caller's error, not a 500. */
private const val NAME_LENGTH_CAP = 255

private const val SIGN_IN_TITLE = "Sign in to continue"

/** Reads a name a caller chose, trimmed, or reports what is wrong with it. */
fun validName(
    raw: String,
    what: String,
    path: String = "name",
): String {
    val name = raw.trim()
    if (name.isEmpty()) {
        throw invalidDocument("$what name must not be blank", listOf(DocumentIssue(path, "must not be blank")))
    }
    if (name.length > NAME_LENGTH_CAP) {
        throw invalidDocument(
            "$what name is too long",
            listOf(DocumentIssue(path, "must be at most $NAME_LENGTH_CAP characters")),
        )
    }
    return name
}

fun notAuthenticated(): WebApplicationException =
    WebApplicationException(
        Response
            .status(401)
            .entity(ApiProblem(status = 401, title = SIGN_IN_TITLE))
            .type(MediaType.APPLICATION_JSON)
            .build(),
    )

/** Refuses a caller who can already see the thing they are acting on, so a 404 would hide nothing. */
fun forbidden(title: String): WebApplicationException =
    WebApplicationException(
        Response.status(403).entity(ApiProblem(status = 403, title = title)).type(MediaType.APPLICATION_JSON).build(),
    )

fun notFound(what: String): WebApplicationException =
    WebApplicationException(
        Response.status(404).entity(ApiProblem(status = 404, title = "$what not found")).type(MediaType.APPLICATION_JSON).build(),
    )

fun conflict(
    title: String,
    issues: List<DocumentIssue> = emptyList(),
): WebApplicationException =
    WebApplicationException(
        Response
            .status(409)
            .entity(ApiProblem(status = 409, title = title, issues = issues))
            .type(MediaType.APPLICATION_JSON)
            .build(),
    )

class ApiExceptionMappers {
    // The built-in security mappers answer with an empty body; these win on priority.
    @ServerExceptionMapper(UnauthorizedException::class)
    fun unauthorized(): RestResponse<ApiProblem> =
        RestResponse.ResponseBuilder
            .create<ApiProblem>(401)
            .entity(ApiProblem(status = 401, title = SIGN_IN_TITLE))
            .header(HttpHeaders.WWW_AUTHENTICATE, BEARER_SCHEME)
            .type(MediaType.APPLICATION_JSON)
            .build()

    @ServerExceptionMapper(ForbiddenException::class)
    fun forbidden(): RestResponse<ApiProblem> = problemResponse(ApiProblem(status = 403, title = "You may not do this"))

    @ServerExceptionMapper
    fun invalidDocument(exception: InvalidDocumentException): RestResponse<ApiProblem> = problemResponse(exception.problem)

    // SerializationException extends IllegalArgumentException; mapping only it keeps real bugs 500s.
    @ServerExceptionMapper
    fun malformedBody(exception: SerializationException): RestResponse<ApiProblem> =
        problemResponse(
            ApiProblem(
                status = 400,
                title = "Request body is not valid JSON for this endpoint",
                issues = listOf(DocumentIssue("", exception.message ?: "malformed body")),
            ),
        )

    @ServerExceptionMapper
    fun constraintViolation(exception: ConstraintViolationException): RestResponse<ApiProblem> =
        problemResponse(
            ApiProblem(
                status = 400,
                title = "Request validation failed",
                issues =
                    exception.constraintViolations.map {
                        DocumentIssue(it.propertyPath.toString(), it.message)
                    },
            ),
        )

    @ServerExceptionMapper
    fun webApplication(exception: WebApplicationException): RestResponse<ApiProblem> {
        val existing = exception.response.entity
        if (existing is ApiProblem) {
            return problemResponse(existing)
        }
        return problemResponse(
            ApiProblem(
                status = exception.response.status,
                title = exception.message ?: "Request failed",
            ),
        )
    }

    private fun problemResponse(problem: ApiProblem): RestResponse<ApiProblem> =
        RestResponse.ResponseBuilder
            .create<ApiProblem>(problem.status)
            .entity(problem)
            .type(MediaType.APPLICATION_JSON)
            .build()
}
