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

export const ProjectRole = PropTypes.oneOf(['VIEWER', 'EDITOR', 'OWNER'])

export const Project = PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    createdAt: PropTypes.string.isRequired,
    updatedAt: PropTypes.string.isRequired,
    role: ProjectRole,
})

export const ProcessingUnit = PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    clockRateMhz: PropTypes.number.isRequired,
    numberOfCores: PropTypes.number.isRequired,
    energyConsumptionW: PropTypes.number.isRequired,
})

export const StorageUnit = PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    speedMbPerS: PropTypes.number.isRequired,
    sizeMb: PropTypes.number.isRequired,
    energyConsumptionW: PropTypes.number.isRequired,
})

export const Machine = PropTypes.shape({
    id: PropTypes.string.isRequired,
    position: PropTypes.number.isRequired,
    cpus: PropTypes.arrayOf(PropTypes.oneOfType([ProcessingUnit, PropTypes.string])),
    gpus: PropTypes.arrayOf(PropTypes.oneOfType([ProcessingUnit, PropTypes.string])),
    memories: PropTypes.arrayOf(PropTypes.oneOfType([StorageUnit, PropTypes.string])),
    storages: PropTypes.arrayOf(PropTypes.oneOfType([StorageUnit, PropTypes.string])),
})

export const Rack = PropTypes.shape({
    id: PropTypes.string.isRequired,
    capacity: PropTypes.number.isRequired,
    powerCapacityW: PropTypes.number.isRequired,
    machines: PropTypes.arrayOf(PropTypes.oneOfType([Machine, PropTypes.string])),
})

export const Tile = PropTypes.shape({
    id: PropTypes.string.isRequired,
    positionX: PropTypes.number.isRequired,
    positionY: PropTypes.number.isRequired,
    rack: PropTypes.oneOfType([Rack, PropTypes.string]),
})

export const Room = PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    tiles: PropTypes.arrayOf(PropTypes.oneOfType([Tile, PropTypes.string])),
})

export const Topology = PropTypes.shape({
    id: PropTypes.number.isRequired,
    number: PropTypes.number.isRequired,
    project: Project.isRequired,
    name: PropTypes.string.isRequired,
    rooms: PropTypes.arrayOf(PropTypes.oneOfType([Room, PropTypes.string])),
})

export const Phenomena = PropTypes.shape({
    failures: PropTypes.bool.isRequired,
    interference: PropTypes.bool.isRequired,
})

export const Scheduler = PropTypes.shape({
    name: PropTypes.string.isRequired,
})

export const Trace = PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
})

export const Workload = PropTypes.shape({
    trace: Trace.isRequired,
    samplingFraction: PropTypes.number.isRequired,
})

export const Targets = PropTypes.shape({
    repeats: PropTypes.number.isRequired,
    metrics: PropTypes.arrayOf(PropTypes.string).isRequired,
})

export const TopologySummary = PropTypes.shape({
    id: PropTypes.number.isRequired,
    number: PropTypes.number.isRequired,
    project: Project.isRequired,
    name: PropTypes.string.isRequired,
})

export const PortfolioSummary = PropTypes.shape({
    id: PropTypes.number.isRequired,
    number: PropTypes.number.isRequired,
    project: Project.isRequired,
    name: PropTypes.string.isRequired,
    targets: PropTypes.shape({
        repeats: PropTypes.number.isRequired,
        metrics: PropTypes.arrayOf(PropTypes.string).isRequired,
    }).isRequired,
})

export const ScenarioSummary = PropTypes.shape({
    id: PropTypes.number.isRequired,
    number: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    workload: Workload.isRequired,
    topology: TopologySummary.isRequired,
    phenomena: Phenomena.isRequired,
    schedulerName: PropTypes.string.isRequired,
    results: PropTypes.object,
})

export const JobState = PropTypes.oneOf(['PENDING', 'CLAIMED', 'RUNNING', 'FAILED', 'FINISHED'])

export const Job = PropTypes.shape({
    id: PropTypes.number.isRequired,
    state: JobState.isRequired,
    createdAt: PropTypes.string.isRequired,
    updatedAt: PropTypes.string.isRequired,
    results: PropTypes.object,
})

export const Scenario = PropTypes.shape({
    id: PropTypes.number.isRequired,
    number: PropTypes.number.isRequired,
    project: Project.isRequired,
    portfolio: PortfolioSummary.isRequired,
    name: PropTypes.string.isRequired,
    workload: Workload.isRequired,
    topology: TopologySummary.isRequired,
    phenomena: Phenomena.isRequired,
    schedulerName: PropTypes.string.isRequired,
    job: Job.isRequired,
})

export const Portfolio = PropTypes.shape({
    id: PropTypes.number.isRequired,
    number: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    project: Project.isRequired,
    targets: Targets.isRequired,
    scenarios: PropTypes.arrayOf(ScenarioSummary).isRequired,
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

export const Status = PropTypes.oneOf(['idle', 'loading', 'error', 'success'])
