/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.web.server.util

import io.quarkus.arc.properties.IfBuildProperty
import java.security.Principal
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.SecurityContext
import javax.ws.rs.ext.Provider

/**
 * Helper class to disable security for the OpenDC web API when in development mode.
 */
@Provider
@PreMatching
@IfBuildProperty(name = "opendc.security.enabled", stringValue = "false")
class DevSecurityOverrideFilter : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        requestContext.securityContext = object : SecurityContext {
            override fun getUserPrincipal(): Principal = Principal { "anon" }

            override fun isSecure(): Boolean = false

            override fun isUserInRole(role: String): Boolean = true

            override fun getAuthenticationScheme(): String = "basic"
        }
    }
}
