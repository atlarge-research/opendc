import {Colors} from "../../colors";
import {Util, IntensityLevel} from "../../util";
import {MapView} from "../mapview";
import {DCProgressBar} from "./dcprogressbar";
import {Layer} from "./layer";
import {CELL_SIZE} from "../../controllers/mapcontroller";


export class DCObjectLayer implements Layer {
    public static ITEM_MARGIN = CELL_SIZE / 7.0;
    public static ITEM_PADDING = CELL_SIZE / 10.0;
    public static STROKE_WIDTH = CELL_SIZE / 20.0;
    public static PROGRESS_BAR_DISTANCE = CELL_SIZE / 17.0;
    public static CONTENT_SIZE = CELL_SIZE - DCObjectLayer.ITEM_MARGIN * 2 - DCObjectLayer.ITEM_PADDING * 3;

    public container: createjs.Container;
    public detailedMode: boolean;
    public coloringMode: boolean;
    public intensityLevels: { [key: number]: IntensityLevel; } = {};

    private mapView: MapView;
    private preload: createjs.LoadQueue;
    private rackSpaceBitmap: createjs.Bitmap;
    private rackEnergyBitmap: createjs.Bitmap;
    private psuBitmap: createjs.Bitmap;
    private coolingItemBitmap: createjs.Bitmap;

    // This associative lookup object keeps all DC display objects with as property name the index of the global map
    // array that they are located in.
    private dcObjectMap: { [key: number]: any; };


    public static drawHoverRack(position: IGridPosition): createjs.Container {
        const result = new createjs.Container();

        DCObjectLayer.drawItemRectangle(
            position, Colors.RACK_BACKGROUND, Colors.RACK_BORDER, result
        );
        DCProgressBar.drawItemProgressRectangle(
            position, Colors.RACK_SPACE_BAR_BACKGROUND, result, 0, 1
        );
        DCProgressBar.drawItemProgressRectangle(
            position, Colors.RACK_ENERGY_BAR_BACKGROUND, result, 1, 1
        );

        return result;
    }

    public static drawHoverPSU(position: IGridPosition): createjs.Container {
        const result = new createjs.Container();

        DCObjectLayer.drawItemRectangle(
            position, Colors.PSU_BACKGROUND, Colors.PSU_BORDER, result
        );

        return result;
    }

    public static drawHoverCoolingItem(position: IGridPosition): createjs.Container {
        const result = new createjs.Container();

        DCObjectLayer.drawItemRectangle(
            position, Colors.COOLING_ITEM_BACKGROUND, Colors.COOLING_ITEM_BORDER, result
        );

        return result;
    }

    /**
     * Draws an object rectangle in a given grid cell, with margin around its border.
     *
     * @param position The coordinates of the grid cell in which it should be located
     * @param color The background color of the item
     * @param borderColor The border color
     * @param container The container to which it should be drawn
     * @returns {createjs.Shape} The drawn shape
     */
    private static drawItemRectangle(position: IGridPosition, color: string, borderColor: string,
                                     container: createjs.Container): createjs.Shape {
        const shape = new createjs.Shape();
        shape.graphics.beginStroke(borderColor);
        shape.graphics.setStrokeStyle(DCObjectLayer.STROKE_WIDTH);
        shape.graphics.beginFill(color);
        shape.graphics.drawRect(
            position.x * CELL_SIZE + DCObjectLayer.ITEM_MARGIN,
            position.y * CELL_SIZE + DCObjectLayer.ITEM_MARGIN,
            CELL_SIZE - DCObjectLayer.ITEM_MARGIN * 2,
            CELL_SIZE - DCObjectLayer.ITEM_MARGIN * 2
        );
        container.addChild(shape);
        return shape;
    }

    /**
     * Draws an bitmap in item format.
     *
     * @param position The coordinates of the grid cell in which it should be located
     * @param container The container to which it should be drawn
     * @param originBitmap The bitmap that should be drawn
     * @returns {createjs.Bitmap} The drawn bitmap
     */
    private static drawItemIcon(position: IGridPosition, container: createjs.Container,
                                originBitmap: createjs.Bitmap): createjs.Bitmap {
        const bitmap = originBitmap.clone();
        container.addChild(bitmap);
        bitmap.x = position.x * CELL_SIZE + DCObjectLayer.ITEM_MARGIN + DCObjectLayer.ITEM_PADDING * 1.5;
        bitmap.y = position.y * CELL_SIZE + DCObjectLayer.ITEM_MARGIN + DCObjectLayer.ITEM_PADDING * 1.5;
        return bitmap;
    }

    constructor(mapView: MapView) {
        this.mapView = mapView;
        this.container = new createjs.Container();

        this.detailedMode = true;
        this.coloringMode = false;

        this.preload = new createjs.LoadQueue();
        this.preload.addEventListener("complete", () => {
            this.rackSpaceBitmap = new createjs.Bitmap(<HTMLImageElement>this.preload.getResult("rack-space"));
            this.rackEnergyBitmap = new createjs.Bitmap(<HTMLImageElement>this.preload.getResult("rack-energy"));
            this.psuBitmap = new createjs.Bitmap(<HTMLImageElement>this.preload.getResult("psu"));
            this.coolingItemBitmap = new createjs.Bitmap(<HTMLImageElement>this.preload.getResult("coolingitem"));

            // Scale the images
            this.rackSpaceBitmap.scaleX = DCProgressBar.PROGRESS_BAR_WIDTH / this.rackSpaceBitmap.image.width;
            this.rackSpaceBitmap.scaleY = DCProgressBar.PROGRESS_BAR_WIDTH / this.rackSpaceBitmap.image.height;

            this.rackEnergyBitmap.scaleX = DCProgressBar.PROGRESS_BAR_WIDTH / this.rackEnergyBitmap.image.width;
            this.rackEnergyBitmap.scaleY = DCProgressBar.PROGRESS_BAR_WIDTH / this.rackEnergyBitmap.image.height;

            this.psuBitmap.scaleX = DCObjectLayer.CONTENT_SIZE / this.psuBitmap.image.width;
            this.psuBitmap.scaleY = DCObjectLayer.CONTENT_SIZE / this.psuBitmap.image.height;

            this.coolingItemBitmap.scaleX = DCObjectLayer.CONTENT_SIZE / this.coolingItemBitmap.image.width;
            this.coolingItemBitmap.scaleY = DCObjectLayer.CONTENT_SIZE / this.coolingItemBitmap.image.height;


            this.populateObjectList();
            this.draw();

            this.mapView.updateScene = true;
        });

        this.preload.loadFile({id: "rack-space", src: 'img/app/rack-space.png'});
        this.preload.loadFile({id: "rack-energy", src: 'img/app/rack-energy.png'});
        this.preload.loadFile({id: "psu", src: 'img/app/psu.png'});
        this.preload.loadFile({id: "coolingitem", src: 'img/app/coolingitem.png'});
    }

    /**
     * Generates a list of DC objects with their associated display objects.
     */
    public populateObjectList(): void {
        this.dcObjectMap = {};

        this.mapView.currentDatacenter.rooms.forEach((room: IRoom) => {
            room.tiles.forEach((tile: ITile) => {
                if (tile.object !== undefined) {
                    const index = tile.position.y * MapView.MAP_SIZE + tile.position.x;

                    switch (tile.objectType) {
                        case "RACK":
                            this.dcObjectMap[index] = {
                                spaceBar: new DCProgressBar(this.container,
                                    Colors.RACK_SPACE_BAR_BACKGROUND, Colors.RACK_SPACE_BAR_FILL,
                                    this.rackSpaceBitmap, tile.position, 0,
                                    Util.getFillRatio((<IRack>tile.object).machines)),
                                energyBar: new DCProgressBar(this.container,
                                    Colors.RACK_ENERGY_BAR_BACKGROUND, Colors.RACK_ENERGY_BAR_FILL,
                                    this.rackEnergyBitmap, tile.position, 1,
                                    Util.getFillRatio((<IRack>tile.object).machines)),
                                itemRect: createjs.Shape,
                                tile: tile, model: tile.object, position: tile.position, type: tile.objectType
                            };

                            break;
                        case "COOLING_ITEM":
                            this.dcObjectMap[index] = {
                                itemRect: createjs.Shape, batteryIcon: createjs.Bitmap,
                                tile: tile, model: tile.object, position: tile.position, type: tile.objectType
                            };
                            break;
                        case "PSU":
                            this.dcObjectMap[index] = {
                                itemRect: createjs.Shape, freezeIcon: createjs.Bitmap,
                                tile: tile, model: tile.object, position: tile.position, type: tile.objectType
                            };
                            break;
                    }
                }
            });
        });
    }

    public draw(): void {
        this.container.removeAllChildren();

        this.container.cursor = "pointer";

        for (let property in this.dcObjectMap) {
            if (this.dcObjectMap.hasOwnProperty(property)) {
                const currentObject = this.dcObjectMap[property];

                switch (currentObject.type) {
                    case "RACK":
                        let color = Colors.RACK_BACKGROUND;

                        if (this.coloringMode && currentObject.tile.roomId ===
                            this.mapView.mapController.roomModeController.currentRoom.id) {
                            color = Util.convertIntensityToColor(this.intensityLevels[currentObject.model.id]);
                        }

                        currentObject.itemRect = DCObjectLayer.drawItemRectangle(
                            currentObject.position, color, Colors.RACK_BORDER, this.container
                        );

                        if (this.detailedMode) {
                            currentObject.spaceBar.fillRatio = Util.getFillRatio(currentObject.model.machines);
                            currentObject.energyBar.fillRatio = Util.getEnergyRatio(currentObject.model);

                            currentObject.spaceBar.draw();
                            currentObject.energyBar.draw();
                        }
                        break;
                    case "COOLING_ITEM":
                        currentObject.itemRect = DCObjectLayer.drawItemRectangle(
                            currentObject.position, Colors.COOLING_ITEM_BACKGROUND, Colors.COOLING_ITEM_BORDER,
                            this.container
                        );

                        currentObject.freezeIcon = DCObjectLayer.drawItemIcon(currentObject.position, this.container,
                            this.coolingItemBitmap);
                        break;
                    case "PSU":
                        currentObject.itemRect = DCObjectLayer.drawItemRectangle(
                            currentObject.position, Colors.PSU_BACKGROUND, Colors.PSU_BORDER,
                            this.container
                        );

                        currentObject.batteryIcon = DCObjectLayer.drawItemIcon(currentObject.position, this.container,
                            this.psuBitmap);
                        break;
                }
            }
        }

        this.mapView.updateScene = true;
    }
}
