import PropTypes from 'prop-types'

const Shapes = {}

Shapes.User = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    googleId: PropTypes.string.isRequired,
    email: PropTypes.string.isRequired,
    givenName: PropTypes.string.isRequired,
    familyName: PropTypes.string.isRequired,
    authorizations: PropTypes.array.isRequired,
})

Shapes.Project = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    datetimeCreated: PropTypes.string.isRequired,
    datetimeLastEdited: PropTypes.string.isRequired,
    topologyIds: PropTypes.array.isRequired,
    portfolioIds: PropTypes.array.isRequired,
})

Shapes.Authorization = PropTypes.shape({
    userId: PropTypes.string.isRequired,
    user: Shapes.User,
    projectId: PropTypes.string.isRequired,
    project: Shapes.Project,
    authorizationLevel: PropTypes.string.isRequired,
})

Shapes.ProcessingUnit = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    clockRateMhz: PropTypes.number.isRequired,
    numberOfCores: PropTypes.number.isRequired,
    energyConsumptionW: PropTypes.number.isRequired,
})

Shapes.StorageUnit = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    speedMbPerS: PropTypes.number.isRequired,
    sizeMb: PropTypes.number.isRequired,
    energyConsumptionW: PropTypes.number.isRequired,
})

Shapes.Machine = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    rackId: PropTypes.string.isRequired,
    position: PropTypes.number.isRequired,
    cpuIds: PropTypes.arrayOf(PropTypes.string.isRequired),
    cpus: PropTypes.arrayOf(Shapes.ProcessingUnit),
    gpuIds: PropTypes.arrayOf(PropTypes.string.isRequired),
    gpus: PropTypes.arrayOf(Shapes.ProcessingUnit),
    memoryIds: PropTypes.arrayOf(PropTypes.string.isRequired),
    memories: PropTypes.arrayOf(Shapes.StorageUnit),
    storageIds: PropTypes.arrayOf(PropTypes.string.isRequired),
    storages: PropTypes.arrayOf(Shapes.StorageUnit),
})

Shapes.Rack = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    capacity: PropTypes.number.isRequired,
    powerCapacityW: PropTypes.number.isRequired,
    machines: PropTypes.arrayOf(Shapes.Machine),
})

Shapes.Tile = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    roomId: PropTypes.string.isRequired,
    positionX: PropTypes.number.isRequired,
    positionY: PropTypes.number.isRequired,
    rackId: PropTypes.string,
    rack: Shapes.Rack,
})

Shapes.Room = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    tiles: PropTypes.arrayOf(Shapes.Tile),
})

Shapes.Topology = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    rooms: PropTypes.arrayOf(Shapes.Room),
})

Shapes.Scheduler = PropTypes.shape({
    name: PropTypes.string.isRequired,
})

Shapes.Trace = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
})

Shapes.Portfolio = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    projectId: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    scenarioIds: PropTypes.arrayOf(PropTypes.string).isRequired,
    targets: PropTypes.shape({
        enabledMetrics: PropTypes.arrayOf(PropTypes.string).isRequired,
        repeatsPerScenario: PropTypes.number.isRequired,
    }).isRequired,
})

Shapes.Scenario = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    portfolioId: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    simulationState: PropTypes.string.isRequired,
    trace: PropTypes.shape({
        traceId: PropTypes.string.isRequired,
        trace: Shapes.Trace,
        loadSamplingFraction: PropTypes.number.isRequired,
    }).isRequired,
    topology: PropTypes.shape({
        topologyId: PropTypes.string.isRequired,
        topology: Shapes.Topology,
    }).isRequired,
    operational: PropTypes.shape({
        failuresEnabled: PropTypes.bool.isRequired,
        performanceInterferenceEnabled: PropTypes.bool.isRequired,
        schedulerName: PropTypes.string.isRequired,
        scheduler: Shapes.Scheduler,
    }).isRequired,
})

Shapes.WallSegment = PropTypes.shape({
    startPosX: PropTypes.number.isRequired,
    startPosY: PropTypes.number.isRequired,
    isHorizontal: PropTypes.bool.isRequired,
    length: PropTypes.number.isRequired,
})

Shapes.InteractionLevel = PropTypes.shape({
    mode: PropTypes.string.isRequired,
    roomId: PropTypes.string,
    rackId: PropTypes.string,
})

export default Shapes
