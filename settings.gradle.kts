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
rootProject.name = "opendc"

include(":opendc-platform")
include(":opendc-compute:opendc-compute-api")
include(":opendc-compute:opendc-compute-service")
include(":opendc-compute:opendc-compute-simulator")
include(":opendc-compute:opendc-compute-workload")
include(":opendc-workflow:opendc-workflow-api")
include(":opendc-workflow:opendc-workflow-service")
include(":opendc-faas:opendc-faas-api")
include(":opendc-faas:opendc-faas-service")
include(":opendc-faas:opendc-faas-simulator")
include(":opendc-experiments:opendc-experiments-capelin")
include(":opendc-experiments:opendc-experiments-serverless20")
include(":opendc-experiments:opendc-experiments-tf20")
include(":opendc-web:opendc-web-api")
include(":opendc-web:opendc-web-ui")
include(":opendc-web:opendc-web-runner")
include(":opendc-simulator:opendc-simulator-core")
include(":opendc-simulator:opendc-simulator-resources")
include(":opendc-simulator:opendc-simulator-power")
include(":opendc-simulator:opendc-simulator-network")
include(":opendc-simulator:opendc-simulator-compute")
include(":opendc-telemetry:opendc-telemetry-api")
include(":opendc-telemetry:opendc-telemetry-sdk")
include(":opendc-telemetry:opendc-telemetry-compute")
include(":opendc-trace:opendc-trace-api")
include(":opendc-trace:opendc-trace-gwf")
include(":opendc-trace:opendc-trace-swf")
include(":opendc-trace:opendc-trace-wtf")
include(":opendc-trace:opendc-trace-wfformat")
include(":opendc-trace:opendc-trace-bitbrains")
include(":opendc-trace:opendc-trace-azure")
include(":opendc-trace:opendc-trace-opendc")
include(":opendc-trace:opendc-trace-parquet")
include(":opendc-harness:opendc-harness-api")
include(":opendc-harness:opendc-harness-engine")
include(":opendc-harness:opendc-harness-cli")
include(":opendc-harness:opendc-harness-junit5")
include(":opendc-utils")

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
