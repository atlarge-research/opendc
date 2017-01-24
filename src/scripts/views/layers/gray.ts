import {MapView} from "../mapview";
import {Colors} from "../../colors";
import {Util} from "../../util";
import {Layer} from "./layer";


/**
 * Class responsible for graying out non-active UI elements.
 */
export class GrayLayer implements Layer {
    public container: createjs.Container;
    public currentRoom: IRoom;
    public currentObjectTile: ITile;

    private mapView: MapView;
    private grayRoomShape: createjs.Shape;


    constructor(mapView: MapView) {
        this.mapView = mapView;
        this.container = new createjs.Container();
    }

    /**
     * Draws grayed out areas around a currently selected room.
     *
     * @param redraw Whether this is a redraw, or an initial draw action
     */
    public draw(redraw?: boolean): void {
        if (this.currentRoom === undefined) {
            return;
        }

        this.container.removeAllChildren();

        let roomBounds = Util.calculateRoomBounds(this.currentRoom);

        let shape = new createjs.Shape();
        shape.graphics.beginFill(Colors.GRAYED_OUT_AREA);
        shape.cursor = "pointer";

        this.drawLargeRects(shape, roomBounds);
        this.drawFineGrainedRects(shape, roomBounds);

        this.container.addChild(shape);
        if (redraw === true) {
            shape.alpha = 1;
        } else {
            shape.alpha = 0;
            this.mapView.animate(shape, {alpha: 1});
        }

        if (this.grayRoomShape !== undefined && !this.grayRoomShape.visible) {
            this.grayRoomShape = undefined;
            this.drawRackLevel(redraw);
        }

        this.mapView.updateScene = true;
    }

    private drawLargeRects(shape: createjs.Shape, roomBounds: IBounds): void {
        if (roomBounds.min[0] > 0) {
            MapView.drawRectangleToShape({x: 0, y: 0}, shape, roomBounds.min[0], MapView.MAP_SIZE);
        }
        if (roomBounds.min[1] > 0) {
            MapView.drawRectangleToShape({x: roomBounds.min[0], y: 0}, shape, roomBounds.max[0] - roomBounds.min[0],
                roomBounds.min[1]);
        }
        if (roomBounds.max[0] < MapView.MAP_SIZE - 1) {
            MapView.drawRectangleToShape({x: roomBounds.max[0], y: 0}, shape, MapView.MAP_SIZE - roomBounds.max[0],
                MapView.MAP_SIZE);
        }
        if (roomBounds.max[1] < MapView.MAP_SIZE - 1) {
            MapView.drawRectangleToShape({x: roomBounds.min[0], y: roomBounds.max[1]}, shape,
                roomBounds.max[0] - roomBounds.min[0], MapView.MAP_SIZE - roomBounds.max[1]);
        }
    }

    private drawFineGrainedRects(shape: createjs.Shape, roomBounds: IBounds): void {
        for (let x = roomBounds.min[0]; x < roomBounds.max[0]; x++) {
            for (let y = roomBounds.min[1]; y < roomBounds.max[1]; y++) {
                if (!Util.tileListContainsPosition(this.currentRoom.tiles, {x: x, y: y})) {
                    MapView.drawRectangleToShape({x: x, y: y}, shape);
                }
            }
        }
    }

    public drawRackLevel(redraw?: boolean): void {
        if (this.currentObjectTile === undefined) {
            return;
        }

        this.grayRoomShape = new createjs.Shape();
        this.grayRoomShape.graphics.beginFill(Colors.GRAYED_OUT_AREA);
        this.grayRoomShape.cursor = "pointer";
        this.grayRoomShape.alpha = 0;

        this.currentRoom.tiles.forEach((tile: ITile) => {
            if (this.currentObjectTile.position.x !== tile.position.x ||
                this.currentObjectTile.position.y !== tile.position.y) {
                MapView.drawRectangleToShape({x: tile.position.x, y: tile.position.y}, this.grayRoomShape);
            }
        });

        this.container.addChild(this.grayRoomShape);
        if (redraw === true) {
            this.grayRoomShape.alpha = 1;
        } else {
            this.grayRoomShape.alpha = 0;
            this.mapView.animate(this.grayRoomShape, {alpha: 1});
        }
    }

    public hideRackLevel(): void {
        if (this.currentObjectTile === undefined) {
            return;
        }

        this.mapView.animate(this.grayRoomShape, {
            alpha: 0, visible: false
        });
    }

    /**
     * Clears the container.
     */
    public clear(): void {
        this.mapView.animate(this.container, {alpha: 0}, () => {
            this.container.removeAllChildren();
            this.container.alpha = 1;
        });
        this.grayRoomShape = undefined;
        this.currentRoom = undefined;
    }

    /**
     * Checks whether there is already an active room with grayed out areas around it.
     *
     * @returns {boolean} Whether the room is grayed out
     */
    public isGrayedOut(): boolean {
        return this.currentRoom !== undefined;
    }
}