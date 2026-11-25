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

package org.opendc.web.server.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.web.server.rest.InvalidDocumentException

/** A handle prefixes every trace its owner uploads, so it has to read well in a slug and in a URL. */
class HandlesTest {
    @Test
    fun `accepts three to thirty-two characters, folded to lower case`() {
        assertEquals("abc", validHandle(" ABC "))
        assertEquals("a".repeat(32), validHandle("a".repeat(32)))
        assertEquals("ada-lovelace-2", validHandle("Ada-Lovelace-2"))
    }

    @Test
    fun `refuses what would read badly in a slug, at the field it came from`() {
        for (bad in listOf("ab", "a".repeat(33), "2fast", "trailing-", "has space", "dots.too")) {
            val refused = assertThrows<InvalidDocumentException>(bad) { validHandle(bad) }
            assertEquals("handle", refused.problem.issues.single().path)
        }
    }

    @Test
    fun `refuses names that would read as the platform speaking`() {
        for (reserved in listOf("admin", "local", "opendc", "Root")) {
            assertThrows<InvalidDocumentException>(reserved) { validHandle(reserved) }
        }
    }

    @Test
    fun `gives every subject a placeholder that is itself a valid handle`() {
        for (subject in listOf("auth0|1", "google-oauth2|104593", "x")) {
            val placeholder = placeholderHandle(subject)
            assertEquals(placeholder, validHandle(placeholder))
        }
    }
}
