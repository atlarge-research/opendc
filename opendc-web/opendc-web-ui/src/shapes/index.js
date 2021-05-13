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

export const Authorization = PropTypes.shape({
    userId: PropTypes.string.isRequired,
    user: User,
    projectId: PropTypes.string.isRequired,
    project: Project,
    authorizationLevel: PropTypes.string.isRequired,
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
    rackId: PropTypes.string.isRequired,
    position: PropTypes.number.isRequired,
    cpuIds: PropTypes.arrayOf(PropTypes.string.isRequired),
    cpus: PropTypes.arrayOf(ProcessingUnit),
    gpuIds: PropTypes.arrayOf(PropTypes.string.isRequired),
    gpus: PropTypes.arrayOf(ProcessingUnit),
    memoryIds: PropTypes.arrayOf(PropTypes.string.isRequired),
    memories: PropTypes.arrayOf(StorageUnit),
    storageIds: PropTypes.arrayOf(PropTypes.string.isRequired),
    storages: PropTypes.arrayOf(StorageUnit),
})

export const Rack = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    capacity: PropTypes.number.isRequired,
    powerCapacityW: PropTypes.number.isRequired,
    machines: PropTypes.arrayOf(Machine),
})

export const Tile = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    roomId: PropTypes.string.isRequired,
    positionX: PropTypes.number.isRequired,
    positionY: PropTypes.number.isRequired,
    rackId: PropTypes.string,
    rack: Rack,
})

export const Room = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    tiles: PropTypes.arrayOf(Tile),
})

export const Topology = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    rooms: PropTypes.arrayOf(Room),
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
