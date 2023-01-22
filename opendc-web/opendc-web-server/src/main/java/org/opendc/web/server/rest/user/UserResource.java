/*
 * Copyright (c) 2023 AtLarge Research
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

package org.opendc.web.server.rest.user;

import io.quarkus.security.identity.SecurityIdentity;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.opendc.web.proto.user.User;
import org.opendc.web.proto.user.UserAccounting;
import org.opendc.web.server.service.UserAccountingService;

/**
 * A resource representing the active user.
 */
@Produces("application/json")
@Path("/users")
@RolesAllowed("openid")
public final class UserResource {
    /**
     * The service for managing the user accounting.
     */
    private final UserAccountingService accountingService;

    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link UserResource}.
     *
     * @param accountingService The {@link UserAccountingService} instance to use.
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public UserResource(UserAccountingService accountingService, SecurityIdentity identity) {
        this.accountingService = accountingService;
        this.identity = identity;
    }

    /**
     * Get the current active user data.
     */
    @GET
    @Path("me")
    public User get() {
        String userId = identity.getPrincipal().getName();
        UserAccounting accounting = accountingService.getAccounting(userId);

        return new User(userId, accounting);
    }
}
