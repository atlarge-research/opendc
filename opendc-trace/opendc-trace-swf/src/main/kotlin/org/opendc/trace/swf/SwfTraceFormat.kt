/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.trace.swf

import org.opendc.trace.spi.TraceFormat
import java.net.URL
import java.nio.file.Paths
import kotlin.io.path.exists

/**
 * Support for the Standard Workload Format (SWF) in OpenDC.
 *
 * The standard is defined by the PWA, see here: https://www.cse.huji.ac.il/labs/parallel/workload/swf.html
 */
public class SwfTraceFormat : TraceFormat {
    override val name: String = "swf"

    override fun open(url: URL): SwfTrace {
        val path = Paths.get(url.toURI())
        require(path.exists()) { "URL $url does not exist" }
        return SwfTrace(path)
    }
}
