/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.trace.util.parquet.exporter

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray
import org.opendc.common.logger.errAndNull
import org.opendc.common.logger.logger

/**
 * Returns a serializer for [ExportColumn] of [T] based on [ExportColumn.name]. Export columns can be
 * deserialized from string values if the string matches a [ExportColumn.regex].
 *
 * ###### Note:
 * - **In order to deserialize columns, they need to be loaded at runtime**.
 * - **The serializer needs the reified type [T], meaning static deserialization
 * (e.g. `@Serializable`, `@Serializer`) will not work. The serializer for [ExportColumn] of [T] needs to be retrieved with this method.**
 *
 * It is assumed the user always know what type of column is needed from deserialization,
 * so that column can be encoded only by their name, not including their type (which would be tedious to write in json).
 *
 * ```kotlin
 * // Decode column of Foo
 * class Foo: Exportable
 * json.decodeFrom<smth>(deserializer = columnSerializer<Foo>(), <smth>)
 *
 * // Decode a collection of columns of Foo
 * json.decodeFrom<smth>(deserializer = ListSerializer(columnSerializer<Foo>()), <smth>)
 * ```
 */
public inline fun <reified T : Exportable> columnSerializer(): KSerializer<ExportColumn<T>> =
    object : KSerializer<ExportColumn<T>> {
        override val descriptor: SerialDescriptor = serialDescriptor<String>()

        override fun deserialize(decoder: Decoder): ExportColumn<T> {
            val strValue = decoder.decodeString().trim('"')
            return ExportColumn.matchingColOrNull<T>(strValue)
                ?: throw SerializationException(
                    "unable to deserialize export column '$strValue'." +
                        "Keep in mind that export columns need to be loaded by the jvm in order to be deserialized",
                )
        }

        override fun serialize(
            encoder: Encoder,
            value: ExportColumn<T>,
        ) {
            encoder.encodeString(value.name)
        }
    }

/**
 * Serializer for a [List] of [ExportColumn] of [T], with the peculiarity of
 * ignoring unrecognized column names (logging an error when an
 * unrecognized column is encountered).
 */
public class ColListSerializer<T : Exportable>(
    private val columnSerializer: KSerializer<ExportColumn<T>>,
) : KSerializer<List<ExportColumn<T>>> {
    private val listSerializer = ListSerializer(columnSerializer)
    override val descriptor: SerialDescriptor = ListSerializer(columnSerializer).descriptor

    /**
     * Unrecognized columns are ignored and an error message is logged.
     *
     * @return the decoded list of [ExportColumn]s (might be empty).
     * @throws[SerializationException] if the current element is not a [jsonArray] or its string representation.
     */
    override fun deserialize(decoder: Decoder): List<ExportColumn<T>> =
        (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonArray?.mapNotNull {
            try {
                Json.decodeFromJsonElement(columnSerializer, it)
            } catch (_: Exception) {
                LOG.errAndNull("no match found for column $it, ignoring...")
            }
        } ?: let {
            val strValue = decoder.decodeString().trim('"')
            // Basically a recursive call with a json decoder instead of the argument decoder.
            Json.decodeFromString(strValue)
        }

    override fun serialize(
        encoder: Encoder,
        value: List<ExportColumn<T>>,
    ) {
        listSerializer.serialize(encoder, value)
    }

    private companion object {
        val LOG by logger()
    }
}
