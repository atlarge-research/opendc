import {Util} from "../../util";
import {InteractionLevel, MapController, AppMode} from "../mapcontroller";
import {MapView} from "../../views/mapview";
import * as $ from "jquery";


export enum RoomInteractionMode {
    DEFAULT,
    ADD_RACK,
    ADD_PSU,
    ADD_COOLING_ITEM
}


export class RoomModeController {
    public currentRoom: IRoom;
    public roomInteractionMode: RoomInteractionMode;

    private mapController: MapController;
    private mapView: MapView;
    private roomTypes: string[];
    private roomTypeMap: IRoomTypeMap;
    private availablePSUs: IPSU[];
    private availableCoolingItems: ICoolingItem[];


    constructor(mapController: MapController) {
        this.mapController = mapController;
        this.mapView = this.mapController.mapView;

        this.mapController.api.getAllRoomTypes().then((roomTypes: string[]) => {
            this.roomTypes = roomTypes;
            this.roomTypeMap = {};

            this.roomTypes.forEach((type: string) => {
                this.mapController.api.getAllowedObjectsByRoomType(type).then((objects: string[]) => {
                    this.roomTypeMap[type] = objects;
                });
            });

            this.populateRoomTypeDropdown();
        });

        // this.mapController.api.getAllPSUSpecs().then((specs: IPSU[]) => {
        //     this.availablePSUs = specs;
        // });
        //
        // this.mapController.api.getAllCoolingItemSpecs().then((specs: ICoolingItem[]) => {
        //     this.availableCoolingItems = specs;
        // });

        this.roomInteractionMode = RoomInteractionMode.DEFAULT;
    }

    public enterMode(room: IRoom) {
        this.currentRoom = room;
        this.roomInteractionMode = RoomInteractionMode.DEFAULT;

        this.mapView.roomTextLayer.setVisibility(false);

        this.mapView.zoomInOnRoom(this.currentRoom);
        $("#room-name-input").val(this.currentRoom.name);
        MapController.hideAndShowMenus("#room-menu");

        // Pre-select the type of the current room in the dropdown
        const roomTypeDropdown = $("#roomtype-select");
        roomTypeDropdown.find('option').prop("selected", "false");
        const roomTypeIndex = this.roomTypes.indexOf(this.currentRoom.roomType);
        if (roomTypeIndex !== -1) {
            roomTypeDropdown.find('option[value="' + roomTypeIndex + '"]').prop("selected", "true");
        } else {
            roomTypeDropdown.val([]);
        }

        this.populateAllowedObjectTypes();

        this.mapView.roomLayer.setClickable(false);

        if (this.mapController.appMode === AppMode.SIMULATION) {
            this.mapController.simulationController.transitionFromBuildingToRoom();
        }
    }

    public goToBuildingMode() {
        this.mapController.interactionLevel = InteractionLevel.BUILDING;

        if (this.roomInteractionMode !== RoomInteractionMode.DEFAULT) {
            this.roomInteractionMode = RoomInteractionMode.DEFAULT;
            this.mapView.hoverLayer.setHoverItemVisibility(false);
            $("#add-rack-btn").attr("data-active", "false");
            $("#add-psu-btn").attr("data-active", "false");
            $("#add-cooling-item-btn").attr("data-active", "false");
        }

        this.mapView.roomTextLayer.setVisibility(true);

        this.mapView.zoomOutOnDC();
        MapController.hideAndShowMenus("#building-menu");

        this.mapView.roomLayer.setClickable(true);

        if (this.mapController.appMode === AppMode.SIMULATION) {
            this.mapController.simulationController.transitionFromRoomToBuilding();
        }
    }

    public setupEventListeners(): void {
        // Component buttons
        const addRackBtn = $("#add-rack-btn");
        const addPSUBtn = $("#add-psu-btn");
        const addCoolingItemBtn = $("#add-cooling-item-btn");

        const roomTypeDropdown = $("#roomtype-select");

        addRackBtn.on("click", () => {
            this.handleItemClick("RACK");
        });
        addPSUBtn.on("click", () => {
            this.handleItemClick("PSU");
        });
        addCoolingItemBtn.on("click", () => {
            this.handleItemClick("COOLING_ITEM");
        });

        // Handler for saving a new room name
        $("#room-name-save").on("click", () => {
            this.currentRoom.name = $("#room-name-input").val();
            this.mapController.api.updateRoom(this.mapView.simulation.id,
                this.mapView.currentDatacenter.id, this.currentRoom).then(() => {
                this.mapView.roomTextLayer.draw();
                this.mapController.showInfoBalloon("Room name saved", "info");
            });
        });

        // Handler for room deletion button
        $("#room-deletion").on("click", () => {
            MapController.showConfirmDeleteDialog("room", () => {
                this.mapController.api.deleteRoom(this.mapView.simulation.id,
                    this.mapView.currentDatacenter.id, this.currentRoom.id).then(() => {
                    const roomIndex = this.mapView.currentDatacenter.rooms.indexOf(this.currentRoom);
                    this.mapView.currentDatacenter.rooms.splice(roomIndex, 1);

                    this.mapView.redrawMap();
                    this.goToBuildingMode();
                });
            });
        });

        // Handler for the room type dropdown component
        roomTypeDropdown.on("change", () => {
            const newRoomType = this.roomTypes[roomTypeDropdown.val()];
            if (!this.checkRoomTypeLegality(newRoomType)) {
                roomTypeDropdown.val(this.roomTypes.indexOf(this.currentRoom.roomType));
                this.mapController.showInfoBalloon("Room type couldn't be changed, illegal objects", "warning");
                return;
            }

            this.currentRoom.roomType = newRoomType;
            this.mapController.api.updateRoom(this.mapView.simulation.id,
                this.mapView.currentDatacenter.id, this.currentRoom).then(() => {
                this.populateAllowedObjectTypes();
                this.mapView.roomTextLayer.draw();
                this.mapController.showInfoBalloon("Room type changed", "info");
            });
        });
    }

    public handleCanvasMouseClick(gridPos: IGridPosition): void {
        if (this.roomInteractionMode === RoomInteractionMode.DEFAULT) {
            const tileIndex = Util.tileListPositionIndexOf(this.currentRoom.tiles, gridPos);

            if (tileIndex !== -1) {
                const tile = this.currentRoom.tiles[tileIndex];

                if (tile.object !== undefined) {
                    this.mapController.interactionLevel = InteractionLevel.OBJECT;
                    this.mapController.objectModeController.enterMode(tile);
                }
            } else {
                this.goToBuildingMode();
            }
        } else if (this.roomInteractionMode === RoomInteractionMode.ADD_RACK) {
            this.addObject(this.mapView.hoverLayer.hoverTilePosition, "RACK");

        } else if (this.roomInteractionMode === RoomInteractionMode.ADD_PSU) {
            this.addObject(this.mapView.hoverLayer.hoverTilePosition, "PSU");

        } else if (this.roomInteractionMode === RoomInteractionMode.ADD_COOLING_ITEM) {
            this.addObject(this.mapView.hoverLayer.hoverTilePosition, "COOLING_ITEM");

        }
    }

    private handleItemClick(type: string): void {
        const addRackBtn = $("#add-rack-btn");
        const addPSUBtn = $("#add-psu-btn");
        const addCoolingItemBtn = $("#add-cooling-item-btn");
        const allObjectContainers = $(".dc-component-container");
        const objectTypes = [
            {
                type: "RACK",
                mode: RoomInteractionMode.ADD_RACK,
                btn: addRackBtn
            },
            {
                type: "PSU",
                mode: RoomInteractionMode.ADD_PSU,
                btn: addPSUBtn
            },
            {
                type: "COOLING_ITEM",
                mode: RoomInteractionMode.ADD_COOLING_ITEM,
                btn: addCoolingItemBtn
            }
        ];

        allObjectContainers.attr("data-active", "false");

        if (this.roomInteractionMode === RoomInteractionMode.DEFAULT) {
            this.mapView.hoverLayer.setHoverItemVisibility(true, type);

            if (type === "RACK") {
                this.roomInteractionMode = RoomInteractionMode.ADD_RACK;
                addRackBtn.attr("data-active", "true");
            } else if (type === "PSU") {
                this.roomInteractionMode = RoomInteractionMode.ADD_PSU;
                addPSUBtn.attr("data-active", "true");
            } else if (type === "COOLING_ITEM") {
                this.roomInteractionMode = RoomInteractionMode.ADD_COOLING_ITEM;
                addCoolingItemBtn.attr("data-active", "true");
            }

            return;
        }

        let changed = false;
        objectTypes.forEach((objectType: any, index: number) => {
            if (this.roomInteractionMode === objectType.mode) {
                if (changed) {
                    return;
                }
                if (type === objectType.type) {
                    this.roomInteractionMode = RoomInteractionMode.DEFAULT;
                    this.mapView.hoverLayer.setHoverItemVisibility(false);
                    objectType.btn.attr("data-active", "false");
                } else {
                    objectTypes.forEach((otherObjectType, otherIndex: number) => {
                        if (index !== otherIndex) {
                            if (type === otherObjectType.type) {
                                this.mapView.hoverLayer.setHoverItemVisibility(true, type);
                                otherObjectType.btn.attr("data-active", "true");
                                this.roomInteractionMode = otherObjectType.mode;
                            }
                        }
                    });
                }
                changed = true;
            }
        });
    }

    private addObject(position: IGridPosition, type: string): void {
        if (!this.mapView.roomLayer.checkHoverTileValidity(position)) {
            return;
        }

        const tileList = this.mapView.mapController.roomModeController.currentRoom.tiles;

        for (let i = 0; i < tileList.length; i++) {
            if (tileList[i].position.x === position.x && tileList[i].position.y === position.y) {
                if (type === "RACK") {
                    this.mapController.api.addRack(this.mapView.simulation.id,
                        this.mapView.currentDatacenter.id, this.currentRoom.id, tileList[i].id, {
                            id: -1,
                            objectType: "RACK",
                            name: "",
                            capacity: 42,
                            powerCapacityW: 5000
                        }).then((rack: IRack) => {
                        tileList[i].object = rack;
                        tileList[i].objectId = rack.id;
                        tileList[i].objectType = type;
                        this.mapView.dcObjectLayer.populateObjectList();
                        this.mapView.dcObjectLayer.draw();

                        this.mapView.updateScene = true;
                    });
                } else if (type === "PSU") {
                    this.mapController.api.addPSU(this.mapView.simulation.id,
                        this.mapView.currentDatacenter.id, this.currentRoom.id, tileList[i].id, this.availablePSUs[0])
                        .then((psu: IPSU) => {
                            tileList[i].object = psu;
                            tileList[i].objectId = psu.id;
                            tileList[i].objectType = type;
                            this.mapView.dcObjectLayer.populateObjectList();
                            this.mapView.dcObjectLayer.draw();

                            this.mapView.updateScene = true;
                        });
                } else if (type === "COOLING_ITEM") {
                    this.mapController.api.addCoolingItem(this.mapView.simulation.id,
                        this.mapView.currentDatacenter.id, this.currentRoom.id, tileList[i].id,
                        this.availableCoolingItems[0]).then((coolingItem: ICoolingItem) => {
                        tileList[i].object = coolingItem;
                        tileList[i].objectId = coolingItem.id;
                        tileList[i].objectType = type;
                        this.mapView.dcObjectLayer.populateObjectList();
                        this.mapView.dcObjectLayer.draw();

                        this.mapView.updateScene = true;
                    });
                }

                break;
            }
        }
    }

    /**
     * Populates the room-type dropdown element with all available room types
     */
    private populateRoomTypeDropdown(): void {
        const dropdown = $("#roomtype-select");

        this.roomTypes.forEach((type: string, index: number) => {
            dropdown.append($('<option>').text(Util.toSentenceCase(type)).val(index));
        });
    }

    /**
     * Loads all object types that are allowed in the current room into the menu.
     */
    private populateAllowedObjectTypes(): void {
        const addObjectsLabel = $("#add-objects-label");
        const noObjectsInfo = $("#no-objects-info");
        const allowedObjectTypes = this.roomTypeMap[this.currentRoom.roomType];

        $(".dc-component-container").addClass("hidden");

        if (allowedObjectTypes === undefined || allowedObjectTypes === null || allowedObjectTypes.length === 0) {
            addObjectsLabel.addClass("hidden");
            noObjectsInfo.removeClass("hidden");

            return;
        }

        addObjectsLabel.removeClass("hidden");
        noObjectsInfo.addClass("hidden");
        allowedObjectTypes.forEach((type: string) => {
            switch (type) {
                case "RACK":
                    $("#add-rack-btn").removeClass("hidden");
                    break;
                case "PSU":
                    $("#add-psu-btn").removeClass("hidden");
                    break;
                case "COOLING_ITEM":
                    $("#add-cooling-item-btn").removeClass("hidden");
                    break;
            }
        });
    }

    /**
     * Checks whether a given room type can be assigned to the current room based on units already present.
     *
     * @param newRoomType The new room type to be validated
     * @returns {boolean} Whether it is allowed to change the room's type to the new type
     */
    private checkRoomTypeLegality(newRoomType: string): boolean {
        let legality = true;

        this.currentRoom.tiles.forEach((tile: ITile) => {
            if (tile.objectType !== undefined && tile.objectType !== null && tile.objectType !== "" &&
                this.roomTypeMap[newRoomType].indexOf(tile.objectType) === -1) {
                legality = false;
            }
        });

        return legality;
    }
}
