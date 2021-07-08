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

import PropTypes from 'prop-types'

export const User = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    googleId: PropTypes.string.isRequired,
    email: PropTypes.string.isRequired,
    givenName: PropTypes.string.isRequired,
    familyName: PropTypes.string.isRequired,
    authorizations: PropTypes.array.isRequired,
})

export const Project = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    datetimeCreated: PropTypes.string.isRequired,
    datetimeLastEdited: PropTypes.string.isRequired,
    topologyIds: PropTypes.array.isRequired,
    portfolioIds: PropTypes.array.isRequired,
})

export const ProcessingUnit = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    clockRateMhz: PropTypes.number.isRequired,
    numberOfCores: PropTypes.number.isRequired,
    energyConsumptionW: PropTypes.number.isRequired,
})

export const StorageUnit = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    speedMbPerS: PropTypes.number.isRequired,
    sizeMb: PropTypes.number.isRequired,
    energyConsumptionW: PropTypes.number.isRequired,
})

export const Machine = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    position: PropTypes.number.isRequired,
    cpus: PropTypes.arrayOf(PropTypes.string),
    gpus: PropTypes.arrayOf(PropTypes.string),
    memories: PropTypes.arrayOf(PropTypes.string),
    storages: PropTypes.arrayOf(PropTypes.string),
})

export const Rack = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    capacity: PropTypes.number.isRequired,
    powerCapacityW: PropTypes.number.isRequired,
    machines: PropTypes.arrayOf(PropTypes.string),
})

export const Tile = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    positionX: PropTypes.number.isRequired,
    positionY: PropTypes.number.isRequired,
    rack: PropTypes.string,
})

export const Room = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    tiles: PropTypes.arrayOf(PropTypes.string),
})

export const Topology = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    rooms: PropTypes.arrayOf(PropTypes.string),
})

export const Scheduler = PropTypes.shape({
    name: PropTypes.string.isRequired,
})

export const Trace = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
})

export const Portfolio = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    projectId: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    scenarioIds: PropTypes.arrayOf(PropTypes.string).isRequired,
    targets: PropTypes.shape({
        enabledMetrics: PropTypes.arrayOf(PropTypes.string).isRequired,
        repeatsPerScenario: PropTypes.number.isRequired,
    }).isRequired,
})

export const Scenario = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    portfolioId: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    simulation: PropTypes.shape({
        state: PropTypes.string.isRequired,
    }).isRequired,
    trace: PropTypes.shape({
        traceId: PropTypes.string.isRequired,
        trace: Trace,
        loadSamplingFraction: PropTypes.number.isRequired,
    }).isRequired,
    topology: PropTypes.shape({
        topologyId: PropTypes.string.isRequired,
        topology: Topology,
    }).isRequired,
    operational: PropTypes.shape({
        failuresEnabled: PropTypes.bool.isRequired,
        performanceInterferenceEnabled: PropTypes.bool.isRequired,
        schedulerName: PropTypes.string.isRequired,
        scheduler: Scheduler,
    }).isRequired,
    results: PropTypes.object,
})

export const WallSegment = PropTypes.shape({
    startPosX: PropTypes.number.isRequired,
    startPosY: PropTypes.number.isRequired,
    isHorizontal: PropTypes.bool.isRequired,
    length: PropTypes.number.isRequired,
})

export const InteractionLevel = PropTypes.shape({
    mode: PropTypes.string.isRequired,
    roomId: PropTypes.string,
    rackId: PropTypes.string,
})
