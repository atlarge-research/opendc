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

package org.opendc.trace

/**
 * A table is collection of rows consisting of typed columns.
 */
public interface Table {
    /**
     * The name of the table.
     */
    public val name: String

    /**
     * The columns in this table.
     */
    public val columns: List<TableColumn>

    /**
     * Open a [TableReader] for a projection of this table.
     *
     * @param projection The names of the columns to fetch from the table or `null` if no projection is performed.
     */
    public fun newReader(projection: List<String>? = null): TableReader

    /**
     * Open a [TableWriter] for this table.
     *
     * @throws UnsupportedOperationException if writing is not supported by the table.
     */
    public fun newWriter(): TableWriter
}
