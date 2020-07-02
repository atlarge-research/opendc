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

Shapes.Simulation = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    datetimeCreated: PropTypes.string.isRequired,
    datetimeLastEdited: PropTypes.string.isRequired,
    topologyIds: PropTypes.array.isRequired,
    experimentIds: PropTypes.array.isRequired,
})

Shapes.Authorization = PropTypes.shape({
    userId: PropTypes.string.isRequired,
    user: Shapes.User,
    simulationId: PropTypes.string.isRequired,
    simulation: Shapes.Simulation,
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

Shapes.Experiment = PropTypes.shape({
    _id: PropTypes.string.isRequired,
    simulationId: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    topology: Shapes.Topology,
    traceId: PropTypes.string.isRequired,
    trace: Shapes.Trace,
    schedulerName: PropTypes.string.isRequired,
    scheduler: Shapes.Scheduler,
    name: PropTypes.string.isRequired,
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
