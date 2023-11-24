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

import { schema } from 'normalizr'

const Cpu = new schema.Entity('cpus', {}, { idAttribute: 'id' })
const Gpu = new schema.Entity('gpus', {}, { idAttribute: 'id' })
const Memory = new schema.Entity('memories', {}, { idAttribute: 'id' })
const Storage = new schema.Entity('storages', {}, { idAttribute: 'id' })

export const Machine = new schema.Entity(
    'machines',
    {
        cpus: [Cpu],
        gpus: [Gpu],
        memories: [Memory],
        storages: [Storage],
    },
    { idAttribute: 'id' }
)

export const Rack = new schema.Entity('racks', { machines: [Machine] }, { idAttribute: 'id' })

export const Tile = new schema.Entity('tiles', { rack: Rack }, { idAttribute: 'id' })

export const Room = new schema.Entity('rooms', { tiles: [Tile] }, { idAttribute: 'id' })

export const Topology = new schema.Entity('topologies', { rooms: [Room] }, { idAttribute: 'id' })
