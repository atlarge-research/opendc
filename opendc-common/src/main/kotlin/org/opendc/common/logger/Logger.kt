/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.common.logger

import mu.KotlinLogging
import org.slf4j.Logger

/**
 * @return a slf4j logger named as the calling class simple name.
 * Can also be used in the companion object to limit the instances of loggers.
 *
 *
 * ```kotlin
 * class Foo {
 *      val LOG by logger() // Same as: KotlinLogging.logger(name = "Foo")
 *
 *      companion object {
 *          val LOG by logger() // Same as: KotlinLogging.logger(name = "Foo")
 *          val LOG by logger("smth") // Same as: KotlinLogging.logger(name = "smth")
 *      }
 * }
 * ```
 */
public fun <T : Any> T.logger(name: String? = null): Lazy<Logger> {
    return lazy {
        KotlinLogging.logger(
            name
                ?: runCatching { this::class.java.enclosingClass.simpleName }
                    .getOrNull()
                ?: "unknown",
        )
    }
}

/**
 * Logs [msg] with WARN level and returns null.
 * ```kotlin
 * // Replace
 * LOG.warn(<msg>)
 * return null
 * // With
 * return LOG.warnAndNull(<msg>)
 */
public fun Logger.warnAndNull(msg: String): Nothing? {
    this.warn(msg)
    return null
}

/**
 * Logs [msg] with ERROR level and returns null.
 * ```kotlin
 * // Replace
 * LOG.error(<msg>)
 * return null
 * // With
 * return LOG.errAndNull(<msg>)
 */
public fun Logger.errAndNull(msg: String): Nothing? {
    this.error(msg)
    return null
}

/**
 * Logs [msg] with *WARN* level and returns [obj].
 *
 *
 * ```kotlin
 * // Replace
 * if (<key> !in map) {
 *      LOG.warn("warn-message")
 *      return <default-value>
 * } else map[<key>]
 * // With
 * map.getOrDefault(<key>, LOG.withWarn(<default-value>, "<warn-message>"))
 * ```
 */
public fun <T> Logger.withWarn(
    obj: T,
    msg: String,
): T {
    this.warn(msg)
    return obj
}

/**
 * Logs [msg] with *ERROR* level and returns [obj].
 */
public fun <T> Logger.withErr(
    obj: T,
    msg: String,
): T {
    this.error(msg)
    return obj
}

/**
 * Logs [msg] with *INFO* level on a new line.
 * ```console
 *
 * 09:28:08.830 [INFO] ScenariosSpec -
 * | === Compute Export Config ===
 * | Host Fields (columns)    : timestamp,
 * ...
 * // Instead of
 * 09:28:08.830 [INFO] ScenariosSpec - | === Compute Export Config ===
 * | Host Fields (columns)    : timestamp,
 * ```
 */
public fun Logger.infoNewLine(msg: String) {
    info("\n" + msg)
}
