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

package org.opendc.compute.simulator.telemetry.parquet

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.opendc.common.logger.logger
import org.opendc.compute.simulator.telemetry.table.HostTableReader
import org.opendc.compute.simulator.telemetry.table.ServiceTableReader
import org.opendc.compute.simulator.telemetry.table.TaskTableReader
import org.opendc.trace.util.parquet.exporter.ColListSerializer
import org.opendc.trace.util.parquet.exporter.ExportColumn
import org.opendc.trace.util.parquet.exporter.Exportable
import org.opendc.trace.util.parquet.exporter.columnSerializer

/**
 * Aggregates the necessary settings to personalize the output
 * parquet files for compute workloads.
 *
 * @param[hostExportColumns]     the columns that will be included in the `host.parquet` raw output file.
 * @param[taskExportColumns]   the columns that will be included in the `task.parquet` raw output file.
 * @param[serviceExportColumns]  the columns that will be included in the `service.parquet` raw output file.
 */
@Serializable(with = ComputeExportConfig.Companion.ComputeExportConfigSerializer::class)
public data class ComputeExportConfig(
    public val hostExportColumns: Set<ExportColumn<HostTableReader>>,
    public val taskExportColumns: Set<ExportColumn<TaskTableReader>>,
    public val serviceExportColumns: Set<ExportColumn<ServiceTableReader>>,
) {
    public constructor(
        hostExportColumns: Collection<ExportColumn<HostTableReader>>,
        taskExportColumns: Collection<ExportColumn<TaskTableReader>>,
        serviceExportColumns: Collection<ExportColumn<ServiceTableReader>>,
    ) : this(
        hostExportColumns.toSet() + DfltHostExportColumns.BASE_EXPORT_COLUMNS,
        taskExportColumns.toSet() + DfltTaskExportColumns.BASE_EXPORT_COLUMNS,
        serviceExportColumns.toSet() + DfltServiceExportColumns.BASE_EXPORT_COLUMNS,
    )

    /**
     * @return formatted string representing the export config.
     */
    public fun fmt(): String =
        """
        | === Compute Export Config ===
        | Host columns    : ${hostExportColumns.map { it.name }.toString().trim('[', ']')}
        | Task columns  : ${taskExportColumns.map { it.name }.toString().trim('[', ']')}
        | Service columns : ${serviceExportColumns.map { it.name }.toString().trim('[', ']')}
        """.trimIndent()

    public companion object {
        internal val LOG by logger()

        /**
         * Force the jvm to load the default [ExportColumn]s relevant to compute export,
         * so that they are available for deserialization.
         */
        public fun loadDfltColumns() {
            DfltHostExportColumns
            DfltTaskExportColumns
            DfltServiceExportColumns
        }

        /**
         * Config that includes all columns defined in [DfltHostExportColumns],
         * [DfltTaskExportColumns], [DfltServiceExportColumns] among all other loaded
         * columns for [HostTableReader], [TaskTableReader] and [ServiceTableReader].
         */
        public val ALL_COLUMNS: ComputeExportConfig by lazy {
            ComputeExportConfig.Companion.loadDfltColumns()
            ComputeExportConfig(
                hostExportColumns = ExportColumn.getAllLoadedColumns(),
                taskExportColumns = ExportColumn.getAllLoadedColumns(),
                serviceExportColumns = ExportColumn.getAllLoadedColumns(),
            )
        }

        /**
         * A runtime [KSerializer] is needed for reasons explained in [columnSerializer] docs.
         *
         * This serializer makes use of reified column serializers for the 2 properties.
         */
        internal object ComputeExportConfigSerializer : KSerializer<ComputeExportConfig> {
            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor("org.opendc.compute.telemetry.export.parquet.ComputeExportConfig") {
                    element(
                        "hostExportColumns",
                        ListSerializer(columnSerializer<HostTableReader>()).descriptor,
                    )
                    element(
                        "taskExportColumns",
                        ListSerializer(columnSerializer<TaskTableReader>()).descriptor,
                    )
                    element(
                        "serviceExportColumns",
                        ListSerializer(columnSerializer<ServiceTableReader>()).descriptor,
                    )
                }

            override fun deserialize(decoder: Decoder): ComputeExportConfig {
                val jsonDec =
                    (decoder as? JsonDecoder) ?: let {
                        // Basically a recursive call with a JsonDecoder.
                        return json.decodeFromString(decoder.decodeString().trim('"'))
                    }

                // Loads the default columns so that they are available for deserialization.
                ComputeExportConfig.Companion.loadDfltColumns()
                val elem = jsonDec.decodeJsonElement().jsonObject

                val hostFields: List<ExportColumn<HostTableReader>> = elem["hostExportColumns"].toFieldList()
                val taskFields: List<ExportColumn<TaskTableReader>> = elem["taskExportColumns"].toFieldList()
                val serviceFields: List<ExportColumn<ServiceTableReader>> = elem["serviceExportColumns"].toFieldList()

                return ComputeExportConfig(
                    hostExportColumns = hostFields,
                    taskExportColumns = taskFields,
                    serviceExportColumns = serviceFields,
                )
            }

            override fun serialize(
                encoder: Encoder,
                value: ComputeExportConfig,
            ) {
                encoder.encodeStructure(ComputeExportConfig.Companion.ComputeExportConfigSerializer.descriptor) {
                    encodeSerializableElement(
                        ComputeExportConfig.Companion.ComputeExportConfigSerializer.descriptor,
                        0,
                        ColListSerializer(columnSerializer<HostTableReader>()),
                        value.hostExportColumns.toList(),
                    )
                    encodeSerializableElement(
                        ComputeExportConfig.Companion.ComputeExportConfigSerializer.descriptor,
                        1,
                        ColListSerializer(columnSerializer<TaskTableReader>()),
                        value.taskExportColumns.toList(),
                    )
                    encodeSerializableElement(
                        ComputeExportConfig.Companion.ComputeExportConfigSerializer.descriptor,
                        2,
                        ColListSerializer(columnSerializer<ServiceTableReader>()),
                        value.serviceExportColumns.toList(),
                    )
                }
            }
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

private inline fun <reified T : Exportable> JsonElement?.toFieldList(): List<ExportColumn<T>> =
    this?.let {
        json.decodeFromJsonElement(ColListSerializer(columnSerializer<T>()), it)
    }?.ifEmpty {
        ComputeExportConfig.Companion.LOG.warn(
            "deserialized list of export columns for exportable ${T::class.simpleName} " +
                "produced empty list, falling back to all loaded columns",
        )
        ExportColumn.getAllLoadedColumns<T>()
    } ?: ExportColumn.getAllLoadedColumns<T>()
