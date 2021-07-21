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

import { CPU_UNITS, GPU_UNITS, MEMORY_UNITS, STORAGE_UNITS } from '../../../util/unit-specifications'
import machine from './machine'
import rack from './rack'
import room from './room'
import tile from './tile'
import topology from './topology'

function objects(state = {}, action) {
    return {
        cpus: CPU_UNITS,
        gpus: GPU_UNITS,
        memories: MEMORY_UNITS,
        storages: STORAGE_UNITS,
        machines: machine(state.machines, action, state),
        racks: rack(state.racks, action, state),
        tiles: tile(state.tiles, action, state),
        rooms: room(state.rooms, action, state),
        root: topology(state.root, action, state),
    }
}

export default objects
