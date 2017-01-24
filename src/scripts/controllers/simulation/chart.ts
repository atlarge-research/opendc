import * as c3 from "c3";
import {InteractionLevel, MapController} from "../mapcontroller";
import {ColorRepresentation, SimulationController} from "../simulationcontroller";
import {Util} from "../../util";


export interface IStateColumn {
    loadFractions: string[] | number[];
    inUseMemoryMb: string[] | number[];
    temperatureC: string[] | number[];
}


export class ChartController {
    public roomSeries: { [key: number]: IStateColumn };
    public rackSeries: { [key: number]: IStateColumn };
    public machineSeries: { [key: number]: IStateColumn };
    public chart: c3.ChartAPI;
    public machineChart: c3.ChartAPI;

    private simulationController: SimulationController;
    private mapController: MapController;
    private chartData: (string | number)[][];
    private xSeries: (string | number)[];
    private names: { [key: string]: string };


    constructor(simulationController: SimulationController) {
        this.simulationController = simulationController;
        this.mapController = simulationController.mapController;
    }

    public setup(): void {
        this.names = {};

        this.roomSeries = {};
        this.rackSeries = {};
        this.machineSeries = {};

        this.simulationController.sections.forEach((simulationSection: ISection) => {
            simulationSection.datacenter.rooms.forEach((room: IRoom) => {
                if (room.roomType === "SERVER" && this.roomSeries[room.id] === undefined) {
                    this.names["ro" + room.id] = (room.name === "" || room.name === undefined) ?
                        "Unnamed room" : room.name;

                    this.roomSeries[room.id] = {
                        loadFractions: ["ro" + room.id],
                        inUseMemoryMb: ["ro" + room.id],
                        temperatureC: ["ro" + room.id]
                    };
                }

                room.tiles.forEach((tile: ITile) => {
                    if (tile.object !== undefined && tile.objectType === "RACK" && this.rackSeries[tile.objectId] === undefined) {
                        let objectName = (<IRack>tile.object).name;
                        this.names["ra" + tile.objectId] = objectName === "" || objectName === undefined ?
                            "Unnamed rack" : objectName;

                        this.rackSeries[tile.objectId] = {
                            loadFractions: ["ra" + tile.objectId],
                            inUseMemoryMb: ["ra" + tile.objectId],
                            temperatureC: ["ra" + tile.objectId]
                        };

                        (<IRack>tile.object).machines.forEach((machine: IMachine) => {
                            if (machine === null || this.machineSeries[machine.id] !== undefined) {
                                return;
                            }

                            this.names["ma" + machine.id] = "Machine at position " + (machine.position + 1).toString();

                            this.machineSeries[machine.id] = {
                                loadFractions: ["ma" + machine.id],
                                inUseMemoryMb: ["ma" + machine.id],
                                temperatureC: ["ma" + machine.id]
                            };
                        });
                    }
                });
            });
        });


        this.xSeries = ["time"];
        this.chartData = [this.xSeries];

        this.chart = this.chartSetup("#statistics-chart");
        this.machineChart = this.chartSetup("#machine-statistics-chart");
    }

    public chartSetup(chartId: string): c3.ChartAPI {
        return c3.generate({
            bindto: chartId,
            data: {
                xFormat: '%S',
                x: "time",
                columns: this.chartData,
                names: this.names
            },
            axis: {
                x: {
                    type: "timeseries",
                    tick: {
                        format: function (time: Date) {
                            let formattedTime = time.getSeconds() + "s";

                            if (time.getMinutes() > 0) {
                                formattedTime = time.getMinutes() + "m" + formattedTime;
                            }
                            if (time.getHours() > 0) {
                                formattedTime = time.getHours() + "h" + formattedTime;
                            }

                            return formattedTime;
                        },
                        culling: {
                            max: 5
                        },
                        count: 8
                    },
                    padding: {
                        left: 0,
                        right: 10
                    }
                },
                y: {
                    min: 0,
                    max: 1,
                    padding: {
                        top: 0,
                        bottom: 0
                    },
                    tick: {
                        format: function (d) {
                            return (Math.round(d * 100) / 100).toString();
                        }
                    }
                }
            }
        });
    }

    public update(): void {
        this.xSeries = (<(number|string)[]>["time"]).concat(Util.range(this.simulationController.currentTick));

        this.chartData = [this.xSeries];

        let prefix = "";
        let machineId = -1;
        if (this.mapController.interactionLevel === InteractionLevel.BUILDING) {
            for (let roomId in this.roomSeries) {
                if (this.roomSeries.hasOwnProperty(roomId)) {
                    if (this.simulationController.colorRepresentation === ColorRepresentation.LOAD) {
                        this.chartData.push(this.roomSeries[roomId].loadFractions);
                    }
                }
            }
            prefix = "ro";
        } else if (this.mapController.interactionLevel === InteractionLevel.ROOM) {
            for (let rackId in this.rackSeries) {
                if (this.rackSeries.hasOwnProperty(rackId) &&
                    this.simulationController.rackToRoomMap[rackId] ===
                    this.mapController.roomModeController.currentRoom.id) {
                    if (this.simulationController.colorRepresentation === ColorRepresentation.LOAD) {
                        this.chartData.push(this.rackSeries[rackId].loadFractions);
                    }
                }
            }
            prefix = "ra";
        } else if (this.mapController.interactionLevel === InteractionLevel.NODE) {
            if (this.simulationController.colorRepresentation === ColorRepresentation.LOAD) {
                this.chartData.push(
                    this.machineSeries[this.mapController.nodeModeController.currentMachine.id].loadFractions
                );
            }
            prefix = "ma";
            machineId = this.mapController.nodeModeController.currentMachine.id;
        }

        let unloads: string[] = [];
        for (let id in this.names) {
            if (this.names.hasOwnProperty(id)) {
                if (machineId === -1) {
                    if (id.substr(0, 2) !== prefix ||
                        (this.mapController.interactionLevel === InteractionLevel.ROOM &&
                        this.simulationController.rackToRoomMap[parseInt(id.substr(2))] !==
                        this.mapController.roomModeController.currentRoom.id)) {
                        unloads.push(id);
                    }
                }
                else {
                    if (id !== prefix + machineId) {
                        unloads.push(id);
                    }
                }
            }
        }

        let targetChart: c3.ChartAPI;
        if (this.mapController.interactionLevel === InteractionLevel.NODE) {
            targetChart = this.machineChart;
        } else {
            targetChart = this.chart;
        }

        targetChart.load({
            columns: this.chartData,
            unload: unloads
        });

    }

    public tickUpdated(tick: number): void {
        let roomStates: IRoomState[] = this.simulationController.stateCache.stateList[tick].roomStates;
        roomStates.forEach((roomState: IRoomState) => {
            ChartController.insertAtIndex(this.roomSeries[roomState.roomId].loadFractions, tick + 1, roomState.loadFraction);
        });

        let rackStates: IRackState[] = this.simulationController.stateCache.stateList[tick].rackStates;
        rackStates.forEach((rackState: IRackState) => {
            ChartController.insertAtIndex(this.rackSeries[rackState.rackId].loadFractions, tick + 1, rackState.loadFraction);
        });

        let machineStates: IMachineState[] = this.simulationController.stateCache.stateList[tick].machineStates;
        machineStates.forEach((machineState: IMachineState) => {
            ChartController.insertAtIndex(this.machineSeries[machineState.machineId].loadFractions, tick + 1, machineState.loadFraction);
        });
    }

    private static insertAtIndex(list: any[], index: number, data: any): void {
        if (index > list.length) {
            let i = list.length;
            while (i < index) {
                list[i] = null;
                i++;
            }
        }

        list[index] = data;
    }
}