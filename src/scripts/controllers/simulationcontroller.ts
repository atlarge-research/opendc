///<reference path="../../../typings/index.d.ts" />
///<reference path="mapcontroller.ts" />
import * as $ from "jquery";
import {MapView} from "../views/mapview";
import {MapController, InteractionLevel, AppMode} from "./mapcontroller";
import {Util} from "../util";
import {StateCache} from "./simulation/statecache";
import {ChartController} from "./simulation/chart";
import {TaskViewController} from "./simulation/taskview";
import {TimelineController} from "./simulation/timeline";


export enum ColorRepresentation {
    LOAD,
    TEMPERATURE,
    MEMORY
}


export class SimulationController {
    public mapView: MapView;
    public mapController: MapController;

    public playing: boolean;
    public currentTick: number;
    public stateCache: StateCache;
    public lastSimulatedTick: number;
    public simulation: ISimulation;
    public experiments: IExperiment[];
    public currentExperiment: IExperiment;
    public currentPath: IPath;
    public sections: ISection[];
    public currentSection: ISection;
    public experimentSelectionMode: boolean;
    public traces: ITrace[];
    public schedulers: IScheduler[];
    public sectionIndex: number;
    public chartController: ChartController;
    public timelineController: TimelineController;

    public colorRepresentation: ColorRepresentation;
    public rackToRoomMap: {[key: number]: number;};

    private taskViewController: TaskViewController;
    private tickerId: number;


    public static showOrHideSimComponents(visibility: boolean): void {
        if (visibility) {
            $("#statistics-menu").removeClass("hidden");
            $("#experiment-menu").removeClass("hidden");
            $("#tasks-menu").removeClass("hidden");
            $(".timeline-container").removeClass("hidden");
        } else {
            $("#statistics-menu").addClass("hidden");
            $("#experiment-menu").addClass("hidden");
            $("#tasks-menu").addClass("hidden");
            $(".timeline-container").addClass("hidden");
        }
    }

    constructor(mapController: MapController) {
        this.mapController = mapController;
        this.mapView = this.mapController.mapView;
        this.simulation = this.mapController.mapView.simulation;
        this.experiments = this.simulation.experiments;
        this.taskViewController = new TaskViewController(this);
        this.timelineController = new TimelineController(this);
        this.chartController = new ChartController(this);

        this.timelineController.setupListeners();
        this.experimentSelectionMode = true;
        this.sectionIndex = 0;

        this.currentTick = 1;
        this.playing = false;
        this.stateCache = new StateCache(this);
        this.colorRepresentation = ColorRepresentation.LOAD;

        this.traces = [];
        this.schedulers = [];

        this.mapController.api.getAllTraces().then((data) => {
            this.traces = data;
        });

        this.mapController.api.getAllSchedulers().then((data) => {
            this.schedulers = data;
        });
    }

    public enterMode() {
        this.experimentSelectionMode = true;

        if (this.mapController.interactionLevel === InteractionLevel.BUILDING) {
            this.mapView.roomLayer.coloringMode = true;
            this.mapView.dcObjectLayer.coloringMode = false;
        } else if (this.mapController.interactionLevel === InteractionLevel.ROOM ||
            this.mapController.interactionLevel === InteractionLevel.OBJECT) {
            this.mapView.roomLayer.coloringMode = false;
            this.mapView.dcObjectLayer.coloringMode = true;
        } else if (this.mapController.interactionLevel === InteractionLevel.NODE) {
            this.mapController.nodeModeController.goToObjectMode();
        }

        this.mapController.appMode = AppMode.SIMULATION;
        this.mapView.dcObjectLayer.detailedMode = false;
        this.mapView.gridLayer.setVisibility(false);
        this.mapView.updateScene = true;

        this.mapController.setAllMenuModes();
        SimulationController.showOrHideSimComponents(true);
        $(".mode-switch").attr("data-selected", "simulation");
        $("#save-version-btn").hide();
        $(".color-indicator").removeClass("hidden");

        $("#change-experiment-btn").click(() => {
            this.playing = false;
            this.stateCache.stopCaching();
            this.timelineController.update();
            this.showExperimentsDialog();
        });

        this.setupColorMenu();
        this.showExperimentsDialog();
    }

    private launchSimulation(): void {
        this.onSimulationSectionChange();

        this.chartController.setup();

        this.stateCache.startCaching();

        this.tickerId = setInterval(() => {
            this.simulationTick();
        }, 1000);
    }

    private onSimulationSectionChange(): void {
        this.currentSection = this.currentPath.sections[this.sectionIndex];
        this.mapView.currentDatacenter = this.currentSection.datacenter;

        // Generate a map of all rack IDs in relation to their room IDs for use in room stats
        this.rackToRoomMap = {};
        this.currentSection.datacenter.rooms.forEach((room: IRoom) => {
            room.tiles.forEach((tile: ITile) => {
                if (tile.object !== undefined && tile.objectType === "RACK") {
                    this.rackToRoomMap[tile.objectId] = room.id;
                }
            });
        });

        if (this.mapController.interactionLevel === InteractionLevel.NODE) {
            this.mapController.nodeModeController.goToObjectMode();
        }
        if (this.mapController.interactionLevel === InteractionLevel.OBJECT) {
            this.mapController.objectModeController.goToRoomMode();
        }
        if (this.mapController.interactionLevel === InteractionLevel.ROOM) {
            this.mapController.roomModeController.goToBuildingMode();
        }

        this.mapView.redrawMap();

        this.mapView.zoomOutOnDC();
    }

    public exitMode() {
        this.closeExperimentsDialog();

        this.mapController.appMode = AppMode.CONSTRUCTION;
        this.mapView.dcObjectLayer.detailedMode = true;
        this.mapView.gridLayer.setVisibility(true);
        this.mapView.redrawMap();

        this.stateCache.stopCaching();
        this.playing = false;

        this.mapController.setAllMenuModes();
        SimulationController.showOrHideSimComponents(false);

        this.setColors();
        $(".color-indicator").addClass("hidden")["popover"]("hide").off();
        $(".mode-switch").attr("data-selected", "construction");
        $("#save-version-btn").show();

        clearInterval(this.tickerId);
    }

    public update() {
        if (this.stateCache.cacheBlock) {
            return;
        }

        this.setColors();
        this.updateBuildingStats();
        this.updateRoomStats();
        this.chartController.update();
        this.taskViewController.update();
    }

    public simulationTick(): void {
        this.timelineController.update();

        if (this.currentTick > this.lastSimulatedTick) {
            this.currentTick = this.lastSimulatedTick;
            this.playing = false;
            this.timelineController.setButtonIcon();
        }

        if (this.playing) {
            this.checkCurrentSimulationSection();
            this.update();

            if (!this.stateCache.cacheBlock) {
                this.currentTick++;
            }
        }
    }

    public checkCurrentSimulationSection(): void {
        for (let i = this.sections.length - 1; i >= 0; i--) {
            if (this.currentTick >= this.sections[i].startTick) {
                if (this.sectionIndex !== i) {
                    this.sectionIndex = i;
                    this.onSimulationSectionChange();
                }
                break;
            }
        }
    }

    public transitionFromBuildingToRoom(): void {
        this.mapView.roomLayer.coloringMode = false;
        this.mapView.dcObjectLayer.coloringMode = true;

        this.setColors();
        this.updateRoomStats();
        this.chartController.update();
    }

    public transitionFromRoomToBuilding(): void {
        this.mapView.roomLayer.coloringMode = true;
        this.mapView.dcObjectLayer.coloringMode = false;

        this.setColors();
        this.updateBuildingStats();
        this.chartController.update();
    }

    public transitionFromRoomToRack(): void {
        this.setColors();
        $("#statistics-menu").addClass("hidden");
        this.chartController.update();
    }

    public transitionFromRackToRoom(): void {
        this.setColors();
        $("#statistics-menu").removeClass("hidden");
    }

    public transitionFromRackToNode(): void {
        this.chartController.update();
    }

    public transitionFromNodeToRack(): void {
    }

    private showExperimentsDialog(): void {
        $(".experiment-name-alert").hide();

        this.populateExperimentsList();
        this.populateDropdowns();

        $(".experiment-row").click((event: JQueryEventObject) => {
            if ($(event.target).hasClass("remove-experiment")) {
                return;
            }

            let row = $(event.target).closest(".experiment-row");
            this.prepareAndLaunchExperiment(this.experiments[row.index()]);
        });

        $(".experiment-list .list-body").on("click", ".remove-experiment", (event: JQueryEventObject) => {
            event.stopPropagation();
            let affectedRow = $(event.target).closest(".experiment-row");
            let index = affectedRow.index();
            let affectedExperiment = this.experiments[index];

            MapController.showConfirmDeleteDialog("experiment", () => {
                this.mapController.api.deleteExperiment(affectedExperiment.simulationId, affectedExperiment.id)
                    .then(() => {
                        this.experiments.splice(index, 1);
                        this.populateExperimentsList();
                    });
            });
        });

        $("#new-experiment-btn").click(() => {
            let nameInput = $("#new-experiment-name-input");
            if (nameInput.val() === "") {
                $(".experiment-name-alert").show();
                return;
            } else {
                $(".experiment-name-alert").hide();
            }

            let newExperiment: IExperiment = {
                id: -1,
                name: nameInput.val(),
                pathId: parseInt($("#new-experiment-path-select").val()),
                schedulerName: $("#new-experiment-scheduler-select").val(),
                traceId: parseInt($("#new-experiment-trace-select").val()),
                simulationId: this.simulation.id
            };

            this.mapController.api.addExperimentToSimulation(this.simulation.id, newExperiment)
                .then((data: IExperiment) => {
                    this.simulation.experiments.push(data);
                    this.prepareAndLaunchExperiment(data);
                });
        });

        $(".window-close").click(() => {
            this.exitMode();
        });

        $(".window-overlay").fadeIn(200);
    }

    private prepareAndLaunchExperiment(experiment: IExperiment): void {
        this.prepareSimulationData(experiment);
        this.launchSimulation();
        this.closeExperimentsDialog();
    }

    private prepareSimulationData(experiment: IExperiment): void {
        this.currentExperiment = experiment;
        this.currentPath = this.getPathById(this.currentExperiment.pathId);
        this.sections = this.currentPath.sections;
        this.sectionIndex = 0;
        this.currentTick = 1;
        this.playing = false;
        this.stateCache = new StateCache(this);
        this.colorRepresentation = ColorRepresentation.LOAD;

        this.sections.sort((a: ISection, b: ISection) => {
            return a.startTick - b.startTick;
        });

        $("#experiment-menu-name").text(experiment.name);
        $("#experiment-menu-path").text(SimulationController.getPathName(this.currentPath));
        $("#experiment-menu-scheduler").text(experiment.schedulerName);
        $("#experiment-menu-trace").text(experiment.trace.name);
    }

    private closeExperimentsDialog(): void {
        $(".window-overlay").fadeOut(200);
        $(".window-overlay input").val("");
    }

    private populateDropdowns(): void {
        let pathDropdown = $("#new-experiment-path-select");
        let traceDropdown = $("#new-experiment-trace-select");
        let schedulerDropdown = $("#new-experiment-scheduler-select");

        pathDropdown.empty();
        for (let i = 0; i < this.simulation.paths.length; i++) {
            pathDropdown.append(
                $("<option>").text(SimulationController.getPathName(this.simulation.paths[i]))
                    .val(this.simulation.paths[i].id)
            );
        }

        traceDropdown.empty();
        for (let i = 0; i < this.traces.length; i++) {
            traceDropdown.append(
                $("<option>").text(this.traces[i].name)
                    .val(this.traces[i].id)
            );
        }

        schedulerDropdown.empty();
        for (let i = 0; i < this.schedulers.length; i++) {
            schedulerDropdown.append(
                $("<option>").text(this.schedulers[i].name)
                    .val(this.schedulers[i].name)
            );
        }
    }

    /**
     * Populates the list of experiments.
     */
    private populateExperimentsList(): void {
        let table = $(".experiment-list .list-body");
        table.empty();

        console.log("EXPERIMENT", this.experiments);
        console.log("SIMULATION", this.simulation);

        if (this.experiments.length === 0) {
            $(".experiment-list").hide();
            $(".no-experiments-alert").show();
        } else {
            $(".no-experiments-alert").hide();
            this.experiments.forEach((experiment: IExperiment) => {
                table.append(
                    '<div class="experiment-row">' +
                    '  <div>' + experiment.name + '</div>' +
                    '  <div>' + this.getPathNameById(experiment.pathId) + '</div>' +
                    '  <div>' + experiment.trace.name + '</div>' +
                    '  <div>' + experiment.schedulerName + '</div>' +
                    '  <div class="remove-experiment glyphicon glyphicon-remove"></div>' +
                    '</div>'
                );
            });
        }
    }

    private getPathNameById(id: number): string {
        for (let i = 0; i < this.simulation.paths.length; i++) {
            if (id === this.simulation.paths[i].id) {
                return SimulationController.getPathName(this.simulation.paths[i]);
            }
        }
    }

    private getPathById(id: number): IPath {
        for (let i = 0; i < this.simulation.paths.length; i++) {
            if (id === this.simulation.paths[i].id) {
                return this.simulation.paths[i];
            }
        }
    }

    private static getPathName(path: IPath): string {
        if (path.name === null) {
            return "Path " + path.id;
        } else {
            return path.name;
        }
    }

    private setColors() {
        if (this.mapController.appMode === AppMode.SIMULATION) {
            if (this.mapController.interactionLevel === InteractionLevel.BUILDING) {
                this.mapView.roomLayer.intensityLevels = {};

                this.stateCache.stateList[this.currentTick].roomStates.forEach((roomState: IRoomState) => {
                    if (this.colorRepresentation === ColorRepresentation.LOAD) {
                        this.mapView.roomLayer.intensityLevels[roomState.roomId] =
                            Util.determineLoadIntensityLevel(roomState.loadFraction);
                    }
                });

                this.mapView.roomLayer.draw();
                this.mapView.dcObjectLayer.draw();
            } else if (this.mapController.interactionLevel === InteractionLevel.ROOM ||
                this.mapController.interactionLevel === InteractionLevel.OBJECT) {
                this.mapView.dcObjectLayer.intensityLevels = {};

                this.stateCache.stateList[this.currentTick].rackStates.forEach((rackState: IRackState) => {
                    if (this.colorRepresentation === ColorRepresentation.LOAD) {
                        this.mapView.dcObjectLayer.intensityLevels[rackState.rackId] =
                            Util.determineLoadIntensityLevel(rackState.loadFraction);
                    }
                });

                this.mapView.roomLayer.draw();
                this.mapView.dcObjectLayer.draw();
            }

            if (this.mapController.interactionLevel === InteractionLevel.OBJECT ||
                this.mapController.interactionLevel === InteractionLevel.NODE) {
                this.stateCache.stateList[this.currentTick].machineStates.forEach((machineState: IMachineState) => {
                    let element = $('.node-element[data-id="' + machineState.machineId + '"] .node-element-content');
                    element.css("background-color", Util.convertIntensityToColor(
                        Util.determineLoadIntensityLevel(machineState.loadFraction)
                    ));

                    // Color all transparent icon overlays, as well
                    element = $('.node-element[data-id="' + machineState.machineId + '"] .icon-overlay');
                    element.css("background-color", Util.convertIntensityToColor(
                        Util.determineLoadIntensityLevel(machineState.loadFraction)
                    ));
                });
            }
        } else {
            this.mapView.roomLayer.coloringMode = false;
            this.mapView.dcObjectLayer.coloringMode = false;

            this.mapView.roomLayer.draw();
            this.mapView.dcObjectLayer.draw();
        }
    }

    /**
     * Populates the building simulation menu with dynamic statistics concerning the state of all rooms in the building.
     */
    private updateBuildingStats(): void {
        if (this.mapController.interactionLevel !== InteractionLevel.BUILDING) {
            return;
        }

        let html;
        let container = $(".building-stats-list");

        container.children().remove("div");

        this.stateCache.stateList[this.currentTick].roomStates.forEach((roomState: IRoomState) => {
            if (this.colorRepresentation === ColorRepresentation.LOAD && roomState.room !== undefined) {
                html = '<div>' +
                    '  <h4>' + roomState.room.name + '</h4>' +
                    '  <p>Load: ' + Math.round(roomState.loadFraction * 100) + '%</p>' +
                    '</div>';
                container.append(html);
            }
        });

    }

    /**
     * Populates the room simulation menu with dynamic statistics concerning the state of all racks in the room.
     */
    private updateRoomStats(): void {
        if (this.mapController.interactionLevel !== InteractionLevel.ROOM) {
            return;
        }

        $("#room-name-field").text(this.mapController.roomModeController.currentRoom.name);
        $("#room-type-field").text(this.mapController.roomModeController.currentRoom.roomType);

        let html;
        let container = $(".room-stats-list");

        container.children().remove("div");

        this.stateCache.stateList[this.currentTick].rackStates.forEach((rackState: IRackState) => {
            if (this.rackToRoomMap[rackState.rackId] !== this.mapController.roomModeController.currentRoom.id) {
                return;
            }
            if (this.colorRepresentation === ColorRepresentation.LOAD) {
                html = '<div>' +
                    '  <h4>' + rackState.rack.name + '</h4>' +
                    '  <p>Load: ' + Math.round(rackState.loadFraction * 100) + '%</p>' +
                    '</div>';
                container.append(html);
            }
        });
    }

    private setupColorMenu(): void {
        let html =
            '<select class="form-control" title="Color Representation" id="color-representation-select">' +
            '   <option value="1" selected>Load</option>' +
            '   <option value="2">Power use</option>' +
            '</select>';

        let indicator = $(".color-indicator");
        indicator["popover"]({
            animation: true,
            content: html,
            html: true,
            placement: "top",
            title: "Colors represent:",
            trigger: "manual"
        });
        indicator.click(() => {
            //noinspection JSJQueryEfficiency  // suppressed for dynamic element insertion
            if ($("#color-representation-select").length) {
                indicator["popover"]("hide");
            } else {
                indicator["popover"]("show");

                let selectElement = $("#color-representation-select");
                selectElement.change(() => {
                    console.log(selectElement.val());
                });
            }
        });
    }
}
