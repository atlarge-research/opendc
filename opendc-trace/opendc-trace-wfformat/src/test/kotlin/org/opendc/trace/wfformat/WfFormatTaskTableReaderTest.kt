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

package org.opendc.trace.wfformat

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS

/**
 * Test suite for the [WfFormatTaskTableReader] class.
 */
internal class WfFormatTaskTableReaderTest {
    /**
     * The [JsonFactory] used to construct the parser.
     */
    private val factory = JsonFactory()

    @Test
    fun testEmptyInput() {
        val content = ""
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertFalse(reader.nextRow())
        reader.close()
    }

    @Test
    fun testTopLevelArrayInput() {
        val content = "[]"
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertThrows<JsonParseException> {
            while (reader.nextRow()) {
                continue
            }
        }

        reader.close()
    }

    @Test
    fun testNoWorkflow() {
        val content = """
        {
            "name": "eager-nextflow-chameleon"
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertDoesNotThrow {
            while (reader.nextRow()) {
                continue
            }
        }

        reader.close()
    }

    @Test
    fun testWorkflowArrayType() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": []
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertThrows<JsonParseException> {
            while (reader.nextRow()) {
                continue
            }
        }

        reader.close()
    }

    @Test
    fun testWorkflowNullType() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": null
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertThrows<JsonParseException> {
            while (reader.nextRow()) {
                continue
            }
        }

        reader.close()
    }

    @Test
    fun testNoJobs() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": {

            }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertDoesNotThrow { reader.nextRow() }

        reader.close()
    }

    @Test
    fun testJobsObjectType() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": { "jobs": {} }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertThrows<JsonParseException> { reader.nextRow() }

        reader.close()
    }

    @Test
    fun testJobsNullType() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": { "jobs": null }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertThrows<JsonParseException> { reader.nextRow() }

        reader.close()
    }

    @Test
    fun testJobsInvalidChildType() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": {
                "jobs": [1]
            }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertThrows<JsonParseException> { reader.nextRow() }

        reader.close()
    }

    @Test
    fun testJobsValidChildType() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": {
                "jobs": [
                    {
                        "name": "test"
                    }
                ]
            }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertTrue(reader.nextRow())
        assertEquals("test", reader.getString(TASK_ID))
        assertFalse(reader.nextRow())

        reader.close()
    }

    @Test
    fun testJobsInvalidParents() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": {
                "jobs": [
                    {
                        "name": "test",
                        "parents": 1,
                    }
                ]
            }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertThrows<JsonParseException> { reader.nextRow() }

        reader.close()
    }

    @Test
    fun testJobsInvalidParentsItem() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": {
                "jobs": [
                    {
                        "name": "test",
                        "parents": [1],
                    }
                ]
            }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertThrows<JsonParseException> { reader.nextRow() }

        reader.close()
    }

    @Test
    fun testJobsValidParents() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": {
                "jobs": [
                    {
                        "name": "test",
                        "parents": ["1"]
                    }
                ]
            }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertTrue(reader.nextRow())
        assertEquals(setOf("1"), reader.getSet(TASK_PARENTS, String::class.java))
        assertFalse(reader.nextRow())

        reader.close()
    }

    @Test
    fun testJobsInvalidSecondEntry() {
        val content = """
        {
            "workflow": {
                "jobs": [
                    {
                        "name": "test",
                        "parents": ["1"]
                    },
                    "test"
                ]
            }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertDoesNotThrow { reader.nextRow() }
        assertThrows<JsonParseException> { reader.nextRow() }

        reader.close()
    }

    @Test
    fun testDuplicateJobsArray() {
        val content = """
        {
            "name": "eager-nextflow-chameleon",
            "workflow": {
                "jobs": [
                    {
                        "name": "test",
                        "parents": ["1"]
                    }
                ],
                "jobs": [
                    {
                        "name": "test2",
                        "parents": ["test"]
                    }
                ]
            }
        }
        """.trimIndent()
        val parser = factory.createParser(content)
        val reader = WfFormatTaskTableReader(parser)

        assertTrue(reader.nextRow())
        assertTrue(reader.nextRow())
        assertEquals("test2", reader.getString(TASK_ID))
        assertFalse(reader.nextRow())

        reader.close()
    }
}
