import * as $ from "jquery";
import {SimulationController} from "../simulationcontroller";
import {Util} from "../../util";


export class TaskViewController {
    private simulationController: SimulationController;


    constructor(simulationController: SimulationController) {
        this.simulationController = simulationController;
    }

    /**
     * Populates and displays the list of tasks with their current state.
     */
    public update() {
        const container = $(".task-list");
        container.children().remove(".task-element");

        this.simulationController.stateCache.stateList[this.simulationController.currentTick].taskStates
            .forEach((taskState: ITaskState) => {
                const html = this.generateTaskElementHTML(taskState);
                container.append(html);
            });
    }

    private generateTaskElementHTML(taskState: ITaskState) {
        let iconType, timeInfo;

        if (taskState.task.startTick > this.simulationController.currentTick) {
            iconType = "glyphicon-time";
            timeInfo = "Not started yet";
        } else if (taskState.task.startTick <= this.simulationController.currentTick && taskState.flopsLeft > 0) {
            iconType = "glyphicon-refresh";
            timeInfo = "Started at " + Util.convertSecondsToFormattedTime(taskState.task.startTick);
        } else if (taskState.flopsLeft === 0) {
            iconType = "glyphicon-ok";
            timeInfo = "Started at " + Util.convertSecondsToFormattedTime(taskState.task.startTick);
        }

        // Calculate progression ratio
        const progress = 1 - (taskState.flopsLeft / taskState.task.totalFlopCount);

        // Generate completion text
        const flopsCompleted = taskState.task.totalFlopCount - taskState.flopsLeft;
        const completionInfo = "Completed: " + flopsCompleted + " / " + taskState.task.totalFlopCount + " FLOPS";

        return '<div class="task-element">' +
            '  <div class="task-icon glyphicon ' + iconType + '"></div>' +
            '  <div class="task-info">' +
            '    <div class="task-time">' + timeInfo +
            '    </div>' +
            '    <div class="progress">' +
            '      <div class="progress-bar progress-bar-striped" role="progressbar" aria-valuenow="' +
            progress * 100 + '%"' +
            '        aria-valuemin="0" aria-valuemax="100" style="width: ' + progress * 100 + '%">' +
            '      </div>' +
            '    </div>' +
            '    <div class="task-flops">' + completionInfo + '</div>' +
            '  </div>' +
            '</div>';
    }
}
