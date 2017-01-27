///<reference path="../../../typings/index.d.ts" />
///<reference path="../views/mapview.ts" />
import * as $ from "jquery";
import {Colors} from "../colors";
import {Util} from "../util";
import {SimulationController} from "./simulationcontroller";
import {MapView} from "../views/mapview";
import {APIController} from "./connection/api";
import {BuildingModeController} from "./modes/building";
import {RoomModeController, RoomInteractionMode} from "./modes/room";
import {ObjectModeController} from "./modes/object";
import {NodeModeController} from "./modes/node";
import {ScaleIndicatorController} from "./scaleindicator";

export const CELL_SIZE = 50;


export enum AppMode {
    CONSTRUCTION,
    SIMULATION
}


/**
 * The current level of datacenter hierarchy that is selected
 */
export enum InteractionLevel {
    BUILDING,
    ROOM,
    OBJECT,
    NODE
}


/**
 * Possible states that the application can be in, in terms of interaction
 */
export enum InteractionMode {
    DEFAULT,
    SELECT_ROOM
}


/**
 * Class responsible for handling user input in the map.
 */
export class MapController {
    public stage: createjs.Stage;
    public mapView: MapView;

    public appMode: AppMode;
    public interactionLevel: InteractionLevel;
    public interactionMode: InteractionMode;

    public buildingModeController: BuildingModeController;
    public roomModeController: RoomModeController;
    public objectModeController: ObjectModeController;
    public nodeModeController: NodeModeController;

    public simulationController: SimulationController;
    public api: APIController;
    private scaleIndicatorController: ScaleIndicatorController;

    private canvas: JQuery;
    private gridDragging: boolean;

    private infoTimeOut: any;
    // Current mouse coordinates on the stage canvas (mainly for zooming purposes)
    private currentStageMouseX: number;

    private currentStageMouseY: number;
    // Keep start coordinates relative to the grid to compute dragging offset later
    private gridDragBeginX: number;

    private gridDragBeginY: number;
    // Keep start coordinates on stage to compute delta values
    private stageDragBeginX: number;
    private stageDragBeginY: number;

    private MAX_DELTA = 5;


    /**
     * Hides all side menus except for the active one.
     *
     * @param activeMenu An identifier (e.g. #room-menu) for the menu container
     */
    public static hideAndShowMenus(activeMenu: string): void {
        $(".menu-container.level-menu").each((index: number, elem: Element) => {
            if ($(elem).is(activeMenu)) {
                $(elem).removeClass("hidden");
            } else {
                $(elem).addClass("hidden");
            }
        });
    }

    constructor(mapView: MapView) {
        this.mapView = mapView;
        this.stage = this.mapView.stage;

        new APIController((apiInstance: APIController) => {
            this.api = apiInstance;

            this.buildingModeController = new BuildingModeController(this);
            this.roomModeController = new RoomModeController(this);
            this.objectModeController = new ObjectModeController(this);
            this.nodeModeController = new NodeModeController(this);
            this.simulationController = new SimulationController(this);

            this.scaleIndicatorController = new ScaleIndicatorController(this);

            this.canvas = $("#main-canvas");

            $(window).on("resize", () => {
                this.onWindowResize();
            });

            this.gridDragging = false;

            this.appMode = AppMode.CONSTRUCTION;
            this.interactionLevel = InteractionLevel.BUILDING;
            this.interactionMode = InteractionMode.DEFAULT;

            this.setAllMenuModes();

            this.setupMapInteractionHandlers();
            this.setupEventListeners();
            this.buildingModeController.setupEventListeners();
            this.roomModeController.setupEventListeners();
            this.objectModeController.setupEventListeners();
            this.nodeModeController.setupEventListeners();

            this.scaleIndicatorController.init($(".scale-indicator"));
            this.scaleIndicatorController.update();

            this.mapView.roomLayer.setClickable(true);

            this.matchUserAuthLevel();
        });
    }

    /**
     * Hides and shows the menu bodies corresponding to the current mode (construction or simulation).
     */
    public setAllMenuModes(): void {
        $(".menu-body" + (this.appMode === AppMode.CONSTRUCTION ? ".construction" : ".simulation")).show();
        $(".menu-body" + (this.appMode === AppMode.CONSTRUCTION ? ".simulation" : ".construction")).hide();
    }

    /**
     * Checks whether the mapContainer is still within its legal bounds.
     *
     * Resets, if necessary, to the most similar still legal position.
     */
    public checkAndResetCanvasMovement(): void {
        if (this.mapView.mapContainer.x + this.mapView.gridLayer.gridPixelSize *
            this.mapView.mapContainer.scaleX < this.mapView.canvasWidth) {
            this.mapView.mapContainer.x = this.mapView.canvasWidth - this.mapView.gridLayer.gridPixelSize *
                this.mapView.mapContainer.scaleX;
        }
        if (this.mapView.mapContainer.x > 0) {
            this.mapView.mapContainer.x = 0;
        }
        if (this.mapView.mapContainer.y + this.mapView.gridLayer.gridPixelSize *
            this.mapView.mapContainer.scaleX < this.mapView.canvasHeight) {
            this.mapView.mapContainer.y = this.mapView.canvasHeight - this.mapView.gridLayer.gridPixelSize *
                this.mapView.mapContainer.scaleX;
        }
        if (this.mapView.mapContainer.y > 0) {
            this.mapView.mapContainer.y = 0;
        }
    }

    /**
     * Checks whether the mapContainer is still within its legal bounds and generates corrections if needed.
     *
     * Does not change the x and y coordinates, only returns.
     */
    public checkCanvasMovement(x: number, y: number, scale: number): IGridPosition {
        const result: IGridPosition = {x: x, y: y};
        if (x + this.mapView.gridLayer.gridPixelSize * scale < this.mapView.canvasWidth) {
            result.x = this.mapView.canvasWidth - this.mapView.gridLayer.gridPixelSize *
                this.mapView.mapContainer.scaleX;
        }
        if (x > 0) {
            result.x = 0;
        }
        if (y + this.mapView.gridLayer.gridPixelSize * scale < this.mapView.canvasHeight) {
            result.y = this.mapView.canvasHeight - this.mapView.gridLayer.gridPixelSize *
                this.mapView.mapContainer.scaleX;
        }
        if (y > 0) {
            result.y = 0;
        }

        return result;
    }

    /**
     * Checks whether the current interaction mode is a hover mode (meaning that there is a hover item present).
     *
     * @returns {boolean} Whether it is in hover mode.
     */
    public isInHoverMode(): boolean {
        return this.roomModeController !== undefined &&
            (this.interactionMode === InteractionMode.SELECT_ROOM ||
            this.roomModeController.roomInteractionMode === RoomInteractionMode.ADD_RACK ||
            this.roomModeController.roomInteractionMode === RoomInteractionMode.ADD_PSU ||
            this.roomModeController.roomInteractionMode === RoomInteractionMode.ADD_COOLING_ITEM);
    }

    public static showConfirmDeleteDialog(itemType: string, onConfirm: () => void): void {
        const modalDialog = <any>$("#confirm-delete");
        modalDialog.find(".modal-body").text("Are you sure you want to delete this " + itemType + "?");

        const callback = () => {
            onConfirm();
            modalDialog.modal("hide");
            modalDialog.find("button.confirm").first().off("click");
            $(document).off("keypress");
        };

        $(document).on("keypress", (event: JQueryEventObject) => {
            if (event.which === 13) {
                callback();
            } else if (event.which === 27) {
                modalDialog.modal("hide");
                $(document).off("keypress");
                modalDialog.find("button.confirm").first().off("click");
            }
        });
        modalDialog.find("button.confirm").first().on("click", callback);
        modalDialog.modal("show");
    }

    /**
     * Shows an informational popup in a corner of the screen, communicating a certain event.
     *
     * @param message The message to be displayed in the body of the popup
     * @param type The severity of the message; Currently supported: "info" and "warning"
     */
    public showInfoBalloon(message: string, type: string): void {
        const balloon = $(".info-balloon");
        balloon.html('<span></span>' + message);
        const callback = () => {
            balloon.fadeOut(300);

            this.infoTimeOut = undefined;
        };
        const DISPLAY_TIME = 3000;

        const balloonIcon = balloon.find("span").first();
        balloonIcon.removeClass();

        balloon.css("background", Colors.INFO_BALLOON_MAP[type]);
        balloonIcon.addClass("glyphicon");
        if (type === "info") {
            balloonIcon.addClass("glyphicon-info-sign");
        } else if (type === "warning") {
            balloonIcon.addClass("glyphicon-exclamation-sign");
        }

        if (this.infoTimeOut === undefined) {
            balloon.fadeIn(300);
            this.infoTimeOut = setTimeout(callback, DISPLAY_TIME);
        } else {
            clearTimeout(this.infoTimeOut);
            this.infoTimeOut = setTimeout(callback, DISPLAY_TIME);
        }
    }

    private setupMapInteractionHandlers(): void {
        this.stage.enableMouseOver(20);

        // Listen for mouse movement events to update hover positions
        this.stage.on("stagemousemove", (event: createjs.MouseEvent) => {
            this.currentStageMouseX = event.stageX;
            this.currentStageMouseY = event.stageY;

            const gridPos = this.convertScreenCoordsToGridCoords([event.stageX, event.stageY]);
            const tileX = gridPos.x;
            const tileY = gridPos.y;

            // Check whether the coordinates of the hover location have changed since the last draw
            if (this.mapView.hoverLayer.hoverTilePosition.x !== tileX) {
                this.mapView.hoverLayer.hoverTilePosition.x = tileX;
                this.mapView.updateScene = true;
            }
            if (this.mapView.hoverLayer.hoverTilePosition.y !== tileY) {
                this.mapView.hoverLayer.hoverTilePosition.y = tileY;
                this.mapView.updateScene = true;
            }
        });

        // Handle mousedown interaction
        this.stage.on("mousedown", (e: createjs.MouseEvent) => {
            this.stageDragBeginX = e.stageX;
            this.stageDragBeginY = e.stageY;
        });

        // Handle map dragging interaction
        // Drag begin and progress handlers
        this.mapView.mapContainer.on("pressmove", (e: createjs.MouseEvent) => {
            if (!this.gridDragging) {
                this.gridDragBeginX = e.stageX - this.mapView.mapContainer.x;
                this.gridDragBeginY = e.stageY - this.mapView.mapContainer.y;
                this.stageDragBeginX = e.stageX;
                this.stageDragBeginY = e.stageY;
                this.gridDragging = true;
            } else {
                this.mapView.mapContainer.x = e.stageX - this.gridDragBeginX;
                this.mapView.mapContainer.y = e.stageY - this.gridDragBeginY;

                this.checkAndResetCanvasMovement();

                this.mapView.updateScene = true;
            }
        });

        // Drag exit handlers
        this.mapView.mapContainer.on("pressup", (e: createjs.MouseEvent) => {
            if (this.gridDragging) {
                this.gridDragging = false;
            }

            if (Math.abs(e.stageX - this.stageDragBeginX) < this.MAX_DELTA &&
                Math.abs(e.stageY - this.stageDragBeginY) < this.MAX_DELTA) {
                this.handleCanvasMouseClick(e.stageX, e.stageY);
            }
        });

        // Disable an ongoing drag action if the mouse leaves the canvas
        this.mapView.stage.on("mouseleave", () => {
            if (this.gridDragging) {
                this.gridDragging = false;
            }
        });

        // Relay scroll events to the MapView zoom handler
        $("#main-canvas").on("mousewheel", (event: JQueryEventObject) => {
            const originalEvent = (<any>event.originalEvent);
            this.mapView.zoom([this.currentStageMouseX, this.currentStageMouseY], -0.7 * originalEvent.deltaY);
            this.scaleIndicatorController.update();
        });
    }

    /**
     * Connects clickable UI elements to their respective event listeners.
     */
    private setupEventListeners(): void {
        // Zooming elements
        $("#zoom-plus").on("click", () => {
            this.mapView.zoom([
                this.mapView.canvasWidth / 2,
                this.mapView.canvasHeight / 2
            ], 20);
        });
        $("#zoom-minus").on("click", () => {
            this.mapView.zoom([
                this.mapView.canvasWidth / 2,
                this.mapView.canvasHeight / 2
            ], -20);
        });

        $(".export-canvas").click(() => {
            this.exportCanvasToImage();
        });

        // Menu panels
        $(".menu-header-bar .menu-collapse").on("click", (event: JQueryEventObject) => {
            const container = $(event.target).closest(".menu-container");
            if (this.appMode === AppMode.CONSTRUCTION) {
                container.children(".menu-body.construction").first().slideToggle(300);
            } else if (this.appMode === AppMode.SIMULATION) {
                container.children(".menu-body.simulation").first().slideToggle(300);
            }

        });

        // Menu close button
        $(".menu-header-bar .menu-exit").on("click", (event: JQueryEventObject) => {
            const nearestMenuContainer = $(event.target).closest(".menu-container");
            if (nearestMenuContainer.is("#node-menu")) {
                this.interactionLevel = InteractionLevel.OBJECT;
                $(".node-element-overlay").addClass("hidden");
            }
            nearestMenuContainer.addClass("hidden");
        });

        // Handler for the construction mode switch
        $("#construction-mode-switch").on("click", () => {
            this.simulationController.exitMode();
        });

        // Handler for the simulation mode switch
        $("#simulation-mode-switch").on("click", () => {
            this.simulationController.enterMode();
        });

        // Handler for the version-save button
        $("#save-version-btn").on("click", (event: JQueryEventObject) => {
            const target = $(event.target);

            target.attr("data-saved", "false");
            const lastPath = this.mapView.simulation.paths[this.mapView.simulation.paths.length - 1];
            this.api.branchFromPath(
                this.mapView.simulation.id, lastPath.id, lastPath.sections[lastPath.sections.length - 1].startTick + 1
            ).then((data: IPath) => {
                this.mapView.simulation.paths.push(data);
                this.mapView.currentDatacenter = data.sections[data.sections.length - 1].datacenter;
                target.attr("data-saved", "true");
            });
        });

        $(document).on("keydown", (event: JQueryKeyEventObject) => {
            if ($(event.target).is('input')) {
                return;
            }

            if (event.which === 83) {
                this.simulationController.enterMode();
            } else if (event.which === 67) {
                this.simulationController.exitMode();
            } else if (event.which == 32) {
                if (this.appMode === AppMode.SIMULATION) {
                    this.simulationController.timelineController.togglePlayback();
                }
            }
        });
    }

    /**
     * Handles a simple mouse click (without drag) on the canvas.
     *
     * @param stageX The x coordinate of the location in pixels on the stage
     * @param stageY The y coordinate of the location in pixels on the stage
     */
    private handleCanvasMouseClick(stageX: number, stageY: number): void {
        const gridPos = this.convertScreenCoordsToGridCoords([stageX, stageY]);

        if (this.interactionLevel === InteractionLevel.BUILDING) {
            if (this.interactionMode === InteractionMode.DEFAULT) {
                const roomIndex = Util.roomCollisionIndexOf(this.mapView.currentDatacenter.rooms, gridPos);

                if (roomIndex !== -1) {
                    this.interactionLevel = InteractionLevel.ROOM;
                    this.roomModeController.enterMode(this.mapView.currentDatacenter.rooms[roomIndex]);
                }
            } else if (this.interactionMode === InteractionMode.SELECT_ROOM) {
                if (this.mapView.roomLayer.checkHoverTileValidity(gridPos)) {
                    this.buildingModeController.addSelectedTile(this.mapView.hoverLayer.hoverTilePosition);
                } else if (Util.tileListContainsPosition(this.mapView.roomLayer.selectedTiles, gridPos)) {
                    this.buildingModeController.removeSelectedTile(this.mapView.hoverLayer.hoverTilePosition);
                }
            }
        } else if (this.interactionLevel === InteractionLevel.ROOM) {
            this.roomModeController.handleCanvasMouseClick(gridPos);
        } else if (this.interactionLevel === InteractionLevel.OBJECT) {
            if (gridPos.x !== this.mapView.grayLayer.currentObjectTile.position.x ||
                gridPos.y !== this.mapView.grayLayer.currentObjectTile.position.y) {
                this.objectModeController.goToRoomMode();
            }
        } else if (this.interactionLevel === InteractionLevel.NODE) {
            this.interactionLevel = InteractionLevel.OBJECT;
            this.nodeModeController.goToObjectMode();
        }
    }

    /**
     * Takes screen (stage) coordinates and returns the grid cell position they belong to.
     *
     * @param stagePosition The raw x and y coordinates of the wanted position
     * @returns {Array} The corresponding grid cell coordinates
     */
    private convertScreenCoordsToGridCoords(stagePosition: number[]): IGridPosition {
        const result = {x: 0, y: 0};
        result.x = Math.floor((stagePosition[0] - this.mapView.mapContainer.x) /
            (this.mapView.mapContainer.scaleX * CELL_SIZE));
        result.y = Math.floor((stagePosition[1] - this.mapView.mapContainer.y) /
            (this.mapView.mapContainer.scaleY * CELL_SIZE));
        return result;
    }

    /**
     * Adjusts the canvas size to fit the window perfectly.
     */
    private onWindowResize() {
        const parent = this.canvas.parent(".app-content");
        parent.height($(window).height() - 50);
        this.canvas.attr("width", parent.width());
        this.canvas.attr("height", parent.height());
        this.mapView.canvasWidth = parent.width();
        this.mapView.canvasHeight = parent.height();

        if (this.interactionLevel === InteractionLevel.BUILDING) {
            this.mapView.zoomOutOnDC();
        } else if (this.interactionLevel === InteractionLevel.ROOM) {
            this.mapView.zoomInOnRoom(this.roomModeController.currentRoom);
        } else {
            this.mapView.zoomInOnRoom(this.roomModeController.currentRoom, true);
        }

        this.mapView.updateScene = true;
    }

    private matchUserAuthLevel() {
        const authLevel = localStorage.getItem("simulationAuthLevel");
        if (authLevel === "VIEW") {
            $(".side-menu-container.right-middle-side, .side-menu-container.right-side").hide();
        }
    }

    private exportCanvasToImage() {
        const canvasData = (<HTMLCanvasElement>this.canvas.get(0)).toDataURL("image/png");
        const newWindow = window.open('about:blank', 'OpenDC Canvas Export');
        newWindow.document.write("<img src='" + canvasData + "' alt='Canvas Image Export'/>");
        newWindow.document.title = "OpenDC Canvas Export";
    }
}
