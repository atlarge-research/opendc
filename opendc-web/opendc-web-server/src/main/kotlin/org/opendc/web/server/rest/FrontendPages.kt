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

import io.quarkus.vertx.http.runtime.RouteConstants
import io.smallrye.config.ConfigMapping
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.FileSystemAccess
import io.vertx.ext.web.handler.StaticHandler
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.isRegularFile

@ConfigMapping(prefix = "opendc.frontend")
interface FrontendConfig {
    /** The frontend's static export. Absent, as in development and tests, only the API is served. */
    fun directory(): Optional<String>
}

/**
 * Serves the frontend's static export from the same origin as the API, so a self-hosted deployment
 * needs no CORS and no second host.
 *
 * The export writes each page as `project.html` while the app links to `/project?id=...`, so a page
 * path without an extension is served the page it names, which keeps deep links working.
 */
@ApplicationScoped
class FrontendPages(private val config: FrontendConfig) {
    fun register(
        @Observes router: Router,
    ) {
        config.directory().ifPresent { directory ->
            val root = Path.of(directory).toAbsolutePath().normalize()
            val files = StaticHandler.create(FileSystemAccess.ROOT, root.toString()).setIndexPage(INDEX_PAGE)
            router.route().order(RouteConstants.ROUTE_ORDER_BEFORE_DEFAULT).handler { context ->
                serve(context, root, files)
            }
        }
    }

    private fun serve(
        context: RoutingContext,
        root: Path,
        files: StaticHandler,
    ) {
        val path = context.normalizedPath()
        when {
            context.request().method() !in PAGE_METHODS || !isFrontendPath(path) -> context.next()
            isPagePath(path) && isPage(root, "$path.html") -> context.reroute("$path.html")
            else -> files.handle(context)
        }
    }

    private fun isPage(
        root: Path,
        page: String,
    ): Boolean {
        val file = root.resolve(page.removePrefix("/")).normalize()
        return file.startsWith(root) && file.isRegularFile()
    }
}

/** Where the REST API is mounted, relative to the origin. Has to match `quarkus.rest.path`. */
const val API_ROOT = "api/v1"

private const val INDEX_PAGE = "index.html"

private val PAGE_METHODS = setOf(HttpMethod.GET, HttpMethod.HEAD)

private val SERVER_PREFIXES = listOf("/api/", "/q/")

private fun isFrontendPath(path: String): Boolean = SERVER_PREFIXES.none { path.startsWith(it) }

private fun isPagePath(path: String): Boolean = path != "/" && '.' !in path.substringAfterLast('/')
