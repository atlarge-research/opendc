import {InteractionLevel} from "../../controllers/mapcontroller";
import {Util, IntensityLevel} from "../../util";
import {Colors} from "../../colors";
import {MapView} from "../mapview";
import {Layer} from "./layer";


/**
 * Class responsible for rendering the rooms.
 */
export class RoomLayer implements Layer {
    public container: createjs.Container;
    public coloringMode: boolean;
    public selectedTiles: ITile[];
    public selectedTileObjects: TilePositionObject[];
    public intensityLevels: { [key: number]: IntensityLevel; } = {};

    private mapView: MapView;
    private allRoomTileObjects: TilePositionObject[];
    private validNextTilePositions: IGridPosition[];


    constructor(mapView: MapView) {
        this.mapView = mapView;
        this.container = new createjs.Container();

        this.allRoomTileObjects = [];
        this.selectedTiles = [];
        this.validNextTilePositions = [];
        this.selectedTileObjects = [];
        this.coloringMode = false;

        this.draw();
    }

    /**
     * Draws all rooms to the canvas.
     */
    public draw() {
        this.container.removeAllChildren();

        this.mapView.currentDatacenter.rooms.forEach((room: IRoom) => {
            let color = Colors.ROOM_DEFAULT;

            if (this.coloringMode && room.roomType === "SERVER" && this.intensityLevels[room.id] !== undefined) {
                color = Util.convertIntensityToColor(this.intensityLevels[room.id]);
            }

            room.tiles.forEach((tile: ITile) => {
                this.allRoomTileObjects.push({
                    position: tile.position,
                    tileObject: MapView.drawRectangle(tile.position, color, this.container)
                });
            });
        });
    }

    /**
     * Adds a newly selected tile to the list of selected tiles.
     *
     * If the tile was already selected beforehand, it is removed.
     *
     * @param tile The tile to be added
     */
    public addSelectedTile(tile: ITile): void {
        this.selectedTiles.push(tile);

        let tileObject = MapView.drawRectangle(tile.position, Colors.ROOM_SELECTED, this.container);
        this.selectedTileObjects.push({
            position: {x: tile.position.x, y: tile.position.y},
            tileObject: tileObject
        });

        this.validNextTilePositions = Util.deriveValidNextTilePositions(
            this.mapView.currentDatacenter.rooms, this.selectedTiles);

        this.mapView.updateScene = true;
    }

    /**
     * Removes a selected tile (upon being clicked on again).
     *
     * @param position The position at which a selected tile should be removed
     * @param objectIndex The index of the tile in the selectedTileObjects array
     */
    public removeSelectedTile(position: IGridPosition, objectIndex: number): void {
        let index = Util.tileListPositionIndexOf(this.selectedTiles, position);

        // Check whether the given position doesn't belong to an already removed tile
        if (index === -1) {
            return;
        }

        this.selectedTiles.splice(index, 1);

        this.container.removeChild(this.selectedTileObjects[objectIndex].tileObject);
        this.selectedTileObjects.splice(objectIndex, 1);

        this.validNextTilePositions = Util.deriveValidNextTilePositions(
            this.mapView.currentDatacenter.rooms, this.selectedTiles);

        this.mapView.updateScene = true;
    }

    /**
     * Checks whether a hovered tile is in a valid location.
     *
     * @param position The tile location to be checked
     * @returns {boolean} Whether it is a valid location
     */
    public checkHoverTileValidity(position: IGridPosition): boolean {
        if (this.mapView.mapController.interactionLevel === InteractionLevel.BUILDING) {
            if (this.selectedTiles.length === 0) {
                return !Util.checkRoomCollision(this.mapView.currentDatacenter.rooms, position);
            }
            return Util.positionListContainsPosition(this.validNextTilePositions, position);
        } else if (this.mapView.mapController.interactionLevel === InteractionLevel.ROOM) {
            let valid = false;
            this.mapView.mapController.roomModeController.currentRoom.tiles.forEach((element: ITile) => {
                if (position.x === element.position.x && position.y === element.position.y &&
                    element.object === undefined) {
                    valid = true;
                }
            });
            return valid;
        }
    }

    /**
     * Cancels room tile selection by removing all selected tiles from the scene.
     */
    public cancelRoomConstruction(): void {
        if (this.selectedTiles.length === 0) {
            return;
        }

        this.selectedTileObjects.forEach((tileObject: TilePositionObject) => {
            this.container.removeChild(tileObject.tileObject);
        });

        this.resetTileLists();

        this.mapView.updateScene = true;
    }

    /**
     * Finalizes the selected room tiles into a standard room.
     */
    public finalizeRoom(room: IRoom): void {
        if (this.selectedTiles.length === 0) {
            return;
        }

        this.mapView.currentDatacenter.rooms.push(room);

        this.resetTileLists();

        // Trigger a redraw
        this.draw();
        this.mapView.wallLayer.generateWalls();
        this.mapView.wallLayer.draw();

        this.mapView.updateScene = true;
    }

    private resetTileLists(): void {
        this.selectedTiles = [];
        this.validNextTilePositions = [];
        this.selectedTileObjects = [];
    }

    public setClickable(value: boolean): void {
        this.allRoomTileObjects.forEach((tileObj: TilePositionObject) => {
            tileObj.tileObject.cursor = value ? "pointer" : "default";
        });
    }
}