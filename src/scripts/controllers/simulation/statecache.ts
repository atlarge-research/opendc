import {SimulationController} from "../simulationcontroller";


export class StateCache {
    public static CACHE_INTERVAL = 3000;
    private static PREFERRED_CACHE_ADVANCE = 5;

    public stateList: {[key: number]: ITickState};
    public lastCachedTick: number;
    public cacheBlock: boolean;

    private simulationController: SimulationController;
    private intervalId: number;
    private caching: boolean;

    // Item caches
    private machineCache: {[keys: number]: IMachine};
    private rackCache: {[keys: number]: IRack};
    private roomCache: {[keys: number]: IRoom};
    private taskCache: {[keys: number]: ITask};


    constructor(simulationController: SimulationController) {
        this.stateList = {};
        this.lastCachedTick = -1;
        this.cacheBlock = true;
        this.simulationController = simulationController;
        this.caching = false;
    }

    public startCaching(): void {
        this.machineCache = {};
        this.rackCache = {};
        this.roomCache = {};
        this.taskCache = {};

        this.simulationController.mapView.currentDatacenter.rooms.forEach((room: IRoom) => {
            this.addRoomToCache(room);
        });
        this.simulationController.currentExperiment.trace.tasks.forEach((task: ITask) => {
            this.taskCache[task.id] = task;
        });

        this.caching = true;

        this.cache();
        this.intervalId = setInterval(() => {
            this.cache();
        }, StateCache.CACHE_INTERVAL);
    }

    private addRoomToCache(room: IRoom) {
        this.roomCache[room.id] = room;

        room.tiles.forEach((tile: ITile) => {
            if (tile.objectType === "RACK") {
                this.rackCache[tile.objectId] = <IRack>tile.object;

                (<IRack> tile.object).machines.forEach((machine: IMachine) => {
                    if (machine !== null) {
                        this.machineCache[machine.id] = machine;
                    }
                });
            }
        });
    }

    public stopCaching(): void {
        if (this.caching) {
            this.caching = false;
            clearInterval(this.intervalId);
        }
    }

    private cache(): void {
        let tick = this.lastCachedTick + 1;

        this.updateLastTick().then(() => {
            // Check if end of simulated region has been reached
            if (this.lastCachedTick > this.simulationController.lastSimulatedTick) {
                return;
            }

            this.fetchAllStatesOfTick(tick).then((data: ITickState) => {
                this.stateList[tick] = data;

                this.updateTasks(tick);

                // Update chart cache
                this.simulationController.chartController.tickUpdated(tick);

                this.lastCachedTick++;

                if (!this.cacheBlock && this.lastCachedTick - this.simulationController.currentTick <= 0) {
                    this.cacheBlock = true;
                    return;
                }

                if (this.cacheBlock) {
                    if (this.lastCachedTick - this.simulationController.currentTick >= StateCache.PREFERRED_CACHE_ADVANCE) {
                        this.cacheBlock = false;
                    }
                }
            });
        });
    }

    private updateTasks(tick: number): void {
        const taskIDsInTick = [];

        this.stateList[tick].taskStates.forEach((taskState: ITaskState) => {
            taskIDsInTick.push(taskState.taskId);
            if (this.stateList[tick - 1] !== undefined) {
                let previousFlops = 0;
                const previousStates = this.stateList[tick - 1].taskStates;

                for (let i = 0; i < previousStates.length; i++) {
                    if (previousStates[i].taskId === taskState.taskId) {
                        previousFlops = previousStates[i].flopsLeft;
                        break;
                    }
                }

                if (previousFlops > 0 && taskState.flopsLeft === 0) {
                    taskState.task.finishedTick = tick;
                }
            }
        });

        // Generate pseudo-task-states for tasks that haven't started yet or have already finished
        const traceTasks = this.simulationController.currentExperiment.trace.tasks;
        if (taskIDsInTick.length !== traceTasks.length) {
            traceTasks
                .filter((task: ITask) => {
                    return taskIDsInTick.indexOf(task.id) === -1;
                })
                .forEach((task: ITask) => {
                    const flopStateCount = task.startTick >= tick ? task.totalFlopCount : 0;

                    this.stateList[tick].taskStates.push({
                        id: -1,
                        taskId: task.id,
                        task: task,
                        experimentId: this.simulationController.currentExperiment.id,
                        tick,
                        flopsLeft: flopStateCount
                    });
                });
        }

        this.stateList[tick].taskStates.sort((a: ITaskState, b: ITaskState) => {
            return a.task.startTick - b.task.startTick;
        });
    }

    private updateLastTick(): Promise<void> {
        return this.simulationController.mapController.api.getLastSimulatedTickByExperiment(
            this.simulationController.simulation.id, this.simulationController.currentExperiment.id).then((data) => {
            this.simulationController.lastSimulatedTick = data;
        });
    }

    private fetchAllStatesOfTick(tick: number): Promise<ITickState> {
        let tickState: ITickState = {
            tick,
            machineStates: [],
            rackStates: [],
            roomStates: [],
            taskStates: []
        };
        const promises = [];

        promises.push(this.simulationController.mapController.api.getMachineStatesByTick(
            this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
            tick, this.machineCache
        ).then((states: IMachineState[]) => {
            tickState.machineStates = states;
        }));

        promises.push(this.simulationController.mapController.api.getRackStatesByTick(
            this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
            tick, this.rackCache
        ).then((states: IRackState[]) => {
            tickState.rackStates = states;
        }));

        promises.push(this.simulationController.mapController.api.getRoomStatesByTick(
            this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
            tick, this.roomCache
        ).then((states: IRoomState[]) => {
            tickState.roomStates = states;
        }));

        promises.push(this.simulationController.mapController.api.getTaskStatesByTick(
            this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
            tick, this.taskCache
        ).then((states: ITaskState[]) => {
            tickState.taskStates = states;
        }));

        return Promise.all(promises).then(() => {
            return tickState;
        });
    }
}
