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

package org.opendc.web.server

import io.quarkus.narayana.jta.QuarkusTransaction
import io.restassured.specification.RequestSpecification
import org.opendc.web.server.auth.changeProfile
import org.opendc.web.server.auth.provisionAccount
import org.opendc.web.server.model.AccessToken
import java.time.Instant
import java.util.UUID

/** Someone other than the implicit account, signed in with a personal access token of their own. */
data class TestPerson(
    val id: Long,
    val handle: String,
    val token: String,
) {
    fun request(): RequestSpecification = ApiTest.requestJson().header("Authorization", "Bearer $token")
}

/** People made the way production makes them: provisioned on first sight, then choosing a handle. */
object TestAccounts {
    fun person(prefix: String = "person"): TestPerson =
        QuarkusTransaction.requiringNew().call {
            val handle = "$prefix-${UUID.randomUUID().toString().take(8)}"
            val account = provisionAccount("test|$handle", emptySet(), Instant.now())
            changeProfile(account, handle, "Person $handle") { error("a new account owns no traces") }
            TestPerson(account.id, account.handle, AccessToken.mint(account, "test", Instant.now()).secret)
        }

    /** An account that signed in once and has not chosen its handle yet. */
    fun newcomer(): TestPerson =
        QuarkusTransaction.requiringNew().call {
            val account = provisionAccount("test|newcomer-${UUID.randomUUID()}", emptySet(), Instant.now())
            TestPerson(account.id, account.handle, AccessToken.mint(account, "test", Instant.now()).secret)
        }
}
