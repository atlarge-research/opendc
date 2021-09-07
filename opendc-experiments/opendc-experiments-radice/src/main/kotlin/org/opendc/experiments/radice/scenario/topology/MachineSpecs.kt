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

@file:JvmName("MachineSpecs")
package org.opendc.experiments.radice.scenario.topology

import org.opendc.simulator.compute.power.InterpolationPowerModel

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2012q1/power_ssj2008-20120306-00434.html
 */
@JvmField
val DELL_R620 = MachineSpec(
    id = "dell:r620",
    name = "PowerEdge R620 (Intel Xeon E5-2670, 2.6 GHz)",
    cpuCapacity = 2.6,
    cpuCount = 16,
    memCapacity = 128.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(54.1, 78.4, 88.5, 99.5, 115.0, 126.0, 143.0, 165.0, 196.0, 226.0, 243.0)
    )
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2012q1/power_ssj2008-20120306-00434.html
 */
@JvmField
val DELL_R620_FAST = DELL_R620.copy(
    id = "dell:r620/fast",
    name = "PowerEdge R620 (Intel Xeon 3.2 GHz)",
    cpuCapacity = 3.2,
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2012q1/power_ssj2008-20120306-00434.html
 */
@JvmField
val DELL_R620_28_CORE = DELL_R620.copy(
    id = "dell:r620/28-core",
    name = "PowerEdge R620 (Intel Xeon E5-2670, 2.6 GHz)",
    cpuCount = 28,
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2012q1/power_ssj2008-20120306-00434.html
 */
@JvmField
val DELL_R620_128_CORE = DELL_R620.copy(
    id = "dell:r620/128-core",
    name = "PowerEdge R620 (Intel Xeon E5-2670, 2.6 GHz)",
    cpuCount = 128,
    memCapacity = 1024.0,
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2010q3/power_ssj2008-20100727-00278.html
 */
@JvmField
val DELL_R710 = MachineSpec(
    id = "dell:r710",
    name = "PowerEdge R710 (Intel Xeon X5670, 2.93 GHz)",
    cpuCapacity = 2.93,
    cpuCount = 12,
    memCapacity = 96.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(64.0, 104.0, 116.0, 126.0, 137.0, 148.0, 158.0, 173.0, 190.0, 210.0, 232.0)
    )
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2012q2/power_ssj2008-20120515-00468.html
 */
@JvmField
val DELL_R820 = MachineSpec(
    id = "dell:r820",
    name = "PowerEdge R820 (Intel Xeon 2.7 GHz)",
    cpuCapacity = 2.7,
    cpuCount = 32,
    memCapacity = 756.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(110.0, 149.0, 167.0, 188.0, 218.0, 237.0, 268.0, 307.0, 358.0, 414.0, 446.0)
    )
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2012q2/power_ssj2008-20120515-00468.html
 */
@JvmField
val DELL_R820_128_CORE = DELL_R820.copy(
    id = "dell:r820/128-core",
    name = "PowerEdge R820 (Intel Xeon 2.7 GHz)",
    cpuCount = 128,
    memCapacity = 1536.0
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2019q2/power_ssj2008-20190409-00952.html
 */
@JvmField
val DELL_R740 = MachineSpec(
    id = "dell:r740",
    name = "PowerEdge R740 (Intel Xeon Platinum 8280, 2.70 GHz)",
    cpuCapacity = 2.7,
    cpuCount = 112,
    memCapacity = 1536.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(49.8, 114.0, 137.0, 159.0, 182.0, 208.0, 240.0, 277.0, 319.0, 376.0, 432.0)
    )
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2019q3/power_ssj2008-20190702-00979.html
 */
@JvmField
val DELL_R240 = MachineSpec(
    id = "dell:r240",
    name = "PowerEdge R240 (Intel Xeon E-2176G, 3.70 GHz)",
    cpuCapacity = 3.7,
    cpuCount = 6,
    memCapacity = 48.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(20.0, 25.1, 28.5, 32.3, 36.6, 41.7, 47.5, 55.9, 67.2, 81.0, 93.1)
    )
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2020q3/power_ssj2008-20200728-01041.html
 */
@JvmField
val DELL_R6515 = MachineSpec(
    id = "dell:r6515",
    name = "PowerEdge R6515 (AMD EPYC 7702P, 2.00 GHz)",
    cpuCapacity = 2.0,
    cpuCount = 64,
    memCapacity = 1536.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(54.0, 123.0, 139.0, 150.0, 161.0, 169.0, 181.0, 194.0, 207.0, 218.0, 231.0)
    )
)

@JvmField
val DELL_R6515_FASTER = DELL_R6515.copy(
    id = "dell:r6515/faster",
    name = "PowerEdge R6515 (AMD EPYC 7702P, 2.60 GHz)",
    cpuCapacity = 2.6
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2019q3/power_ssj2008-20190911-01005.html
 */
@JvmField
val DELL_R7515 = MachineSpec(
    id = "dell:r7515",
    name = "PowerEdge R7515 (AMD EPYC 7742, 2.25 GHz)",
    cpuCapacity = 2.25,
    cpuCount = 64,
    memCapacity = 1536.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(99.5, 117.0, 130.0, 141.0, 151.0, 164.0, 186.0, 205.0, 220.0, 236.0, 246.0)
    )
)

@JvmField
val DELL_R7515_FASTER = DELL_R7515.copy(
    id = "dell:r7515/faster",
    name = "PowerEdge R7515 (AMD EPYC 7742, 2.60 GHz)",
    cpuCapacity = 2.6
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2019q3/power_ssj2008-20190911-01005.html
 */
@JvmField
val DELL_R7525 = MachineSpec(
    id = "dell:r7525",
    name = "PowerEdge R7525 (AMD EPYC 7702, 2.00 GHz)",
    cpuCapacity = 2.00,
    cpuCount = 128,
    memCapacity = 1536.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(72.0, 181.0, 207.0, 229.0, 250.0, 270.0, 294.0, 321.0, 347.0, 370.0, 393.0)
    )
)

@JvmField
val DELL_R7525_FASTER = DELL_R7525.copy(
    id = "dell:r7525/faster",
    name = "PowerEdge R7525 (AMD EPYC 7702, 2.60 GHz)",
    cpuCapacity = 2.6
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2021q1/power_ssj2008-20210221-01065.html
 */
@JvmField
val HPE_DL345_GEN10_PLUS = MachineSpec(
    id = "hpe:dl345-gen10-plus",
    name = "ProLiant DL345 Gen10 Plus (AMD EPYC 7763, 2.45 GHz)",
    cpuCapacity = 2.45,
    cpuCount = 64,
    memCapacity = 1536.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(58.0, 104.0, 120.0, 134.0, 150.0, 169.0, 197.0, 217.0, 236.0, 255.0, 272.0)
    )
)

@JvmField
val HPE_DL345_GEN10_PLUS_FASTER = HPE_DL345_GEN10_PLUS.copy(
    id = "hpe:dl345-gen10-plus/faster",
    name = "ProLiant DL345 Gen10 Plus (AMD EPYC 7763, 2.6 GHz)",
    cpuCapacity = 2.6
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2021q2/power_ssj2008-20210601-01103.html
 */
@JvmField
val HPE_DL380_GEN10_PLUS = MachineSpec(
    id = "hpe:dl380-gen10-plus",
    name = "ProLiant DL380 Gen10 Plus (Intel Xeon Platinum 8380, 2.30 GHz)",
    cpuCapacity = 2.30,
    cpuCount = 80,
    memCapacity = 1536.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(127.0, 198.0, 226.0, 253.0, 281.0, 314.0, 354.0, 405.0, 464.0, 576.0, 619.0)
    )
)

@JvmField
val HPE_DL380_GEN10_PLUS_FASTER = HPE_DL380_GEN10_PLUS.copy(
    id = "hpe:dl380-gen10-plus/faster",
    name = "ProLiant DL380 Gen10 Plus (Intel Xeon Platinum 8380, 2.60 GHz)",
    cpuCapacity = 2.6
)

/**
 * Based on https://www.spec.org/power_ssj2008/results/res2021q2/power_ssj2008-20210601-01103.html
 */
@JvmField
val HPE_DL110_GEN10_PLUS = MachineSpec(
    id = "hpe:dl110-gen10-plus",
    name = "ProLiant DL110 Gen10 Plus (Intel Xeon Gold 6314U, 2.30 GHz)",
    cpuCapacity = 2.30,
    cpuCount = 32,
    memCapacity = 756.0,
    powerModel = InterpolationPowerModel(
        doubleArrayOf(93.8, 104.0, 116.0, 130.0, 143.0, 156.0, 172.0, 197.0, 250.0, 287.0, 307.0)
    )
)

@JvmField
val HPE_DL110_GEN10_PLUS_FASTER = HPE_DL110_GEN10_PLUS.copy(
    id = "hpe:dl110-gen10-plus/faster",
    name = "ProLiant DL110 Gen10 Plus (Intel Xeon Gold 6314U, 2.60 GHz)",
    cpuCapacity = 2.6
)
