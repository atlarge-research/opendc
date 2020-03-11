/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.metal.power

import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import com.atlarge.opendc.core.power.PowerModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * A power model which emits a single value.
 */
public fun ConstantPowerModel(value: Double): PowerModel<BareMetalDriver> = { _ -> flowOf(value) }

/**
 * A power model that assumes a naive linear relation between power usage and host CPU utilization.
 *
 * @param idle The power draw in Watts on idle.
 * @param max The maximum power draw in Watts of the server.
 */
public fun LinearLoadPowerModel(idle: Double, max: Double): PowerModel<BareMetalDriver> = { driver ->
    driver.usage.map { load -> (max - idle) * load + idle }
}
