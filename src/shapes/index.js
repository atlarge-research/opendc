import PropTypes from 'prop-types';

const Shapes = {};

Shapes.User = PropTypes.shape({
    id: PropTypes.number.isRequired,
    googleId: PropTypes.string.isRequired,
    email: PropTypes.string.isRequired,
    givenName: PropTypes.string.isRequired,
    familyName: PropTypes.string.isRequired,
});

Shapes.Simulation = PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    datetimeCreated: PropTypes.string.isRequired,
    datetimeLastEdited: PropTypes.string.isRequired,
});

Shapes.Authorization = PropTypes.shape({
    userId: PropTypes.number.isRequired,
    user: Shapes.User,
    simulationId: PropTypes.number.isRequired,
    simulation: Shapes.Simulation,
    authorizationLevel: PropTypes.string.isRequired,
});

Shapes.FailureModel = PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    rate: PropTypes.number.isRequired,
});

Shapes.ProcessingUnit = PropTypes.shape({
    id: PropTypes.number.isRequired,
    manufacturer: PropTypes.string.isRequired,
    family: PropTypes.string.isRequired,
    generation: PropTypes.string.isRequired,
    model: PropTypes.string.isRequired,
    clockRateMhz: PropTypes.number.isRequired,
    numberOfCores: PropTypes.number.isRequired,
    energyConsumptionW: PropTypes.number.isRequired,
    failureModelId: PropTypes.number.isRequired,
    failureModel: Shapes.FailureModel,
});

Shapes.StorageUnit = PropTypes.shape({
    id: PropTypes.number.isRequired,
    manufacturer: PropTypes.string.isRequired,
    family: PropTypes.string.isRequired,
    generation: PropTypes.string.isRequired,
    model: PropTypes.string.isRequired,
    speedMbPerS: PropTypes.number.isRequired,
    sizeMb: PropTypes.number.isRequired,
    energyConsumptionW: PropTypes.number.isRequired,
    failureModelId: PropTypes.number.isRequired,
    failureModel: Shapes.FailureModel,
});

Shapes.Machine = PropTypes.shape({
    id: PropTypes.number.isRequired,
    rackId: PropTypes.number.isRequired,
    position: PropTypes.number.isRequired,
    cpuIds: PropTypes.arrayOf(PropTypes.number.isRequired),
    cpus: PropTypes.arrayOf(Shapes.ProcessingUnit),
    gpuIds: PropTypes.arrayOf(PropTypes.number.isRequired),
    gpus: PropTypes.arrayOf(Shapes.ProcessingUnit),
    memoryIds: PropTypes.arrayOf(PropTypes.number.isRequired),
    memories: PropTypes.arrayOf(Shapes.StorageUnit),
    storageIds: PropTypes.arrayOf(PropTypes.number.isRequired),
    storages: PropTypes.arrayOf(Shapes.StorageUnit),
});

Shapes.Rack = PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    capacity: PropTypes.number.isRequired,
    powerCapacityW: PropTypes.number.isRequired,
    machines: PropTypes.arrayOf(Shapes.Machine),
});

Shapes.CoolingItem = PropTypes.shape({
    id: PropTypes.number.isRequired,
    energyConsumptionW: PropTypes.number.isRequired,
    type: PropTypes.string.isRequired,
    failureModelId: PropTypes.number.isRequired,
    failureModel: Shapes.FailureModel,
});

Shapes.PSU = PropTypes.shape({
    id: PropTypes.number.isRequired,
    energyKwh: PropTypes.number.isRequired,
    type: PropTypes.string.isRequired,
    failureModelId: PropTypes.number.isRequired,
    failureModel: Shapes.FailureModel,
});

Shapes.Tile = PropTypes.shape({
    id: PropTypes.number.isRequired,
    roomId: PropTypes.number.isRequired,
    positionX: PropTypes.number.isRequired,
    positionY: PropTypes.number.isRequired,
    objectId: PropTypes.number,
    objectType: PropTypes.string,
    rack: Shapes.Rack,
    coolingItem: Shapes.CoolingItem,
    psu: Shapes.PSU,
});

Shapes.Room = PropTypes.shape({
    id: PropTypes.number.isRequired,
    datacenterId: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    roomType: PropTypes.string.isRequired,
    tiles: PropTypes.arrayOf(Shapes.Tile),
});

Shapes.Datacenter = PropTypes.shape({
    id: PropTypes.number.isRequired,
    rooms: PropTypes.arrayOf(Shapes.Room),
});

Shapes.Section = PropTypes.shape({
    id: PropTypes.number.isRequired,
    pathId: PropTypes.number.isRequired,
    startTick: PropTypes.number.isRequired,
    datacenterId: PropTypes.number.isRequired,
    datacenter: Shapes.Datacenter,
});

Shapes.Path = PropTypes.shape({
    id: PropTypes.number.isRequired,
    simulationId: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    datetimeCreated: PropTypes.string.isRequired,
    sections: PropTypes.arrayOf(Shapes.Section),
});

Shapes.WallSegment = PropTypes.shape({
    startPosX: PropTypes.number.isRequired,
    startPosY: PropTypes.number.isRequired,
    isHorizontal: PropTypes.bool.isRequired,
    length: PropTypes.number.isRequired,
});

Shapes.InteractionLevel = PropTypes.shape({
    mode: PropTypes.string.isRequired,
    roomId: PropTypes.number,
    rackId: PropTypes.bool
});

export default Shapes;
