import {AppMode, MapController, InteractionLevel} from "../mapcontroller";
import {MapView} from "../../views/mapview";
import * as $ from "jquery";


/**
 * Class responsible for rendering object mode and handling its UI interactions.
 */
export class ObjectModeController {
    public currentObject: IDCObject;
    public objectType: string;
    public currentRack: IRack;
    public currentPSU: IPSU;
    public currentCoolingItem: ICoolingItem;
    public currentObjectTile: ITile;

    private mapController: MapController;
    private mapView: MapView;


    constructor(mapController: MapController) {
        this.mapController = mapController;
        this.mapView = this.mapController.mapView;
    }

    /**
     * Performs the necessary setup actions and enters object mode.
     *
     * @param tile A reference to the tile containing the rack that was selected.
     */
    public enterMode(tile: ITile) {
        this.currentObjectTile = tile;
        this.mapView.grayLayer.currentObjectTile = tile;
        this.currentObject = tile.object;
        this.objectType = tile.objectType;

        // Show the corresponding sub-menu of object mode
        $(".object-sub-menu").hide();

        switch (this.objectType) {
            case "RACK":
                $("#rack-sub-menu").show();
                this.currentRack = <IRack>this.currentObject;
                $("#rack-name-input").val(this.currentRack.name);
                this.populateNodeList();

                break;

            case "PSU":
                $("#psu-sub-menu").show();
                this.currentPSU = <IPSU>this.currentObject;

                break;

            case "COOLING_ITEM":
                $("#cooling-item-sub-menu").show();
                this.currentCoolingItem = <ICoolingItem>this.currentObject;

                break;
        }

        this.mapView.grayLayer.drawRackLevel();
        MapController.hideAndShowMenus("#object-menu");
        this.scrollToBottom();

        if (this.mapController.appMode === AppMode.SIMULATION) {
            this.mapController.simulationController.transitionFromRoomToRack();
        }
    }

    /**
     * Leaves object mode and transfers to room mode.
     */
    public goToRoomMode() {
        this.mapController.interactionLevel = InteractionLevel.ROOM;
        this.mapView.grayLayer.hideRackLevel();
        MapController.hideAndShowMenus("#room-menu");
        this.mapController.roomModeController.enterMode(this.mapController.roomModeController.currentRoom);

        if (this.mapController.appMode === AppMode.SIMULATION) {
            this.mapController.simulationController.transitionFromRackToRoom();
        }
    }

    /**
     * Connects all DOM event listeners to their respective element targets.
     */
    public setupEventListeners() {
        // Handler for saving a new rack name
        $("#rack-name-save").on("click", () => {
            this.currentRack.name = $("#rack-name-input").val();
            this.mapController.api.updateRack(this.mapView.simulation.id,
                this.mapView.currentDatacenter.id, this.mapController.roomModeController.currentRoom.id,
                this.mapController.objectModeController.currentObjectTile.id, this.currentRack).then(
                () => {
                    this.mapController.showInfoBalloon("Rack name saved", "info");
                });
        });

        let nodeListContainer = $(".node-list-container");

        // Handler for the 'add' button of each machine slot of the rack
        nodeListContainer.on("click", ".add-node", (event: JQueryEventObject) => {
            // Convert the DOM element index to a JS array index
            let index = this.currentRack.machines.length - $(event.target).closest(".node-element").index() - 1;

            // Insert an empty machine at the selected position
            this.mapController.api.addMachineToRack(this.mapView.simulation.id,
                this.mapView.currentDatacenter.id, this.mapController.roomModeController.currentRoom.id,
                this.mapController.objectModeController.currentObjectTile.id, {
                    id: -1,
                    rackId: this.currentRack.id,
                    position: index,
                    tags: [],
                    cpuIds: [],
                    gpuIds: [],
                    memoryIds: [],
                    storageIds: []
                }).then((data: IMachine) => {
                this.currentRack.machines[index] = data;
                this.populateNodeList();
                this.mapView.dcObjectLayer.draw();
            });

            event.stopPropagation();
        });

        // Handler for the 'remove' button of each machine slot of the rack
        nodeListContainer.on("click", ".remove-node", (event: JQueryEventObject) => {
            let target = $(event.target);
            MapController.showConfirmDeleteDialog("machine", () => {
                // Convert the DOM element index to a JS array index
                let index = this.currentRack.machines.length - target.closest(".node-element").index() - 1;

                this.mapController.api.deleteMachine(this.mapView.simulation.id,
                    this.mapView.currentDatacenter.id, this.mapController.roomModeController.currentRoom.id,
                    this.mapController.objectModeController.currentObjectTile.id,
                    index).then(() => {
                    this.currentRack.machines[index] = null;
                    this.populateNodeList();
                    this.mapView.dcObjectLayer.draw();
                });
            });
            event.stopPropagation();
        });

        // Handler for every node element, triggering node mode
        nodeListContainer.on("click", ".node-element", (event: JQueryEventObject) => {
            let domIndex = $(event.target).closest(".node-element").index();
            let index = this.currentRack.machines.length - domIndex - 1;
            let machine = this.currentRack.machines[index];

            if (machine != null) {
                this.mapController.interactionLevel = InteractionLevel.NODE;

                // Gray out the other nodes
                $(event.target).closest(".node-list-container").children(".node-element").each((nodeIndex: number, element: Element) => {
                    if (nodeIndex !== domIndex) {
                        $(element).children(".node-element-overlay").removeClass("hidden");
                    } else {
                        $(element).children(".node-element-overlay").addClass("hidden");
                    }
                });

                this.mapController.nodeModeController.enterMode(machine);
            }
        });

        // Handler for rack deletion button
        $("#rack-deletion").on("click", () => {
            MapController.showConfirmDeleteDialog("rack", () => {
                this.mapController.api.deleteRack(this.mapView.simulation.id,
                    this.mapView.currentDatacenter.id, this.mapController.roomModeController.currentRoom.id,
                    this.mapController.objectModeController.currentObjectTile.id).then(() => {
                    this.currentObjectTile.object = undefined;
                    this.currentObjectTile.objectType = undefined;
                    this.currentObjectTile.objectId = undefined;
                    this.mapView.redrawMap();
                    this.goToRoomMode();
                });
            });
        });

        // Handler for PSU deletion button
        $("#psu-deletion").on("click", () => {
            MapController.showConfirmDeleteDialog("PSU", () => {
                this.mapController.api.deletePSU(this.mapView.simulation.id,
                    this.mapView.currentDatacenter.id, this.mapController.roomModeController.currentRoom.id,
                    this.mapController.objectModeController.currentObjectTile.id).then(() => {
                    this.mapView.redrawMap();
                    this.goToRoomMode();
                });
                this.currentObjectTile.object = undefined;
                this.currentObjectTile.objectType = undefined;
                this.currentObjectTile.objectId = undefined;
            });
        });

        // Handler for Cooling Item deletion button
        $("#cooling-item-deletion").on("click", () => {
            MapController.showConfirmDeleteDialog("cooling item", () => {
                this.mapController.api.deleteCoolingItem(this.mapView.simulation.id,
                    this.mapView.currentDatacenter.id, this.mapController.roomModeController.currentRoom.id,
                    this.mapController.objectModeController.currentObjectTile.id).then(() => {
                    this.mapView.redrawMap();
                    this.goToRoomMode();
                });
                this.currentObjectTile.object = undefined;
                this.currentObjectTile.objectType = undefined;
                this.currentObjectTile.objectId = undefined;
            });
        });
    }

    public updateNodeComponentOverlays(): void {
        if (this.currentRack === undefined || this.currentRack.machines === undefined) {
            return;
        }

        for (let i = 0; i < this.currentRack.machines.length; i++) {
            if (this.currentRack.machines[i] === null) {
                continue;
            }

            let container = this.mapController.appMode === AppMode.CONSTRUCTION ? ".construction" : ".simulation";
            let element = $(container + " .node-element").eq(this.currentRack.machines.length - i - 1);
            if (this.currentRack.machines[i].cpus.length !== 0) {
                element.find(".overlay-cpu").addClass("hidden");
            } else {
                element.find(".overlay-cpu").removeClass("hidden");
            }
            if (this.currentRack.machines[i].gpus.length !== 0) {
                element.find(".overlay-gpu").addClass("hidden");
            } else {
                element.find(".overlay-gpu").removeClass("hidden");
            }
            if (this.currentRack.machines[i].memories.length !== 0) {
                element.find(".overlay-memory").addClass("hidden");
            } else {
                element.find(".overlay-memory").removeClass("hidden");
            }
            if (this.currentRack.machines[i].storages.length !== 0) {
                element.find(".overlay-storage").addClass("hidden");
            } else {
                element.find(".overlay-storage").removeClass("hidden");
            }
        }
    }

    /**
     * Dynamically generates and inserts HTML code for every node in the current rack.
     */
    private populateNodeList(): void {
        let type, content;
        let container = $(".node-list-container");

        // Remove any previously present node elements
        container.children().remove(".node-element");

        for (let i = 0; i < this.currentRack.machines.length; i++) {
            // Depending on whether the current machine slot is filled, allow removing or adding a new machine by adding
            // the appropriate button next to the machine slot
            type = (this.currentRack.machines[i] == null ? "glyphicon-plus add-node" : "glyphicon-remove remove-node");
            content =
                '<div class="node-element" data-id="' + (this.currentRack.machines[i] === null ?
                    "" : this.currentRack.machines[i].id) + '">' +
                '  <div class="node-element-overlay hidden"></div>' +
                '  <a class="node-element-btn glyphicon ' + type + '" href="javascript:void(0)"></a>' +
                '  <div class="node-element-number">' + (i + 1) + '</div>';
            if (this.currentRack.machines[i] !== null) {
                content +=
                    '<div class="node-element-content">' +
                    '  <img src="img/app/node-cpu.png">' +
                    '  <img src="img/app/node-gpu.png">' +
                    '  <img src="img/app/node-memory.png">' +
                    '  <img src="img/app/node-storage.png">' +
                    '  <img src="img/app/node-network.png">' +
                    '  <div class="icon-overlay overlay-cpu hidden"></div>' +
                    '  <div class="icon-overlay overlay-gpu hidden"></div>' +
                    '  <div class="icon-overlay overlay-memory hidden"></div>' +
                    '  <div class="icon-overlay overlay-storage hidden"></div>' +
                    '  <div class="icon-overlay overlay-network"></div>' +
                    '</div>';
            }
            content += '</div>';
            // Insert the generated machine slot into the DOM
            container.prepend(content);
        }

        this.updateNodeComponentOverlays();
    }

    private scrollToBottom(): void {
        let scrollContainer = $('.node-list-container');
        scrollContainer.scrollTop(scrollContainer[0].scrollHeight);
    }
}