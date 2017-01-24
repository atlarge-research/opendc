import {Layer} from "./layer";
import {MapView} from "../mapview";
import {Colors} from "../../colors";
import {DCObjectLayer} from "./dcobject";
import {CELL_SIZE} from "../../controllers/mapcontroller";


/**
 * Class responsible for rendering the hover layer.
 */
export class HoverLayer implements Layer {
    public container: createjs.Container;
    public hoverTilePosition: IGridPosition;

    private mapView: MapView;
    private hoverTile: createjs.Shape;
    private hoverRack: createjs.Container;
    private hoverPSU: createjs.Container;
    private hoverCoolingItem: createjs.Container;


    constructor(mapView: MapView) {
        this.mapView = mapView;
        this.container = new createjs.Container();

        this.initialDraw();
    }

    /**
     * Draws the hover tile to the container at its current location and with its current color.
     */
    public draw(): void {
        let color;

        if (this.mapView.roomLayer.checkHoverTileValidity(this.hoverTilePosition)) {
            color = Colors.ROOM_HOVER_VALID;
        } else {
            color = Colors.ROOM_HOVER_INVALID;
        }

        this.hoverTile.graphics.clear().beginFill(color)
            .drawRect(this.hoverTilePosition.x * CELL_SIZE, this.hoverTilePosition.y * CELL_SIZE,
                CELL_SIZE, CELL_SIZE)
            .endFill();
        if (this.hoverRack.visible) {
            this.hoverRack.x = this.hoverTilePosition.x * CELL_SIZE;
            this.hoverRack.y = this.hoverTilePosition.y * CELL_SIZE;
        } else if (this.hoverPSU.visible) {
            this.hoverPSU.x = this.hoverTilePosition.x * CELL_SIZE;
            this.hoverPSU.y = this.hoverTilePosition.y * CELL_SIZE;
        } else if (this.hoverCoolingItem.visible) {
            this.hoverCoolingItem.x = this.hoverTilePosition.x * CELL_SIZE;
            this.hoverCoolingItem.y = this.hoverTilePosition.y * CELL_SIZE;
        }
    }

    /**
     * Performs the initial drawing action.
     */
    public initialDraw(): void {
        this.container.removeAllChildren();

        this.hoverTile = new createjs.Shape();

        this.hoverTilePosition = {x: 0, y: 0};

        this.hoverTile = MapView.drawRectangle(this.hoverTilePosition, Colors.ROOM_HOVER_VALID, this.container);
        this.hoverTile.visible = false;

        this.hoverRack = DCObjectLayer.drawHoverRack(this.hoverTilePosition);
        this.hoverPSU = DCObjectLayer.drawHoverPSU(this.hoverTilePosition);
        this.hoverCoolingItem = DCObjectLayer.drawHoverCoolingItem(this.hoverTilePosition);

        this.container.addChild(this.hoverRack);
        this.container.addChild(this.hoverPSU);
        this.container.addChild(this.hoverCoolingItem);

        this.hoverRack.visible = false;
        this.hoverPSU.visible = false;
        this.hoverCoolingItem.visible = false;
    }

    /**
     * Sets the hover tile visibility to true/false.
     *
     * @param value The visibility value
     */
    public setHoverTileVisibility(value: boolean): void {
        this.hoverTile.visible = value;
        this.mapView.updateScene = true;
    }

    /**
     * Sets the hover item visibility to true/false.
     *
     * @param value The visibility value
     * @param type The type of the object to be shown
     */
    public setHoverItemVisibility(value: boolean, type?: string): void {
        if (value === true) {
            this.hoverTile.visible = true;

            this.setHoverItemVisibilities(type);
        } else {
            this.hoverTile.visible = false;
            this.hoverRack.visible = false;
            this.hoverPSU.visible = false;
            this.hoverCoolingItem.visible = false;
        }

        this.mapView.updateScene = true;
    }

    private setHoverItemVisibilities(type: string): void {
        if (type === "RACK") {
            this.hoverRack.visible = true;
            this.hoverPSU.visible = false;
            this.hoverCoolingItem.visible = false;
        } else if (type === "PSU") {
            this.hoverRack.visible = false;
            this.hoverPSU.visible = true;
            this.hoverCoolingItem.visible = false;
        } else if (type === "COOLING_ITEM") {
            this.hoverRack.visible = false;
            this.hoverPSU.visible = false;
            this.hoverCoolingItem.visible = true;
        }
    }
}