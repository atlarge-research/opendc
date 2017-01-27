import {InteractionMode, MapController} from "../mapcontroller";
import {MapView} from "../../views/mapview";
import * as $ from "jquery";


/**
 * Class responsible for handling building mode interactions.
 */
export class BuildingModeController {
    public newRoomId: number;

    private mapController: MapController;
    private mapView: MapView;


    constructor(mapController: MapController) {
        this.mapController = mapController;
        this.mapView = this.mapController.mapView;
    }

    /**
     * Connects all DOM event listeners to their respective element targets.
     */
    public setupEventListeners() {
        const resetConstructionButtons = () => {
            this.mapController.interactionMode = InteractionMode.DEFAULT;
            this.mapView.hoverLayer.setHoverTileVisibility(false);
            $("#room-construction").text("Construct new room");
            $("#room-construction-cancel").slideToggle(300);
        };

        // Room construction button
        $("#room-construction").on("click", (event: JQueryEventObject) => {
            if (this.mapController.interactionMode === InteractionMode.DEFAULT) {
                this.mapController.interactionMode = InteractionMode.SELECT_ROOM;
                this.mapView.hoverLayer.setHoverTileVisibility(true);
                this.mapController.api.addRoomToDatacenter(this.mapView.simulation.id,
                    this.mapView.currentDatacenter.id).then((room: IRoom) => {
                    this.newRoomId = room.id;
                });
                $(event.target).text("Finalize room");
                $("#room-construction-cancel").slideToggle(300);
            } else if (this.mapController.interactionMode === InteractionMode.SELECT_ROOM) {
                resetConstructionButtons();
                this.finalizeRoom();
            }
        });

        // Cancel button for room construction
        $("#room-construction-cancel").on("click", () => {
            resetConstructionButtons();
            this.cancelRoomConstruction();
        });
    }

    /**
     * Cancels room construction and deletes the temporary room created previously.
     */
    public cancelRoomConstruction() {
        this.mapController.api.deleteRoom(this.mapView.simulation.id,
            this.mapView.currentDatacenter.id, this.newRoomId).then(() => {
            this.mapView.roomLayer.cancelRoomConstruction();
        });
    }

    /**
     * Finalizes room construction by triggering a redraw of the room layer with the new room added.
     */
    public finalizeRoom() {
        this.mapController.api.getRoom(this.mapView.simulation.id,
            this.mapView.currentDatacenter.id, this.newRoomId).then((room: IRoom) => {
            this.mapView.roomLayer.finalizeRoom(room);
        });
    }

    /**
     * Adds a newly selected tile to the list of selected tiles.
     *
     * @param position The new tile position to be added
     */
    public addSelectedTile(position: IGridPosition): void {
        const tile = {
            id: -1,
            roomId: this.newRoomId,
            position: {x: position.x, y: position.y}
        };
        this.mapController.api.addTileToRoom(this.mapView.simulation.id,
            this.mapView.currentDatacenter.id, this.newRoomId, tile).then((tile: ITile) => {
            this.mapView.roomLayer.addSelectedTile(tile);
        });
    }

    /**
     * Removes a previously selected tile.
     *
     * @param position The position of the tile to be removed
     */
    public removeSelectedTile(position: IGridPosition): void {
        let objectIndex = -1;

        for (let i = 0; i < this.mapView.roomLayer.selectedTileObjects.length; i++) {
            const tile = this.mapView.roomLayer.selectedTileObjects[i];
            if (tile.position.x === position.x && tile.position.y === position.y) {
                objectIndex = i;
            }
        }
        this.mapController.api.deleteTile(this.mapView.simulation.id,
            this.mapView.currentDatacenter.id, this.newRoomId,
            this.mapView.roomLayer.selectedTileObjects[objectIndex].tileObject.id).then(() => {
            this.mapView.roomLayer.removeSelectedTile(position, objectIndex);
        });
    }
}
