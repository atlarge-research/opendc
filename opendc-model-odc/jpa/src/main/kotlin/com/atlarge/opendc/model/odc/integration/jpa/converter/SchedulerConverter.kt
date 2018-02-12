/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.model.odc.integration.jpa.converter

import com.atlarge.opendc.model.odc.platform.scheduler.FifoScheduler
import com.atlarge.opendc.model.odc.platform.scheduler.Scheduler
import com.atlarge.opendc.model.odc.platform.scheduler.SrtfScheduler
import javax.persistence.AttributeConverter

/**
 * An internal [AttributeConverter] that maps a name of a scheduler to the actual scheduler implementation.
 * The converter currently chooses between the following two schedulers:
 * - [FifoScheduler] (default)
 * - [SrtfScheduler]
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class SchedulerConverter : AttributeConverter<Scheduler, String> {
    /**
     * Converts the data stored in the database column into the
     * value to be stored in the entity attribute.
     * Note that it is the responsibility of the converter writer to
     * specify the correct dbData type for the corresponding column
     * for use by the JDBC driver: i.e., persistence providers are
     * not expected to do such type conversion.
     *
     * @param dbData the data from the database column to be converted
     * @return the converted value to be stored in the entity attribute
     */
    override fun convertToEntityAttribute(dbData: String?): Scheduler = when (dbData?.toUpperCase()) {
        "SRTF" -> SrtfScheduler()
        else -> FifoScheduler()
    }

    /**
     * Converts the value stored in the entity attribute into the
     * data representation to be stored in the database.
     *
     * @param attribute the entity attribute value to be converted
     * @return the converted data to be stored in the database column
     */
    override fun convertToDatabaseColumn(attribute: Scheduler?): String =
        attribute?.name?.toUpperCase() ?: "FIFO"
}
