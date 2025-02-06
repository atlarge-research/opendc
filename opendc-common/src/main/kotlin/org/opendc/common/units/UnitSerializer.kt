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

package org.opendc.common.units

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import org.opendc.common.logger.logger

/**
 * Serializer for [T].
 * @param[ifNumber] function invoked if the value to parse is a number without unit of measure.
 * ```json
 * // json e.g.
 * "value": 3
 * // or
 * "value": "3"
 * ```
 * @param[serializerFun] function invoked when [T] needs to be serialized.
 *
 * @param[conditions] conditions used during the deserialization process.
 * If the condition returns [T] then it is considered as the result of the deserialization.
 * If the condition returns `null` the next condition is tested, until one
 * satisfied condition is found, throws exception otherwise.
 */
internal open class UnitSerializer<T : Unit<T>>(
    ifNumber: (Number) -> T,
    serializerFun: Encoder.(T) -> kotlin.Unit,
    vararg conditions: String.() -> T?,
) : OnlyString<T>(
        object : KSerializer<T> {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("unit-serializer", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): T {
                val strField = decoder.decodeString()
                try {
                    // If the field is a number.
                    return ifNumber(json.decodeFromString<Double>(strField))
                } catch (e: Exception) {
                    // No ops.
                }

                conditions.forEach { condition ->
                    // If condition satisfied return result.
                    strField.condition()?.let { return it }
                }

                throw RuntimeException("unable to parse unit of measure $strField")
            }

            override fun serialize(
                encoder: Encoder,
                value: T,
            ) {
                serializerFun(encoder, value)
            }
        },
    ) {
    companion object {
        val LOG by logger()

        val json = Json

        /**
         * @return a lambda that can be passed as condition to [UnitSerializer] constructor.
         */
        fun <T> ifMatches(
            regex: Regex,
            block: MatchResult.() -> T,
        ): String.() -> T? =
            {
                regex.matchEntire(this)?.block()
            }

        /**
         * @return a lambda that can be passed as condition to [UnitSerializer] constructor.
         */
        fun <T> ifMatches(
            regexStr: String,
            vararg options: RegexOption = emptyArray(),
            block: MatchResult.() -> T,
        ): String.() -> T? =
            {
                Regex(regexStr, options.toSet()).matchEntire(this)?.block()
            }

        /**
         * @return a lambda that can be passed as condition to [UnitSerializer] constructor.
         */
        fun <T> ifNoExc(block: String.() -> T): String.() -> T? =
            {
                try {
                    block()
                } catch (_: Exception) {
                    null
                }
            }

        // Constants that are used by multiple serializers to build consistent
        // (and easy to change) regexes for deserialization.
        // There is no guarantee that they are used with `IGNORE_CASE` option.

        @JvmStatic
        protected val NUM_GROUP = Regex("\\s*([\\de.-]+)\\s*?")

        @JvmStatic
        protected val BITS = Regex("\\s*(?:b|(?:bit|Bit)(?:|s))\\s?")

        @JvmStatic
        protected val BYTES = Regex("\\s*(?:B|(?:byte|Byte)(?:|s))\\s?")

        @JvmStatic
        protected val NANO = Regex("\\s*(?:n|nano|Nano)\\s*?")

        @JvmStatic
        protected val MICRO = Regex("\\s*(?:micro|Micro)\\s*?")

        @JvmStatic
        protected val MILLI = Regex("\\s*(?:m|milli|Milli)\\s*?")

        @JvmStatic
        protected val KILO = Regex("\\s*(?:K|Kilo|k|kilo)\\s*?")

        @JvmStatic
        protected val KIBI = Regex("\\s*(?:Ki|Kibi|ki|kibi)\\s?")

        @JvmStatic
        protected val MEGA = Regex("\\s*(?:M|Mega|m|mega)\\s*?")

        @JvmStatic
        protected val MEBI = Regex("\\s*(?:Mi|Mebi|mi|mebi)\\s*?")

        @JvmStatic
        protected val GIGA = Regex("\\s*(?:G|Giga|g|giga)\\s*?")

        @JvmStatic
        protected val GIBI = Regex("\\s*(?:Gi|Gibi|gi|gibi)\\s*?")

        @JvmStatic
        protected val TERA = Regex("\\s*(?:T|Tera|t|tera)\\s*?")

        @JvmStatic
        protected val TEBI = Regex("\\s*(?:Ti|Tebi|ti|tebi)\\s*?")

        @JvmStatic
        protected val WATTS = Regex("\\s*(?:w|watts|W|Watts)\\s*?")

        @JvmStatic
        protected val PER = Regex("\\s*(?:p|per|/)\\s*?")

        @JvmStatic
        protected val SEC = Regex("\\s*(?:s|sec|Sec|second|Second)\\s*?")

        @JvmStatic
        protected val MIN = Regex("\\s*(?:m|min|Min|minute|Minute)\\s*?")

        @JvmStatic
        protected val HOUR = Regex("\\s*(?:h|hour|Hour)\\s*?")
    }
}

/**
 * Allows manipulating an abstract JSON representation of the class before serialization or deserialization.
 * Maps a [JsonPrimitive] to its [String] representation.
 *
 * ```json
 * // e.g.
 * "value": 3
 * // for deserialization becomes
 * "value": "3"
 */
internal open class OnlyString<T : Any>(tSerial: KSerializer<T>) : JsonTransformingSerializer<T>(tSerial) {
    override fun transformDeserialize(element: JsonElement): JsonElement = JsonPrimitive(element.toString().trim('"'))
}

/**
 * Kotlin's serialization plugin does not have a serializer for [Number].
 * ```kotlin
 * // This function allows, when the type inferred without
 * // type parameter is Number, to replace
 * Json.decodeFromString<Double>(str)
 * // with
 * Json.decNumFromStr(str)
 *
 * ```
 */
internal fun Json.decNumFromStr(str: String): Number = decodeFromString<Double>(str)
