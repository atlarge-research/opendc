///<reference path="../../definitions.ts" />
///<reference path="../../../../typings/index.d.ts" />
import {Util} from "../../util";
import {ServerConnection} from "../../serverconnection";


export class APIController {
    constructor(onConnect: (api: APIController) => any) {
        ServerConnection.connect(() => {
            onConnect(this);
        });
    }


    ///
    // PATH: /users
    ///

    // METHOD: GET
    public getUserByEmail(email: string): Promise<IUser> {
        return ServerConnection.send({
            path: "/users",
            method: "GET",
            parameters: {
                body: {},
                path: {},
                query: {
                    email
                }
            }
        });
    }

    // METHOD: POST
    public addUser(user: IUser): Promise<IUser> {
        return ServerConnection.send({
            path: "/users",
            method: "POST",
            parameters: {
                body: {
                    user: user
                },
                path: {},
                query: {}
            }
        });
    }

    ///
    // PATH: /users/{id}
    ///

    // METHOD: GET
    public getUser(userId: number): Promise<IUser> {
        return ServerConnection.send({
            path: "/users/{userId}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    userId
                },
                query: {}
            }
        });
    }

    // METHOD: PUT
    public updateUser(userId: number, user: IUser): Promise<IUser> {
        return ServerConnection.send({
            path: "/users/{userId}",
            method: "PUT",
            parameters: {
                body: {
                    user: {
                        givenName: user.givenName,
                        familyName: user.familyName
                    }
                },
                path: {
                    userId
                },
                query: {}
            }
        });
    }

    // METHOD: DELETE
    public deleteUser(userId: number): Promise<IUser> {
        return ServerConnection.send({
            path: "/users/{userId}",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    userId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /users/{userId}/authorizations
    ///

    // METHOD: GET
    public getAuthorizationsByUser(userId: number): Promise<IAuthorization[]> {
        let authorizations = [];
        return ServerConnection.send({
            path: "/users/{userId}/authorizations",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    userId
                },
                query: {}
            }
        }).then((data: any) => {
            authorizations = data;
            return this.getUser(userId);
        }).then((userData: any) => {
            let promises = [];
            authorizations.forEach((authorization: IAuthorization) => {
                authorization.user = userData;
                promises.push(this.getSimulation(authorization.simulationId).then((simulationData: any) => {
                    authorization.simulation = simulationData;
                }));
            });
            return Promise.all(promises);
        }).then((data: any) => {
            return authorizations;
        });
    }

    ///
    // PATH: /simulations
    ///

    // METHOD: POST
    public addSimulation(simulation: ISimulation): Promise<ISimulation> {
        return ServerConnection.send({
            path: "/simulations",
            method: "POST",
            parameters: {
                body: {
                    simulation: Util.packageForSending(simulation)
                },
                path: {},
                query: {}
            }
        }).then((data: any) => {
            this.parseSimulationTimestamps(data);
            return data;
        });
    }

    ///
    // PATH: /simulations/{simulationId}
    ///

    // METHOD: GET
    public getSimulation(simulationId: number): Promise<ISimulation> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId
                },
                query: {}
            }
        }).then((data: any) => {
            this.parseSimulationTimestamps(data);
            return data;
        });
    }

    // METHOD: PUT
    public updateSimulation(simulation: ISimulation): Promise<ISimulation> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}",
            method: "PUT",
            parameters: {
                body: {
                    simulation: Util.packageForSending(simulation)
                },
                path: {
                    simulationId: simulation.id
                },
                query: {}
            }
        }).then((data: any) => {
            this.parseSimulationTimestamps(data);
            return data;
        });
    }

    // METHOD: DELETE
    public deleteSimulation(simulationId: number): Promise<ISimulation> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    simulationId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/authorizations
    ///

    // METHOD: GET
    public getAuthorizationsBySimulation(simulationId: number): Promise<IAuthorization[]> {
        let authorizations = [];
        return ServerConnection.send({
            path: "/simulations/{simulationId}/authorizations",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId
                },
                query: {}
            }
        }).then((data: any) => {
            authorizations = data;
            return this.getSimulation(simulationId);
        }).then((simulationData: any) => {
            let promises = [];
            authorizations.forEach((authorization: IAuthorization) => {
                authorization.simulation = simulationData;
                promises.push(this.getUser(authorization.userId).then((userData: any) => {
                    authorization.user = userData;
                }));
            });
            return Promise.all(promises);
        }).then((data: any) => {
            return authorizations;
        });
    }

    ///
    // PATH: /simulations/{simulationId}/authorizations/{userId}
    ///

    // METHOD: GET
    // Not needed

    // METHOD: POST
    public addAuthorization(authorization: IAuthorization): Promise<IAuthorization> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/authorizations/{userId}",
            method: "POST",
            parameters: {
                body: {
                    authorization: {
                        authorizationLevel: authorization.authorizationLevel
                    }
                },
                path: {
                    simulationId: authorization.simulationId,
                    userId: authorization.userId
                },
                query: {}
            }
        });
    }

    // METHOD: PUT
    public updateAuthorization(authorization: IAuthorization): Promise<IAuthorization> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/authorizations/{userId}",
            method: "PUT",
            parameters: {
                body: {
                    authorization: {
                        authorizationLevel: authorization.authorizationLevel
                    }
                },
                path: {
                    simulationId: authorization.simulationId,
                    userId: authorization.userId
                },
                query: {}
            }
        });
    }

    // METHOD: DELETE
    public deleteAuthorization(authorization: IAuthorization): Promise<IAuthorization> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/authorizations/{userId}",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    simulationId: authorization.simulationId,
                    userId: authorization.userId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}
    ///

    // METHOD: GET
    public getDatacenter(simulationId: number, datacenterId: number): Promise<IDatacenter> {
        let datacenter;

        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId
                },
                query: {}
            }
        }).then((data: any) => {
            datacenter = data;

            return this.getRoomsByDatacenter(simulationId, datacenterId);
        }).then((data: any) => {
            datacenter.rooms = data;
            return datacenter;
        });
    }


    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}/rooms
    ///

    // METHOD: GET
    public getRoomsByDatacenter(simulationId: number, datacenterId: number): Promise<IRoom[]> {
        let rooms;

        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId
                },
                query: {}
            }
        }).then((data: any) => {
            rooms = data;

            let promises = [];
            rooms.forEach((room: IRoom) => {
                promises.push(this.loadRoomTiles(simulationId, datacenterId, room));
            });
            return Promise.all(promises).then((data: any) => {
                return rooms;
            });
        });
    }

    // METHOD: POST
    public addRoomToDatacenter(simulationId: number, datacenterId: number): Promise<IRoom> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms",
            method: "POST",
            parameters: {
                body: {
                    room: {
                        id: -1,
                        datacenterId,
                        roomType: "SERVER"
                    }
                },
                path: {
                    simulationId,
                    datacenterId
                },
                query: {}
            }
        }).then((data: any) => {
            data.tiles = [];
            return data;
        });
    }

    ///
    // PATH: /room-types
    ///

    // METHOD: GET
    public getAllRoomTypes(): Promise<string[]> {
        return ServerConnection.send({
            path: "/room-types",
            method: "GET",
            parameters: {
                body: {},
                path: {},
                query: {}
            }
        }).then((data: any) => {
            let result = [];
            data.forEach((roomType: any) => {
                result.push(roomType.name);
            });
            return result;
        });
    }

    ///
    // PATH: /room-types/{name}/allowed-objects
    ///

    // METHOD: GET
    public getAllowedObjectsByRoomType(name: string): Promise<string[]> {
        return ServerConnection.send({
            path: "/room-types/{name}/allowed-objects",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    name
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}
    ///

    // METHOD: GET
    public getRoom(simulationId: number, datacenterId: number, roomId: number): Promise<IRoom> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.loadRoomTiles(simulationId, datacenterId, data);
        });
    }

    // METHOD: PUT
    public updateRoom(simulationId: number, datacenterId: number, room: IRoom): Promise<IRoom> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}",
            method: "PUT",
            parameters: {
                body: {
                    room: Util.packageForSending(room)
                },
                path: {
                    simulationId,
                    datacenterId,
                    roomId: room.id
                },
                query: {}
            }
        }).then((data: any) => {
            return this.loadRoomTiles(simulationId, datacenterId, data);
        });
    }

    // METHOD: DELETE
    public deleteRoom(simulationId: number, datacenterId: number, roomId: number): Promise<IRoom> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles
    ///

    // METHOD: GET
    public getTilesByRoom(simulationId: number, datacenterId: number, roomId: number): Promise<ITile[]> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId
                },
                query: {}
            }
        }).then((data: any) => {
            let promises = data.map((item) => {
                return this.loadTileObject(simulationId, datacenterId, roomId, item);
            });

            return Promise.all(promises).then(() => {
                return data;
            })
        });
    }

    // METHOD: POST
    public addTileToRoom(simulationId: number, datacenterId: number, roomId: number, tile: ITile): Promise<ITile> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles",
            method: "POST",
            parameters: {
                body: {
                    tile: Util.packageForSending(tile)
                },
                path: {
                    simulationId,
                    datacenterId,
                    roomId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.loadTileObject(simulationId, datacenterId, roomId, data);
        });
    }

    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}
    ///

    // METHOD: GET
    // Not needed (yet)

    // METHOD: DELETE
    public deleteTile(simulationId: number, datacenterId: number, roomId: number, tileId: number): Promise<ITile> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/cooling-item
    ///

    // METHOD: GET
    public getCoolingItem(simulationId: number, datacenterId: number, roomId: number,
                          tileId: number): Promise<ICoolingItem> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/cooling-item",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.loadFailureModel(data);
        });
    }

    // METHOD: POST
    public addCoolingItem(simulationId: number, datacenterId: number, roomId: number, tileId: number,
                          coolingItem: ICoolingItem): Promise<ICoolingItem> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/cooling-item",
            method: "POST",
            parameters: {
                body: {
                    coolingItem: Util.packageForSending(coolingItem)
                },
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.loadFailureModel(data);
        });
    }

    // METHOD: PUT
    // Not needed (yet)

    // METHOD: DELETE
    public deleteCoolingItem(simulationId: number, datacenterId: number, roomId: number,
                             tileId: number): Promise<ICoolingItem> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/cooling-item",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/psu
    ///

    // METHOD: GET
    public getPSU(simulationId: number, datacenterId: number, roomId: number, tileId: number): Promise<IPSU> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/psu",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.loadFailureModel(data);
        });
    }

    // METHOD: POST
    public addPSU(simulationId: number, datacenterId: number, roomId: number, tileId: number,
                  psu: IPSU): Promise<IPSU> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/psu",
            method: "POST",
            parameters: {
                body: {
                    psu: Util.packageForSending(psu)
                },
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.loadFailureModel(data);
        });
    }

    // METHOD: PUT
    // Not needed (yet)

    // METHOD: DELETE
    public deletePSU(simulationId: number, datacenterId: number, roomId: number,
                     tileId: number): Promise<IPSU> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/psu",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack
    ///

    // METHOD: GET
    public getRack(simulationId: number, datacenterId: number, roomId: number,
                   tileId: number): Promise<IRack> {
        let rack = {};

        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        }).then((data: any) => {
            rack = data;
            return this.getMachinesByRack(simulationId, datacenterId, roomId, tileId);
        }).then((machines: any) => {
            let promises = machines.map((machine) => {
                return this.loadMachineUnits(machine);
            });


            return Promise.all(promises).then(() => {
                rack["machines"] = [];

                machines.forEach((machine: IMachine) => {
                    rack["machines"][machine.position] = machine;
                });

                for (let i = 0; i < rack["capacity"]; i++) {
                    if (rack["machines"][i] === undefined) {
                        rack["machines"][i] = null;
                    }
                }

                return rack;
            });
        });
    }

    // METHOD: POST
    public addRack(simulationId: number, datacenterId: number, roomId: number,
                   tileId: number, rack: IRack): Promise<IRack> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack",
            method: "POST",
            parameters: {
                body: {
                    rack: Util.packageForSending(rack)
                },
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        }).then((data: any) => {
            data.machines = [];

            for (let i = 0; i < data.capacity; i++) {
                data.machines.push(null);
            }

            return data;
        });
    }

    // METHOD: PUT
    public updateRack(simulationId: number, datacenterId: number, roomId: number,
                      tileId: number, rack: IRack): Promise<IRack> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack",
            method: "PUT",
            parameters: {
                body: {
                    rack
                },
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        }).then((data: any) => {
            data.machines = rack.machines;

            return data;
        });
    }

    // METHOD: DELETE
    public deleteRack(simulationId: number, datacenterId: number, roomId: number,
                      tileId: number): Promise<IRack> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack/machines
    ///

    // METHOD: GET
    public getMachinesByRack(simulationId: number, datacenterId: number, roomId: number,
                             tileId: number): Promise<IMachine[]> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack/machines",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        }).then((data: any) => {
            let promises = data.map((machine) => {
                return this.loadMachineUnits(machine);
            });

            return Promise.all(promises).then(() => {
                return data;
            });
        });
    }

    // METHOD: POST
    public addMachineToRack(simulationId: number, datacenterId: number, roomId: number,
                            tileId: number, machine: IMachine): Promise<IMachine> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack/machines",
            method: "POST",
            parameters: {
                body: {
                    machine: Util.packageForSending(machine)
                },
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.loadMachineUnits(data);
        });
    }

    ///
    // PATH: /simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack/machines/{position}
    ///

    // METHOD: GET
    // Not needed (yet)

    // METHOD: PUT
    public updateMachine(simulationId: number, datacenterId: number, roomId: number,
                         tileId: number, machine: IMachine): Promise<IMachine> {
        machine["tags"] = [];
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack/machines/{position}",
            method: "PUT",
            parameters: {
                body: {
                    machine: Util.packageForSending(machine)
                },
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId,
                    position: machine.position
                },
                query: {}
            }
        }).then((data: any) => {
            return this.loadMachineUnits(data);
        });
    }

    // METHOD: DELETE
    public deleteMachine(simulationId: number, datacenterId: number, roomId: number,
                         tileId: number, position: number): Promise<any> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/datacenters/{datacenterId}/rooms/{roomId}/tiles/{tileId}/rack/machines/{position}",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    datacenterId,
                    roomId,
                    tileId,
                    position
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/experiments
    ///

    // METHOD: GET
    public getExperimentsBySimulation(simulationId: number): Promise<IExperiment[]> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/experiments",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId
                },
                query: {}
            }
        }).then((data: any) => {
            let promises = data.map((item: any) => {
                return this.getTrace(item.traceId).then((traceData: any) => {
                    item.trace = traceData;
                });
            });
            return Promise.all(promises).then(() => {
                return data;
            });
        });
    }

    // METHOD: POST
    public addExperimentToSimulation(simulationId: number, experiment: IExperiment): Promise<IExperiment> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/experiments",
            method: "POST",
            parameters: {
                body: {
                    experiment: Util.packageForSending(experiment)
                },
                path: {
                    simulationId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.getTrace(data.traceId).then((traceData: any) => {
                data.trace = traceData;

                return data;
            });
        });
    }

    ///
    // PATH: /simulations/{simulationId}/experiments/{experimentId}
    ///

    // METHOD: GET
    // Not needed (yet)

    // METHOD: PUT
    public updateExperiment(experiment: IExperiment): Promise<IExperiment> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/experiments/{experimentId}",
            method: "PUT",
            parameters: {
                body: {
                    experiment: Util.packageForSending(experiment)
                },
                path: {
                    experimentId: experiment.id,
                    simulationId: experiment.simulationId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.getTrace(data.traceId).then((traceData: any) => {
                data.trace = traceData;

                return data;
            });
        });
    }

    // METHOD: DELETE
    public deleteExperiment(simulationId: number, experimentId: number): Promise<any> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/experiments/{experimentId}",
            method: "DELETE",
            parameters: {
                body: {},
                path: {
                    experimentId,
                    simulationId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /simulations/{simulationId}/experiments/{experimentId}/last-simulated-tick
    ///

    // METHOD: GET
    public getLastSimulatedTickByExperiment(simulationId: number, experimentId: number): Promise<number> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/experiments/{experimentId}/last-simulated-tick",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    experimentId
                },
                query: {}
            }
        }).then((data: any) => {
            return data.lastSimulatedTick;
        });
    }

    ///
    // PATH: /simulations/{simulationId}/experiments/{experimentId}/machine-states
    ///

    // METHOD: GET
    public getMachineStates(simulationId: number, experimentId: number, machines: {[keys: number]: IMachine},
                            tick?: number): Promise<IMachineState[]> {
        let query;
        if (tick !== undefined) {
            query = {tick};
        } else {
            query = {};
        }

        return ServerConnection.send({
            path: "/simulations/{simulationId}/experiments/{experimentId}/machine-states",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    experimentId
                },
                query
            }
        }).then((data: any) => {
            data.forEach((item: any) => {
                item.machine = machines[item.machineId];
            });

            return data;
        });
    }

    ///
    // PATH: /simulations/{simulationId}/experiments/{experimentId}/rack-states
    ///

    // METHOD: GET
    public getRackStates(simulationId: number, experimentId: number, racks: {[keys: number]: IRack},
                         tick?: number): Promise<IRackState[]> {
        let query;
        if (tick !== undefined) {
            query = {tick};
        } else {
            query = {};
        }

        return ServerConnection.send({
            path: "/simulations/{simulationId}/experiments/{experimentId}/rack-states",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    experimentId
                },
                query: query
            }
        }).then((data: any) => {
            let promises = data.map((item: any) => {
                item.rack = racks[item.rackId];
            });

            return Promise.all(promises).then(() => {
                return data;
            });
        });
    }

    ///
    // PATH: /simulations/{simulationId}/experiments/{experimentId}/room-states
    ///

    // METHOD: GET
    public getRoomStates(simulationId: number, experimentId: number, rooms: {[keys: number]: IRoom},
                         tick?: number): Promise<IRoomState[]> {
        let query;
        if (tick !== undefined) {
            query = {tick};
        } else {
            query = {};
        }

        return ServerConnection.send({
            path: "/simulations/{simulationId}/experiments/{experimentId}/room-states",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    experimentId
                },
                query
            }
        }).then((data: any) => {
            let promises = data.map((item: any) => {
                item.room = rooms[item.roomId];
            });

            return Promise.all(promises).then(() => {
                return data;
            });
        });
    }

    ///
    // PATH: /simulations/{simulationId}/experiments/{experimentId}/task-states
    ///

    // METHOD: GET
    public getTaskStates(simulationId: number, experimentId: number, tasks: {[keys: number]: ITask},
                         tick?: number): Promise<ITaskState[]> {
        let query;
        if (tick === undefined) {
            query = {tick};
        } else {
            query = {};
        }

        return ServerConnection.send({
            path: "/simulations/{simulationId}/experiments/{experimentId}/task-states",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    experimentId
                },
                query
            }
        }).then((data: any) => {
            let promises = data.map((item: any) => {
                item.task = tasks[item.taskId];
            });

            return Promise.all(promises).then(() => {
                return data;
            });
        });
    }

    ///
    // PATH: /simulations/{simulationId}/paths
    ///

    // METHOD: GET
    public getPathsBySimulation(simulationId: number): Promise<IPath[]> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/paths",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId
                },
                query: {}
            }
        }).then((data: any) => {
            let promises = data.map((item: any) => {
                return this.getSectionsByPath(simulationId, item.id).then((sectionsData: any) => {
                    item.sections = sectionsData;
                });
            });
            return Promise.all(promises).then(() => {
                return data;
            });
        });
    }

    ///
    // PATH: /simulations/{simulationId}/paths/{pathId}
    ///

    // METHOD: GET
    public getPath(simulationId: number, pathId: number): Promise<IPath> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/paths/{pathId}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    pathId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.getSectionsByPath(simulationId, pathId).then((sectionsData: any) => {
                data.sections = sectionsData;
                return data;
            });
        });
    }

    ///
    // PATH: /simulations/{simulationId}/paths/{pathId}/branches
    ///

    // METHOD: GET
    public getBranchesByPath(simulationId: number, pathId: number): Promise<IPath[]> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/paths/{pathId}/branches",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    pathId
                },
                query: {}
            }
        }).then((data: any) => {
            let promises = data.map((item: any) => {
                return this.getSectionsByPath(simulationId, item.id).then((sectionsData: any) => {
                    item.sections = sectionsData;
                });
            });
            return Promise.all(promises).then(() => {
                return data;
            });
        });
    }

    // METHOD: POST
    public branchFromPath(simulationId: number, pathId: number, startTick: number): Promise<IPath> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/paths/{pathId}/branches",
            method: "POST",
            parameters: {
                body: {
                    section: {
                        startTick
                    }
                },
                path: {
                    simulationId,
                    pathId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.getSectionsByPath(simulationId, data.id).then((sectionsData: any) => {
                data.sections = sectionsData;
                return data;
            });
        });
    }

    ///
    // PATH: /simulations/{simulationId}/paths/{pathId}/sections
    ///

    // METHOD: GET
    public getSectionsByPath(simulationId: number, pathId: number): Promise<IPath[]> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/paths/{pathId}/sections",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    pathId
                },
                query: {}
            }
        }).then((data: any) => {
            let promises = data.map((path: ISection) => {
                return this.getDatacenter(simulationId, path.datacenterId).then((datacenter: any) => {
                    path.datacenter = datacenter;
                });
            });
            return Promise.all(promises).then(() => {
                return data;
            });
        });
    }

    ///
    // PATH: /simulations/{simulationId}/paths/{pathId}/sections/{sectionId}
    ///

    // METHOD: GET
    public getSection(simulationId: number, pathId: number, sectionId: number): Promise<ISection> {
        return ServerConnection.send({
            path: "/simulations/{simulationId}/paths/{pathId}/sections/{sectionId}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    simulationId,
                    pathId,
                    sectionId
                },
                query: {}
            }
        }).then((data: any) => {
            return this.getDatacenter(simulationId, data.datacenterId).then((datacenter: any) => {
                data.datacenter = datacenter;
                return data;
            });
        });
    }

    ///
    // PATH: /specifications/psus
    ///

    // METHOD: GET
    public getAllPSUSpecs(): Promise<IPSU[]> {
        let psus;
        return ServerConnection.send({
            path: "/specifications/psus",
            method: "GET",
            parameters: {
                body: {},
                path: {},
                query: {}
            }
        }).then((data: any) => {
            psus = data;

            let promises = [];
            data.forEach((psu: IPSU) => {
                promises.push(this.getFailureModel(psu.failureModelId));
            });
            return Promise.all(promises);
        }).then((data: any) => {
            return psus;
        });
    }

    ///
    // PATH: /specifications/psus/{id}
    ///

    // METHOD: GET
    public getPSUSpec(id: number): Promise<IPSU> {
        let psu;

        return ServerConnection.send({
            path: "/specifications/psus/{id}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    id
                },
                query: {}
            }
        }).then((data: any) => {
            psu = data;
            return this.getFailureModel(data.failureModelId);
        }).then((data: any) => {
            psu.failureModel = data;
            return psu;
        });
    }

    ///
    // PATH: /specifications/cooling-items
    ///

    // METHOD: GET
    public getAllCoolingItemSpecs(): Promise<ICoolingItem[]> {
        let coolingItems;

        return ServerConnection.send({
            path: "/specifications/cooling-items",
            method: "GET",
            parameters: {
                body: {},
                path: {},
                query: {}
            }
        }).then((data: any) => {
            coolingItems = data;

            let promises = [];
            data.forEach((item: ICoolingItem) => {
                promises.push(this.getFailureModel(item.failureModelId));
            });
            return Promise.all(promises);
        }).then((data: any) => {
            return coolingItems;
        });
    }

    ///
    // PATH: /specifications/cooling-items/{id}
    ///

    // METHOD: GET
    public getCoolingItemSpec(id: number): Promise<IPSU> {
        let coolingItem;

        return ServerConnection.send({
            path: "/specifications/cooling-items/{id}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    id
                },
                query: {}
            }
        }).then((data: any) => {
            coolingItem = data;
            return this.getFailureModel(data.failureModelId);
        }).then((data: any) => {
            coolingItem.failureModel = data;
            return coolingItem;
        });
    }

    ///
    // PATH: /schedulers
    ///

    // METHOD: GET
    public getAllSchedulers(): Promise<IScheduler[]> {
        return ServerConnection.send({
            path: "/schedulers",
            method: "GET",
            parameters: {
                body: {},
                path: {},
                query: {}
            }
        });
    }

    ///
    // PATH: /traces
    ///

    // METHOD: GET
    public getAllTraces(): Promise<ITrace[]> {
        return ServerConnection.send({
            path: "/traces",
            method: "GET",
            parameters: {
                body: {},
                path: {},
                query: {}
            }
        });
    }

    ///
    // PATH: /traces/{traceId}
    ///

    // METHOD: GET
    public getTrace(traceId: number): Promise<ITrace> {
        let trace;

        return ServerConnection.send({
            path: "/traces/{traceId}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    traceId
                },
                query: {}
            }
        }).then((data: any) => {
            trace = data;
            return this.getTasksByTrace(traceId);
        }).then((data: any) => {
            trace.tasks = data;
            return trace;
        });
    }

    ///
    // PATH: /traces/{traceId}/tasks
    ///

    // METHOD: GET
    public getTasksByTrace(traceId: number): Promise<ITask[]> {
        return ServerConnection.send({
            path: "/traces/{traceId}/tasks",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    traceId
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /specifications/failure-models
    ///

    // METHOD: GET
    public getAllFailureModels(): Promise<IFailureModel[]> {
        return ServerConnection.send({
            path: "/specifications/failure-models",
            method: "GET",
            parameters: {
                body: {},
                path: {},
                query: {}
            }
        });
    }

    ///
    // PATH: /specifications/failure-models/{id}
    ///

    // METHOD: GET
    public getFailureModel(id: number): Promise<IFailureModel> {
        return ServerConnection.send({
            path: "/specifications/failure-models/{id}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    id
                },
                query: {}
            }
        });
    }

    ///
    // PATH: /specifications/[units]
    ///

    // METHOD: GET
    public getAllSpecificationsOfType(typePlural: string): Promise<INodeUnit> {
        let specs: any;
        return ServerConnection.send({
            path: "/specifications/" + typePlural,
            method: "GET",
            parameters: {
                body: {},
                path: {},
                query: {}
            }
        }).then((data: any) => {
            specs = data;

            let promises = [];
            data.forEach((unit: INodeUnit) => {
                promises.push(this.getFailureModel(unit.failureModelId));
            });
            return Promise.all(promises);
        }).then((data: any) => {
            return specs;
        });
    }

    ///
    // PATH: /specifications/[units]/{id}
    ///

    // METHOD: GET
    public getSpecificationOfType(typePlural: string, id: number): Promise<INodeUnit> {
        let spec;

        return ServerConnection.send({
            path: "/specifications/" + typePlural + "/{id}",
            method: "GET",
            parameters: {
                body: {},
                path: {
                    id
                },
                query: {}
            }
        }).then((data: any) => {
            spec = data;
            return this.getFailureModel(data.failureModelId);
        }).then((data: any) => {
            spec.failureModel = data;
            return spec;
        });
    }


    ///
    // HELPER METHODS
    ///

    private loadRoomTiles(simulationId: number, datacenterId: number, room: IRoom): Promise<IRoom> {
        return this.getTilesByRoom(simulationId, datacenterId, room.id).then((data: any) => {
            room.tiles = data;
            return room;
        });
    }

    private loadTileObject(simulationId: number, datacenterId: number, roomId: number, tile: ITile): Promise<ITile> {
        let promise;

        switch (tile.objectType) {
            case "RACK":
                promise = this.getRack(simulationId, datacenterId, roomId, tile.id).then((data: IRack) => {
                    tile.object = data;
                });
                break;
            case "PSU":
                promise = this.getPSU(simulationId, datacenterId, roomId, tile.id).then((data: IPSU) => {
                    tile.object = data;
                });
                break;
            case "COOLING_ITEM":
                promise = this.getCoolingItem(simulationId, datacenterId, roomId, tile.id).then((data: ICoolingItem) => {
                    tile.object = data;
                });
                break;
            default:
                promise = new Promise((resolve, reject) => {
                    resolve(undefined);
                });
        }

        return promise.then(() => {
            return tile;
        })
    }

    private parseSimulationTimestamps(simulation: ISimulation): void {
        simulation.datetimeCreatedParsed = Util.parseDateTime(simulation.datetimeCreated);
        simulation.datetimeLastEditedParsed = Util.parseDateTime(simulation.datetimeLastEdited);
    }

    private loadFailureModel(data: any): Promise<any> {
        return this.getFailureModel(data.failureModelId).then((failureModel: IFailureModel) => {
            data.failureModel = failureModel;
            return data;
        });
    }

    private loadUnitsOfType(idListName: string, objectListName: string, machine: IMachine): Promise<IMachine> {
        machine[objectListName] = [];

        let promises = machine[idListName].map((item) => {
            return this.getSpecificationOfType(objectListName, item).then((data) => {
                machine[objectListName].push(data);
            });
        });

        return Promise.all(promises).then(() => {
            return machine;
        })
    }

    private loadMachineUnits(machine: IMachine): Promise<IMachine> {
        let listNames = [
            {
                idListName: "cpuIds",
                objectListName: "cpus"
            }, {
                idListName: "gpuIds",
                objectListName: "gpus"
            }, {
                idListName: "memoryIds",
                objectListName: "memories"
            }, {
                idListName: "storageIds",
                objectListName: "storages"
            }
        ];

        let promises = listNames.map((item: any) => {
            return this.loadUnitsOfType(item.idListName, item.objectListName, machine);
        });

        return Promise.all(promises).then(() => {
            return machine;
        });
    }
}
