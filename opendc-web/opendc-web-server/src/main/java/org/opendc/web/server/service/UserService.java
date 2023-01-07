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

package org.opendc.web.server.service;

import io.quarkus.security.identity.SecurityIdentity;
import javax.enterprise.context.ApplicationScoped;
import org.opendc.web.proto.user.User;
import org.opendc.web.proto.user.UserAccounting;

/**
 * Service for managing {@link User}s.
 */
@ApplicationScoped
public final class UserService {
    /**
     * The service for managing the user accounting.
     */
    private final UserAccountingService accountingService;

    /**
     * Construct a {@link UserService} instance.
     *
     * @param accountingService The {@link UserAccountingService} instance to use.
     */
    public UserService(UserAccountingService accountingService) {
        this.accountingService = accountingService;
    }

    /**
     * Obtain the {@link User} object for the specified <code>identity</code>.
     */
    public User getUser(SecurityIdentity identity) {
        String userId = identity.getPrincipal().getName();
        UserAccounting accounting = accountingService.getAccounting(userId);

        return new User(userId, accounting);
    }
}
