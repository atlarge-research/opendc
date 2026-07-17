/*
 * Copyright (c) 2017 AtLarge Research
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

plugins {
    `dokka-conventions`
    `jacoco-aggregation`
}

group = "org.opendc"
version = "3.0-SNAPSHOT"

dependencies {
    // Dokka 2 (DGP v2) dropped the automatic `dokkaHtmlMultiModule` aggregation: the aggregating
    // project must consume each documented module explicitly. Register every subproject that applies
    // the Dokka plugin (i.e. every `kotlin-library-conventions` module). `withId` defers each
    // registration until that plugin is actually applied, so it does not depend on the order in which
    // Gradle configures the projects.
    subprojects.forEach { subproject ->
        subproject.plugins.withId("org.jetbrains.dokka") {
            dokka(subproject)
        }
    }
}
