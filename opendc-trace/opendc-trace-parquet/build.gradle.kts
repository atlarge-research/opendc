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

description = "Parquet helpers for traces in OpenDC"

/* Build configuration */
plugins {
    `kotlin-library-conventions`
}

dependencies {
    /* This configuration is necessary for a slim dependency on Apache Parquet */
    api(libs.parquet) {
        exclude(group = "org.apache.hadoop")
    }
    api(libs.hadoop.common) {
        exclude(group = "org.slf4j", module = "slf4j-reload4j")
        exclude(group = "ch.qos.reload4j", module = "reload4j")
        exclude(group = "org.apache.hadoop")
        exclude(group = "org.apache.curator")
        exclude(group = "org.apache.zookeeper")
        exclude(group = "org.apache.kerby")
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "org.apache.htrace")
        exclude(group = "commons-cli")
        exclude(group = "javax.servlet")
        exclude(group = "javax.servlet.jsp")
        exclude(group = "org.eclipse.jetty")
        exclude(group = "com.sun.jersey")
        exclude(group = "com.jcraft")
        exclude(group = "dnsjava")
        exclude(group = "org.apache.avro")
        exclude(group = "com.google.code.gson")
    }
    runtimeOnly(libs.hadoop.mapreduce.client.core) {
        isTransitive = false
    }

    testRuntimeOnly(libs.slf4j.simple)
}
