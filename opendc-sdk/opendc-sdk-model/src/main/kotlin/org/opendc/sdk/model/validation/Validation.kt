/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.validation

/**
 * A single validation problem found while checking a model.
 *
 * @property path The dotted field path at which the problem was found.
 * @property message A human-readable description of the problem.
 */
public data class ValidationIssue(public val path: String, public val message: String)

/**
 * A model that can validate its own constraints and report any [ValidationIssue]s.
 */
public fun interface Validatable {
    /**
     * Validates this model and returns all issues found, or an empty list when valid.
     */
    public fun validate(): List<ValidationIssue>
}

/**
 * Prepends [prefix] to the [ValidationIssue.path] of every issue, joining with a dot.
 */
public fun List<ValidationIssue>.prefixed(prefix: String): List<ValidationIssue> =
    map { ValidationIssue(if (it.path.isEmpty()) prefix else prefix + "." + it.path, it.message) }

/**
 * Validates each element and prefixes its issues with an indexed path such as `prefix[0]`.
 */
public fun Iterable<Validatable>.validateEach(prefix: String): List<ValidationIssue> =
    flatMapIndexed { i, v -> v.validate().prefixed(prefix + "[" + i + "]") }
