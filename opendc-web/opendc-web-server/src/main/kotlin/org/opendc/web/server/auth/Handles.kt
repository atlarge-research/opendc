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

import org.opendc.web.server.model.sha256Hex
import org.opendc.web.server.rest.DocumentIssue
import org.opendc.web.server.rest.invalidDocument

private const val MIN_HANDLE_LENGTH = 3

private const val MAX_HANDLE_LENGTH = 32

/** A letter first and no dash last, so a handle reads well in a slug. */
private val HANDLE = Regex("[a-z][a-z0-9-]{${MIN_HANDLE_LENGTH - 2},${MAX_HANDLE_LENGTH - 2}}[a-z0-9]")

/** Names that would read as the platform speaking, or collide with paths and built-ins. */
private val RESERVED_HANDLES =
    setOf("admin", "administrator", "anonymous", "api", "builtin", "help", "local", "me", "opendc", "root", "support", "system")

/** How many hex digits of the subject's hash name a placeholder: enough that two never meet. */
private const val PLACEHOLDER_DIGITS = 16

/** [raw] as a handle, trimmed and folded to lower case, or a 400 naming what is wrong with it. */
fun validHandle(raw: String): String {
    val handle = raw.trim().lowercase()
    if (!HANDLE.matches(handle)) {
        throw invalidDocument(
            "That is not a usable handle",
            listOf(
                DocumentIssue(
                    "handle",
                    "must be $MIN_HANDLE_LENGTH to $MAX_HANDLE_LENGTH lowercase letters, digits or dashes, starting with a letter",
                ),
            ),
        )
    }
    if (handle in RESERVED_HANDLES) {
        throw invalidDocument("That handle is reserved", listOf(DocumentIssue("handle", "is reserved")))
    }
    return handle
}

/** What a first sign-in is called until its person chooses: unique, and plainly not a choice. */
fun placeholderHandle(subject: String): String = "user-${sha256Hex(subject).take(PLACEHOLDER_DIGITS)}"
