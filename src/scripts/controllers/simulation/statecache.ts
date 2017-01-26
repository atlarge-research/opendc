import {SimulationController} from "../simulationcontroller";


export class StateCache {
    public static CACHE_INTERVAL = 10000;
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
        this.lastCachedTick = 0;
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

            this.fetchAllAvailableStates().then((data) => {
                this.stateList = data;

                this.updateTasks(tick);

                // Determine last cached tick
                let ticks = Object.keys(this.stateList).sort((a, b) => {
                    return parseInt(a) - parseInt(b);
                });
                if (ticks.length > 0) {
                    this.lastCachedTick = parseInt(ticks[ticks.length - 1]);
                }

                // Update chart cache
                this.simulationController.chartController.tickUpdated(tick);

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

    private fetchAllAvailableStates(): Promise<{[key: number]: ITickState}> {
        let machineStates, rackStates, roomStates, taskStates;
        const promises = [];

        promises.push(
            this.simulationController.mapController.api.getMachineStates(
                this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
                this.machineCache
            ).then((states: IMachineState[]) => {
                machineStates = states;
            })
        );

        promises.push(
            this.simulationController.mapController.api.getRackStates(
                this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
                this.rackCache
            ).then((states: IRackState[]) => {
                rackStates = states;
            })
        );

        promises.push(
            this.simulationController.mapController.api.getRoomStates(
                this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
                this.roomCache
            ).then((states: IRoomState[]) => {
                roomStates = states;
            })
        );

        promises.push(
            this.simulationController.mapController.api.getTaskStates(
                this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
                this.taskCache
            ).then((states: ITaskState[]) => {
                taskStates = states;
            })
        );

        return Promise.all(promises).then(() => {
            let tickStates: {[key: number]: ITickState} = {};

            machineStates.forEach((machineState: IMachineState) => {
                if (tickStates[machineState.tick] === undefined) {
                    tickStates[machineState.tick] = {
                        tick: machineState.tick,
                        machineStates: [machineState],
                        rackStates: [],
                        roomStates: [],
                        taskStates: []
                    };
                } else {
                    tickStates[machineState.tick].machineStates.push(machineState);
                }
            });
            rackStates.forEach((rackState: IRackState) => {
                if (tickStates[rackState.tick] === undefined) {
                    tickStates[rackState.tick] = {
                        tick: rackState.tick,
                        machineStates: [],
                        rackStates: [rackState],
                        roomStates: [],
                        taskStates: []
                    };
                } else {
                    tickStates[rackState.tick].rackStates.push(rackState);
                }
            });
            roomStates.forEach((roomState: IRoomState) => {
                if (tickStates[roomState.tick] === undefined) {
                    tickStates[roomState.tick] = {
                        tick: roomState.tick,
                        machineStates: [],
                        rackStates: [],
                        roomStates: [roomState],
                        taskStates: []
                    };
                } else {
                    tickStates[roomState.tick].roomStates.push(roomState);
                }
            });
            taskStates.forEach((taskState: ITaskState) => {
                if (tickStates[taskState.tick] === undefined) {
                    tickStates[taskState.tick] = {
                        tick: taskState.tick,
                        machineStates: [],
                        rackStates: [],
                        roomStates: [],
                        taskStates: [taskState]
                    };
                } else {
                    tickStates[taskState.tick].taskStates.push(taskState);
                }
            });

            return tickStates;
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

        promises.push(
            this.simulationController.mapController.api.getMachineStates(
                this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
                this.machineCache, tick
            ).then((states: IMachineState[]) => {
                tickState.machineStates = states;
            })
        );

        promises.push(
            this.simulationController.mapController.api.getRackStates(
                this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
                this.rackCache, tick
            ).then((states: IRackState[]) => {
                tickState.rackStates = states;
            })
        );

        promises.push(
            this.simulationController.mapController.api.getRoomStates(
                this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
                this.roomCache, tick
            ).then((states: IRoomState[]) => {
                tickState.roomStates = states;
            })
        );

        promises.push(
            this.simulationController.mapController.api.getTaskStates(
                this.simulationController.mapView.simulation.id, this.simulationController.currentExperiment.id,
                this.taskCache, tick
            ).then((states: ITaskState[]) => {
                tickState.taskStates = states;
            })
        );

        return Promise.all(promises).then(() => {
            return tickState;
        });
    }
}
