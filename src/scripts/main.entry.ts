///<reference path="../../typings/index.d.ts" />
///<reference path="./views/mapview.ts" />
import * as $ from "jquery";
import {MapView} from "./views/mapview";
import {APIController} from "./controllers/connection/api";
window["$"] = $;
require("jquery-mousewheel");
window["jQuery"] = $;

require("./user-authentication");


$(document).ready(function () {
    new Display();  //tslint:disable-line:no-unused-expression
});


/**
 * Class responsible for launching the main view.
 */
class Display {
    private stage: createjs.Stage;
    private view: MapView;


    /**
     * Adjusts the canvas size to fit the window's initial dimensions (full expansion).
     */
    private static fitCanvasSize() {
        const canvas = $("#main-canvas");
        const parent = canvas.parent();
        parent.height($(window).height() - 50);
        canvas.attr("width", parent.width());
        canvas.attr("height", parent.height());
    }

    constructor() {
        // Check whether project has been selected before going to the app page
        if (localStorage.getItem("simulationId") === null) {
            window.location.replace("projects");
            return;
        }

        Display.fitCanvasSize();
        this.stage = new createjs.Stage("main-canvas");

        new APIController((api: APIController) => {
            api.getSimulation(parseInt(localStorage.getItem("simulationId")))
                .then((simulationData: ISimulation) => {
                    if (simulationData.name !== "") {
                        document.title = simulationData.name + " | OpenDC";
                    }

                    api.getPathsBySimulation(simulationData.id)
                        .then((pathData: IPath[]) => {
                            simulationData.paths = pathData;
                        }).then(() => {
                        return api.getExperimentsBySimulation(simulationData.id);
                    }).then((experimentData: IExperiment[]) => {
                        $(".loading-overlay").hide();

                        simulationData.experiments = experimentData;
                        this.view = new MapView(simulationData, this.stage);
                    });
                });
        });

    }
}
