/**
 * JSON Specification of the data model.
 *
 * Represents the data model after populating it (based on the DB ideas retrieved from the backend). Unpopulated
 * objects end with 'Stub'.
 */

// Webpack require declaration
//declare var require: {
//    <T>(path: string): T;
	//    (paths: string[], callback: (...modules: any[]) => void): void;
	//ensure: (paths: string[], callback: (require: <T>(path: string) => T) => void) => void;
//};

// Meta-constructs
interface IDateTime {
    year: number;
    month: number;
    day: number;
    hour: number;
    minute: number;
    second: number;
}

interface IGridPosition {
    x: number;
    y: number;
}

interface IBounds {
    min: number[];
    center: number[];
    max: number[];
}

interface IRoomNamePos {
    topLeft: IGridPosition;
    length: number;
}

interface IRoomWall {
    startPos: number[];
    horizontal: boolean;
    length: number;
}

interface TilePositionObject {
    position: IGridPosition;
    tileObject: createjs.Shape;
}

type IRoomTypeMap = { [key: string]: string[]; };

interface ITickState {
    tick: number;
    roomStates: IRoomState[];
    rackStates: IRackState[];
    machineStates: IMachineState[];
    taskStates: ITaskState[];
}

// Communication
interface IRequest {
    id?: number;
    path: string;
    method: string;
    parameters: {
        body: any;
        path: any;
        query: any;
    };
    token?: string;
}

interface IResponse {
    id?: number;
    status: {
        code: number;
        description?: string;
    };
    content: any;
}

// Simulation
interface ISimulation {
    id: number;
    name: string;
    paths?: IPath[];
    experiments?: IExperiment[];
    datetimeCreated: string;
    datetimeCreatedParsed?: IDateTime;
    datetimeLastEdited: string;
    datetimeLastEditedParsed?: IDateTime;
}

interface ISection {
    id: number;
    startTick: number;
    simulationId: number;
    datacenterId: number;
    datacenter?: IDatacenter;
}

interface IPath {
    id: number;
    simulationId: number;
    sections?: ISection[];
    name: string;
    datetimeCreated: string;
}

interface ITrace {
    id: number;
    name: string;
    tasks?: ITask[];
}

interface IScheduler {
    name: string;
}

interface ITask {
    id: number;
    traceId: number;
    queueEntryTick: number;
    startTick?: number;
    finishedTick?: number;
    totalFlopCount: number;
}

interface IExperiment {
    id: number;
    simulationId: number;
    pathId: number;
    traceId: number;
    trace?: ITrace;
    schedulerName: string;
    name: string;
}

// Authorization
interface IAuthorization {
    userId: number;
    user?: IUser;
    simulationId: number;
    simulation?: ISimulation;
    authorizationLevel: string;
}

interface IUser {
    id: number;
    googleId: number;
    email: string;
    givenName: string;
    familyName: string;
}

// DC Layout
interface IDatacenter {
    id: number;
    rooms?: IRoom[];
}

interface IRoom {
    id: number;
    datacenterId: number;
    name: string;
    roomType: string;
    tiles?: ITile[];
}

interface ITile {
    id: number;
    roomId: number;
    objectId?: number;
    objectType?: string;
    object?: IDCObject;
    position: IGridPosition;
}

// State
interface IMachineState {
    id: number;
    machineId: number;
    machine?: IMachine;
    experimentId: number;
    tick: number;
    temperatureC: number;
    inUseMemoryMb: number;
    loadFraction: number;
}

interface IRackState {
    id: number;
    rackId: number;
    rack?: IRack;
    experimentId: number;
    tick: number;
    temperatureC: number;
    inUseMemoryMb: number;
    loadFraction: number;
}

interface IRoomState {
    id: number;
    roomId: number;
    room?: IRoom;
    experimentId: number;
    tick: number;
    temperatureC: number;
    inUseMemoryMb: number;
    loadFraction: number;
}

interface ITaskState {
    id: number;
    taskId: number;
    task?: ITask;
    experimentId: number;
    tick: number;
    flopsLeft: number;
}

// Generalization of a datacenter object
type IDCObject = IRack | ICoolingItem | IPSU;

interface IRack {
    id: number;
    objectType?: string;
    name: string;
    capacity: number;
    powerCapacityW: number;
    machines?: IMachine[];
}

interface ICoolingItem {
    id: number;
    objectType?: string;
    energyConsumptionW: number;
    type: string;
    failureModelId: number;
    failureModel?: IFailureModel;
}

interface IPSU {
    id: number;
    objectType?: string;
    energyKwh: number;
    type: string;
    failureModelId: number;
    failureModel?: IFailureModel;
}

// Machine
interface IMachine {
    id: number;
    rackId: number;
    position: number;
    tags: string[];
    cpuIds: number[];
    cpus?: ICPU[];
    gpuIds: number[];
    gpus?: IGPU[];
    memoryIds: number[];
    memories?: IMemory[];
    storageIds: number[];
    storages?: IPermanentStorage[];
}

interface IProcessingUnit {
    id: number;
    manufacturer: string;
    family: string;
    generation: string;
    model: string;
    clockRateMhz: number;
    numberOfCores: number;
    energyConsumptionW: number;
    failureModelId: number;
    failureModel?: IFailureModel;
}

interface ICPU extends IProcessingUnit {

}

interface IGPU extends IProcessingUnit {

}

interface IStorageUnit {
    id: number;
    manufacturer: string;
    family: string;
    generation: string;
    model: string;
    speedMbPerS: number;
    sizeMb: number;
    energyConsumptionW: number;
    failureModelId: number;
    failureModel?: IFailureModel;
}

interface IMemory extends IStorageUnit {

}

interface IPermanentStorage extends IStorageUnit {

}

type INodeUnit = IProcessingUnit & IStorageUnit;

interface IFailureModel {
    id: number;
    name: string;
    rate: number;
}
