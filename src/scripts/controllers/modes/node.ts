import {MapController, AppMode, InteractionLevel} from "../mapcontroller";
import {MapView} from "../../views/mapview";
import * as $ from "jquery";


/**
 * Class responsible for rendering node mode and handling UI interactions within it.
 */
export class NodeModeController {
    public currentMachine: IMachine;

    private mapController: MapController;
    private mapView: MapView;


    constructor(mapController: MapController) {
        this.mapController = mapController;
        this.mapView = this.mapController.mapView;

        this.loadAddDropdowns();
    }

    /**
     * Moves the UI model into node mode.
     *
     * @param machine The machine that was selected in rack mode
     */
    public enterMode(machine: IMachine): void {
        this.currentMachine = machine;
        this.populateUnitLists();
        $("#node-menu").removeClass("hidden");

        if (this.mapController.appMode === AppMode.SIMULATION) {
            this.mapController.simulationController.transitionFromRackToNode();
        }
    }

    /**
     * Performs cleanup and closing actions before allowing transferal to rack mode.
     */
    public goToObjectMode(): void {
        $("#node-menu").addClass("hidden");
        $(".node-element-overlay").addClass("hidden");
        this.currentMachine = undefined;
        this.mapController.interactionLevel = InteractionLevel.OBJECT;

        if (this.mapController.appMode === AppMode.SIMULATION) {
            this.mapController.simulationController.transitionFromNodeToRack();
        }
    }

    /**
     * Connects all DOM event listeners to their respective element targets.
     */
    public setupEventListeners(): void {
        const nodeMenu = $("#node-menu");

        nodeMenu.find(".panel-group").on("click", ".remove-unit", (event: JQueryEventObject) => {
            MapController.showConfirmDeleteDialog("unit", () => {
                const index = $(event.target).closest(".panel").index();

                if (index === -1) {
                    return;
                }

                const closestTabPane = $(event.target).closest(".panel-group");

                let objectList, idList;
                if (closestTabPane.is("#cpu-accordion")) {
                    objectList = this.currentMachine.cpus;
                    idList = this.currentMachine.cpuIds;
                } else if (closestTabPane.is("#gpu-accordion")) {
                    objectList = this.currentMachine.gpus;
                    idList = this.currentMachine.gpuIds;
                } else if (closestTabPane.is("#memory-accordion")) {
                    objectList = this.currentMachine.memories;
                    idList = this.currentMachine.memoryIds;
                } else if (closestTabPane.is("#storage-accordion")) {
                    objectList = this.currentMachine.storages;
                    idList = this.currentMachine.storageIds;
                }

                idList.splice(idList.indexOf(objectList[index]).id, 1);
                objectList.splice(index, 1);

                this.mapController.api.updateMachine(this.mapView.simulation.id,
                    this.mapView.currentDatacenter.id, this.mapController.roomModeController.currentRoom.id,
                    this.mapController.objectModeController.currentObjectTile.id, this.currentMachine).then(
                    () => {
                        this.populateUnitLists();
                        this.mapController.objectModeController.updateNodeComponentOverlays();
                    });
            });
        });

        nodeMenu.find(".add-unit").on("click", (event: JQueryEventObject) => {
            const dropdown = $(event.target).closest(".input-group-btn").siblings("select").first();

            const closestTabPane = $(event.target).closest(".input-group").siblings(".panel-group").first();
            let objectList, idList, typePlural;
            if (closestTabPane.is("#cpu-accordion")) {
                objectList = this.currentMachine.cpus;
                idList = this.currentMachine.cpuIds;
                typePlural = "cpus";
            } else if (closestTabPane.is("#gpu-accordion")) {
                objectList = this.currentMachine.gpus;
                idList = this.currentMachine.gpuIds;
                typePlural = "gpus";
            } else if (closestTabPane.is("#memory-accordion")) {
                objectList = this.currentMachine.memories;
                idList = this.currentMachine.memoryIds;
                typePlural = "memories";
            } else if (closestTabPane.is("#storage-accordion")) {
                objectList = this.currentMachine.storages;
                idList = this.currentMachine.storageIds;
                typePlural = "storages";
            }

            if (idList.length + 1 > 4) {
                this.mapController.showInfoBalloon("Machine has only 4 slots", "warning");
                return;
            }

            const id = parseInt(dropdown.val());
            idList.push(id);
            this.mapController.api.getSpecificationOfType(typePlural, id).then((spec: INodeUnit) => {
                objectList.push(spec);

                this.mapController.api.updateMachine(this.mapView.simulation.id,
                    this.mapView.currentDatacenter.id, this.mapController.roomModeController.currentRoom.id,
                    this.mapController.objectModeController.currentObjectTile.id, this.currentMachine).then(
                    () => {
                        this.populateUnitLists();
                        this.mapController.objectModeController.updateNodeComponentOverlays();
                    });
            });
        });
    }

    /**
     * Populates the "add" dropdowns with all available unit options.
     */
    private loadAddDropdowns(): void {
        const unitTypes = [
            "cpus", "gpus", "memories", "storages"
        ];
        const dropdowns = [
            $("#add-cpu-form").find("select"),
            $("#add-gpu-form").find("select"),
            $("#add-memory-form").find("select"),
            $("#add-storage-form").find("select"),
        ];

        unitTypes.forEach((type: string, index: number) => {
            this.mapController.api.getAllSpecificationsOfType(type).then((data: any) => {
                data.forEach((option: INodeUnit) => {
                    dropdowns[index].append($("<option>").val(option.id).text(option.manufacturer + " " + option.family +
                        " " + option.model + " (" + option.generation + ")"));
                });
            });
        });
    }

    /**
     * Generates and inserts dynamically HTML code concerning all units of a machine.
     */
    private populateUnitLists(): void {
        // Contains the skeleton of a unit element and inserts the given data into it
        const generatePanel = (type: string, index: number, list: any, specSection: string): string => {
            return '<div class="panel panel-default">' +
                '  <div class="panel-heading">' +
                '    <h4 class="panel-title">' +
                '      <a class="glyphicon glyphicon-remove remove-unit" href="javascript:void(0)"></a>' +
                '      <a class="accordion-toggle collapsed" data-toggle="collapse" data-parent="#' + type + '-accordion"' +
                '         href="#' + type + '-' + index + '">' +
                list[index].manufacturer + ' ' + list[index].family + ' ' + list[index].model +
                '      </a>' +
                '    </h4>' +
                '  </div>' +
                '  <div id="' + type + '-' + index + '" class="panel-collapse collapse">' +
                '    <table class="spec-table">' +
                '      <tbody>' +
                specSection +
                '      </tbody>' +
                '    </table>' +
                '  </div>' +
                '</div>';
        };

        // Generates the structure of the specification list of a processing unit
        const generateProcessingUnitHtml = (element: IProcessingUnit) => {
            return '        <tr>' +
                '          <td class="glyphicon glyphicon-tasks"></td>' +
                '          <td>Number of Cores</td>' +
                '          <td>' + element.numberOfCores + '</td>' +
                '        </tr>' +
                '        <tr>' +
                '          <td class="glyphicon glyphicon-dashboard"></td>' +
                '          <td>Clockspeed (MHz)</td>' +
                '          <td>' + element.clockRateMhz + '</td>' +
                '        </tr>' +
                '        <tr>' +
                '          <td class="glyphicon glyphicon-flash"></td>' +
                '          <td>Energy Consumption (W)</td>' +
                '          <td>' + element.energyConsumptionW + '</td>' +
                '        </tr>' +
                '        <tr>' +
                '          <td class="glyphicon glyphicon-alert"></td>' +
                '          <td>Failure Rate (%)</td>' +
                '          <td>' + element.failureModel.rate + '</td>' +
                '        </tr>';
        };

        // Generates the structure of the spec list of a storage unit
        const generateStorageUnitHtml = (element: IStorageUnit) => {
            return '        <tr>' +
                '          <td class="glyphicon glyphicon-floppy-disk"></td>' +
                '          <td>Size (Mb)</td>' +
                '          <td>' + element.sizeMb + '</td>' +
                '        </tr>' +
                '        <tr>' +
                '          <td class="glyphicon glyphicon-dashboard"></td>' +
                '          <td>Speed (Mb/s)</td>' +
                '          <td>' + element.speedMbPerS + '</td>' +
                '        </tr>' +
                '        <tr>' +
                '          <td class="glyphicon glyphicon-flash"></td>' +
                '          <td>Energy Consumption (W)</td>' +
                '          <td>' + element.energyConsumptionW + '</td>' +
                '        </tr>' +
                '        <tr>' +
                '          <td class="glyphicon glyphicon-alert"></td>' +
                '          <td>Failure Rate (%)</td>' +
                '          <td>' + element.failureModel.rate + '</td>' +
                '        </tr>';
        };

        // Inserts a "No units" message into the container of the given unit type
        const addNoUnitsMessage = (type: string) => {
            $("#" + type + "-accordion").append("<p>There are currently no units present here. " +
                "<em>Add some with the dropdown below!</em></p>");
        };

        let container = $("#cpu-accordion");
        container.children().remove(".panel");
        container.children().remove("p");

        if (this.currentMachine.cpus.length === 0) {
            addNoUnitsMessage("cpu");
        } else {
            this.currentMachine.cpus.forEach((element: ICPU, i: number) => {
                const specSection = generateProcessingUnitHtml(element);
                const content = generatePanel("cpu", i, this.currentMachine.cpus, specSection);
                container.append(content);
            });
        }

        container = $("#gpu-accordion");
        container.children().remove(".panel");
        container.children().remove("p");
        if (this.currentMachine.gpus.length === 0) {
            addNoUnitsMessage("gpu");
        } else {
            this.currentMachine.gpus.forEach((element: IGPU, i: number) => {
                const specSection = generateProcessingUnitHtml(element);
                const content = generatePanel("gpu", i, this.currentMachine.gpus, specSection);
                container.append(content);
            });
        }

        container = $("#memory-accordion");
        container.children().remove(".panel");
        container.children().remove("p");
        if (this.currentMachine.memories.length === 0) {
            addNoUnitsMessage("memory");
        } else {
            this.currentMachine.memories.forEach((element: IMemory, i: number) => {
                const specSection = generateStorageUnitHtml(element);
                const content = generatePanel("memory", i, this.currentMachine.memories, specSection);
                container.append(content);
            });
        }

        container = $("#storage-accordion");
        container.children().remove(".panel");
        container.children().remove("p");
        if (this.currentMachine.storages.length === 0) {
            addNoUnitsMessage("storage");
        } else {
            this.currentMachine.storages.forEach((element: IMemory, i: number) => {
                const specSection = generateStorageUnitHtml(element);
                const content = generatePanel("storage", i, this.currentMachine.storages, specSection);
                container.append(content);
            });
        }
    }
}
