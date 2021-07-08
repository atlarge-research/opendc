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

const Cpu = new schema.Entity('cpu', {}, { idAttribute: '_id' })
const Gpu = new schema.Entity('gpu', {}, { idAttribute: '_id' })
const Memory = new schema.Entity('memory', {}, { idAttribute: '_id' })
const Storage = new schema.Entity('storage', {}, { idAttribute: '_id' })

export const Machine = new schema.Entity(
    'machine',
    {
        cpus: [Cpu],
        gpus: [Gpu],
        memories: [Memory],
        storages: [Storage],
    },
    { idAttribute: '_id' }
)

export const Rack = new schema.Entity('rack', { machines: [Machine] }, { idAttribute: '_id' })

export const Tile = new schema.Entity('tile', { rack: Rack }, { idAttribute: '_id' })

export const Room = new schema.Entity('room', { tiles: [Tile] }, { idAttribute: '_id' })

export const Topology = new schema.Entity('topology', { rooms: [Room] }, { idAttribute: '_id' })
