///<reference path="../../../typings/globals/createjs-lib/index.d.ts" />
///<reference path="../../../typings/globals/easeljs/index.d.ts" />
///<reference path="../../../typings/globals/tweenjs/index.d.ts" />
///<reference path="../../../typings/globals/preloadjs/index.d.ts" />
///<reference path="../definitions.ts" />
///<reference path="../controllers/mapcontroller.ts" />
import * as $ from "jquery";
import {Util} from "../util";
import {MapController, CELL_SIZE} from "../controllers/mapcontroller";
import {GridLayer} from "./layers/grid";
import {RoomLayer} from "./layers/room";
import {HoverLayer} from "./layers/hover";
import {WallLayer} from "./layers/wall";
import {DCObjectLayer} from "./layers/dcobject";
import {GrayLayer} from "./layers/gray";
import {RoomTextLayer} from "./layers/roomtext";


/**
 * Class responsible for rendering the map, by delegating the rendering tasks to appropriate instances.
 */
export class MapView {
    public static MAP_SIZE = 100;
    public static CELL_SIZE_METERS = 0.5;
    public static MIN_ZOOM = 0.5;
    public static DEFAULT_ZOOM = 2;
    public static MAX_ZOOM = 6;
    public static GAP_CORRECTION_DELTA = 0.2;
    public static ANIMATION_LENGTH = 250;

    // Models
    public simulation: ISimulation;
    public currentDatacenter: IDatacenter;

    // Controllers
    public mapController: MapController;

    // Canvas objects
    public stage: createjs.Stage;
    public mapContainer: createjs.Container;

    // Flag indicating whether the scene should be redrawn
    public updateScene: boolean;
    public animating: boolean;

    // Subviews
    public gridLayer: GridLayer;
    public roomLayer: RoomLayer;
    public dcObjectLayer: DCObjectLayer;
    public roomTextLayer: RoomTextLayer;
    public hoverLayer: HoverLayer;
    public wallLayer: WallLayer;
    public grayLayer: GrayLayer;

    // Dynamic canvas attributes
    public canvasWidth: number;
    public canvasHeight: number;


    /**
     * Draws a line from (x1, y1) to (x2, y2).
     *
     * @param x1 The x coord. of start point
     * @param y1 The y coord. of start point
     * @param x2 The x coord. of end point
     * @param y2 The y coord. of end point
     * @param lineWidth The width of the line to be drawn
     * @param color The color to be used
     * @param container The container to be drawn to
     */
    public static drawLine(x1: number, y1: number, x2: number, y2: number,
                           lineWidth: number, color: string, container: createjs.Container): createjs.Shape {
        let line = new createjs.Shape();
        line.graphics.setStrokeStyle(lineWidth).beginStroke(color);
        line.graphics.moveTo(x1, y1);
        line.graphics.lineTo(x2, y2);
        container.addChild(line);
        return line;
    }

    /**
     * Draws a tile at the given location with the given color.
     *
     * @param position The grid coordinates of the tile
     * @param color The color with which the rectangle should be drawn
     * @param container The container to be drawn to
     * @param sizeX Optional parameter specifying the width of the tile to be drawn (in grid units)
     * @param sizeY Optional parameter specifying the height of the tile to be drawn (in grid units)
     */
    public static drawRectangle(position: IGridPosition, color: string, container: createjs.Container,
                                sizeX?: number, sizeY?: number): createjs.Shape {
        let tile = new createjs.Shape();
        tile.graphics.setStrokeStyle(0);
        tile.graphics.beginFill(color);
        tile.graphics.drawRect(
            position.x * CELL_SIZE - MapView.GAP_CORRECTION_DELTA,
            position.y * CELL_SIZE - MapView.GAP_CORRECTION_DELTA,
            CELL_SIZE * (sizeX === undefined ? 1 : sizeX) + MapView.GAP_CORRECTION_DELTA * 2,
            CELL_SIZE * (sizeY === undefined ? 1 : sizeY) + MapView.GAP_CORRECTION_DELTA * 2
        );
        container.addChild(tile);
        return tile;
    }

    /**
     * Draws a tile at the given location with the given color, and add it to the given shape object.
     *
     * The fill color must be set beforehand, in order to not set it repeatedly and produce unwanted transparent overlap
     * artifacts.
     *
     * @param position The grid coordinates of the tile
     * @param shape The shape to be drawn to
     * @param sizeX Optional parameter specifying the width of the tile to be drawn (in grid units)
     * @param sizeY Optional parameter specifying the height of the tile to be drawn (in grid units)
     */
    public static drawRectangleToShape(position: IGridPosition, shape: createjs.Shape,
                                       sizeX?: number, sizeY?: number) {
        shape.graphics.drawRect(
            position.x * CELL_SIZE - MapView.GAP_CORRECTION_DELTA,
            position.y * CELL_SIZE - MapView.GAP_CORRECTION_DELTA,
            CELL_SIZE * (sizeX === undefined ? 1 : sizeX) + MapView.GAP_CORRECTION_DELTA * 2,
            CELL_SIZE * (sizeY === undefined ? 1 : sizeY) + MapView.GAP_CORRECTION_DELTA * 2
        );
    }

    constructor(simulation: ISimulation, stage: createjs.Stage) {
        this.simulation = simulation;
        let path = this.simulation.paths[this.simulation.paths.length - 1];
        this.currentDatacenter = path.sections[path.sections.length - 1].datacenter;

        this.stage = stage;

        console.log("THE DATA", simulation);

        let canvas = $("#main-canvas");
        this.canvasWidth = canvas.width();
        this.canvasHeight = canvas.height();

        this.mapContainer = new createjs.Container();

        this.initializeLayers();

        this.drawMap();
        this.updateScene = true;
        this.animating = false;

        this.mapController = new MapController(this);

        // Zoom DC to fit, if rooms are present
        if (this.currentDatacenter.rooms.length > 0) {
            this.zoomOutOnDC();
        }

        // Checks at every rendering tick whether the scene has changed, and updates accordingly
        createjs.Ticker.addEventListener("tick", (event: createjs.TickerEvent) => {
            if (this.updateScene || this.animating) {
                if (this.mapController.isInHoverMode()) {
                    this.hoverLayer.draw();
                }

                this.updateScene = false;
                this.stage.update(event);
            }
        });
    }

    private initializeLayers(): void {
        this.gridLayer = new GridLayer(this);
        this.roomLayer = new RoomLayer(this);
        this.dcObjectLayer = new DCObjectLayer(this);
        this.roomTextLayer = new RoomTextLayer(this);
        this.hoverLayer = new HoverLayer(this);
        this.wallLayer = new WallLayer(this);
        this.grayLayer = new GrayLayer(this);
    }

    /**
     * Triggers a redraw and re-population action on all layers.
     */
    public redrawMap(): void {
        this.gridLayer.draw();
        this.roomLayer.draw();
        this.dcObjectLayer.populateObjectList();
        this.dcObjectLayer.draw();
        this.roomTextLayer.draw();
        this.hoverLayer.initialDraw();
        this.wallLayer.generateWalls();
        this.wallLayer.draw();
        this.grayLayer.draw(true);
        this.updateScene = true;
    }

    /**
     * Zooms in on a given position with a given amount.
     *
     * @param position The position that should appear centered after the zoom action
     * @param amount The amount of zooming that should be performed
     */
    public zoom(position: number[], amount: number): void {
        const newZoom = this.mapContainer.scaleX + 0.01 * amount;

        // Check whether zooming too far in / out
        if (newZoom > MapView.MAX_ZOOM ||
            newZoom < MapView.MIN_ZOOM) {
            return;
        }

        // Calculate position difference if zoomed, in order to later compensate for this
        // unwanted movement
        let oldPosition = [
            position[0] - this.mapContainer.x, position[1] - this.mapContainer.y
        ];
        let newPosition = [
            (oldPosition[0] / this.mapContainer.scaleX) * newZoom,
            (oldPosition[1] / this.mapContainer.scaleX) * newZoom
        ];
        let positionDelta = [
            newPosition[0] - oldPosition[0], newPosition[1] - oldPosition[1]
        ];

        // Apply the transformation operation to keep the selected position static
        let newX = this.mapContainer.x - positionDelta[0];
        let newY = this.mapContainer.y - positionDelta[1];

        let finalPos = this.mapController.checkCanvasMovement(newX, newY, newZoom);

        if (!this.animating) {
            this.animate(this.mapContainer, {
                scaleX: newZoom, scaleY: newZoom,
                x: finalPos.x, y: finalPos.y
            });
        }
    }

    /**
     * Adjusts the viewing scale to fully display a selected room and center it in view.
     *
     * @param room The room to be centered
     * @param redraw Optional argument specifying whether this is a scene redraw
     */
    public zoomInOnRoom(room: IRoom, redraw?: boolean): void {
        this.zoomInOnRooms([room]);

        if (redraw === undefined || redraw === false) {
            if (!this.grayLayer.isGrayedOut()) {
                this.grayLayer.currentRoom = room;
                this.grayLayer.draw();
            }
        }

        this.updateScene = true;
    }

    /**
     * Zooms out to global building view.
     */
    public zoomOutOnDC(): void {
        this.grayLayer.clear();

        if (this.currentDatacenter.rooms.length > 0) {
            this.zoomInOnRooms(this.currentDatacenter.rooms);
        }

        this.updateScene = true;
    }

    /**
     * Fits a given list of rooms to view, by scaling the viewport appropriately and moving the mapContainer.
     *
     * @param rooms The array of rooms to be viewed
     */
    private zoomInOnRooms(rooms: IRoom[]): void {
        let bounds = Util.calculateRoomListBounds(rooms);
        let newScale = this.calculateNewScale(bounds);

        // Coordinates of the center of the room, relative to the global origin of the map
        let roomCenterCoords = [
            bounds.center[0] * CELL_SIZE * newScale,
            bounds.center[1] * CELL_SIZE * newScale
        ];
        // Coordinates of the center of the stage (the visible part of the canvas), relative to the global map origin
        let stageCenterCoords = [
            -this.mapContainer.x + this.canvasWidth / 2,
            -this.mapContainer.y + this.canvasHeight / 2
        ];

        let newX = this.mapContainer.x - roomCenterCoords[0] + stageCenterCoords[0];
        let newY = this.mapContainer.y - roomCenterCoords[1] + stageCenterCoords[1];

        let newPosition = this.mapController.checkCanvasMovement(newX, newY, newScale);

        this.animate(this.mapContainer, {
            scaleX: newScale, scaleY: newScale,
            x: newPosition.x, y: newPosition.y
        });
    }

    private calculateNewScale(bounds: IBounds): number {
        const viewPadding = 30;
        const sideMenuWidth = 350;

        let width = bounds.max[0] - bounds.min[0];
        let height = bounds.max[1] - bounds.min[1];

        let scaleX = (this.canvasWidth - 2 * sideMenuWidth) / (width * CELL_SIZE + 2 * viewPadding);
        let scaleY = this.canvasHeight / (height * CELL_SIZE + 2 * viewPadding);

        let newScale = Math.min(scaleX, scaleY);

        if (this.mapContainer.scaleX > MapView.MAX_ZOOM) {
            newScale = MapView.MAX_ZOOM;
        } else if (this.mapContainer.scaleX < MapView.MIN_ZOOM) {
            newScale = MapView.MIN_ZOOM;
        }

        return newScale;
    }

    /**
     * Draws all tiles contained in the MapModel.
     */
    private drawMap(): void {
        // Create and draw the container for the entire map
        let gridPixelSize = CELL_SIZE * MapView.MAP_SIZE;

        // Add a white background to the entire container
        let background = new createjs.Shape();
        background.graphics.beginFill("#fff");
        background.graphics.drawRect(0, 0,
            gridPixelSize, gridPixelSize);
        this.mapContainer.addChild(background);

        this.stage.addChild(this.mapContainer);

        // Set the map container to a default offset and zoom state (overridden if rooms are present)
        this.mapContainer.x = -50;
        this.mapContainer.y = -50;
        this.mapContainer.scaleX = this.mapContainer.scaleY = MapView.DEFAULT_ZOOM;

        this.addLayerContainers();
    }

    private addLayerContainers(): void {
        this.mapContainer.addChild(this.gridLayer.container);
        this.mapContainer.addChild(this.roomLayer.container);
        this.mapContainer.addChild(this.dcObjectLayer.container);
        this.mapContainer.addChild(this.roomTextLayer.container);
        this.mapContainer.addChild(this.hoverLayer.container);
        this.mapContainer.addChild(this.wallLayer.container);
        this.mapContainer.addChild(this.grayLayer.container);
    }

    /**
     * Wrapper function for TweenJS animate functionality.
     *
     * @param target What to animate
     * @param properties Properties to be passed on to TweenJS
     * @param callback To be called when animation ready
     */
    public animate(target: any, properties: any, callback?: () => any): void {
        this.animating = true;
        createjs.Tween.get(target)
            .to(properties, MapView.ANIMATION_LENGTH, createjs.Ease.getPowInOut(4))
            .call(() => {
                this.animating = false;
                this.updateScene = true;

                if (callback !== undefined) {
                    callback();
                }
            });
    }
}