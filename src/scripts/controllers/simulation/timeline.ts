import {SimulationController} from "../simulationcontroller";
import {Util} from "../../util";
import * as $ from "jquery";


export class TimelineController {
    private simulationController: SimulationController;
    private startLabel: JQuery;
    private endLabel: JQuery;
    private playButton: JQuery;
    private loadingIcon: JQuery;
    private cacheSection: JQuery;
    private timeMarker: JQuery;
    private timeline: JQuery;
    private timeUnitFraction: number;
    private timeMarkerWidth: number;
    private timelineWidth: number;


    constructor(simulationController: SimulationController) {
        this.simulationController = simulationController;
        this.startLabel = $(".timeline-container .labels .start-time-label");
        this.endLabel = $(".timeline-container .labels .end-time-label");
        this.playButton = $(".timeline-container .play-btn");
        this.loadingIcon = this.playButton.find("img");
        this.cacheSection = $(".timeline-container .timeline .cache-section");
        this.timeMarker = $(".timeline-container .timeline .time-marker");
        this.timeline = $(".timeline-container .timeline");
        this.timeMarkerWidth = this.timeMarker.width();
        this.timelineWidth = this.timeline.width();
    }

    public togglePlayback(): void {
        if (this.simulationController.stateCache.cacheBlock) {
            this.simulationController.playing = false;
            return;
        }
        this.simulationController.playing = !this.simulationController.playing;
        this.setButtonIcon();
    }

    public setupListeners(): void {
        this.playButton.on("click", () => {
            this.togglePlayback();
        });

        $(".timeline-container .timeline").on("click", (event: JQueryEventObject) => {
            const parentOffset = $(event.target).closest(".timeline").offset();
            const clickX = event.pageX - parentOffset.left;

            let newTick = Math.round(clickX / (this.timelineWidth * this.timeUnitFraction));

            if (newTick > this.simulationController.stateCache.lastCachedTick) {
                newTick = this.simulationController.stateCache.lastCachedTick;
            }
            this.simulationController.currentTick = newTick;
            this.simulationController.checkCurrentSimulationSection();
            this.simulationController.update();
        });
    }

    public setButtonIcon(): void {
        if (this.simulationController.playing && !this.playButton.hasClass("glyphicon-pause")) {
            this.playButton.removeClass("glyphicon-play").addClass("glyphicon-pause");
        } else if (!this.simulationController.playing && !this.playButton.hasClass("glyphicon-play")) {
            this.playButton.removeClass("glyphicon-pause").addClass("glyphicon-play");
        }
    }

    public update(): void {
        this.timeUnitFraction = 1 / (this.simulationController.lastSimulatedTick + 1);
        this.timelineWidth = $(".timeline-container .timeline").width();

        this.updateTimeLabels();

        this.cacheSection.css("width", this.calculateTickPosition(this.simulationController.stateCache.lastCachedTick));
        this.timeMarker.css("left", this.calculateTickPosition(this.simulationController.currentTick));

        this.updateTaskIndicators();
        this.updateSectionMarkers();

        if (this.simulationController.stateCache.cacheBlock) {
            this.playButton.removeClass("glyphicon-pause").removeClass("glyphicon-play");
            this.loadingIcon.show();
        } else {
            this.loadingIcon.hide();
            this.setButtonIcon();
        }
    }

    private updateTimeLabels(): void {
        this.startLabel.text(Util.convertSecondsToFormattedTime(this.simulationController.currentTick));
        this.endLabel.text(Util.convertSecondsToFormattedTime(this.simulationController.lastSimulatedTick));
    }

    private updateSectionMarkers(): void {
        $(".section-marker").remove();

        this.simulationController.sections.forEach((simulationSection: ISection) => {
            if (simulationSection.startTick === 0) {
                return;
            }

            this.timeline.append(
                $('<div class="section-marker">')
                    .css("left", this.calculateTickPosition(simulationSection.startTick))
            );
        });
    }

    private updateTaskIndicators(): void {
        $(".task-indicator").remove();

        const tickStateTypes = {
            "queueEntryTick": "task-queued",
            "startTick": "task-started",
            "finishedTick": "task-finished"
        };

        if (this.simulationController.stateCache.lastCachedTick === -1) {
            return;
        }

        const indicatorCountList = new Array(this.simulationController.stateCache.lastCachedTick);
        let indicator;
        this.simulationController.currentExperiment.trace.tasks.forEach((task: ITask) => {
            for (let tickStateType in tickStateTypes) {
                if (!tickStateTypes.hasOwnProperty(tickStateType)) {
                    continue;
                }

                if (task[tickStateType] !== undefined &&
                    task[tickStateType] <= this.simulationController.stateCache.lastCachedTick) {

                    let bottomOffset;
                    if (indicatorCountList[task[tickStateType]] === undefined) {
                        indicatorCountList[task[tickStateType]] = 1;
                        bottomOffset = 0;
                    } else {
                        bottomOffset = indicatorCountList[task[tickStateType]] * 10;
                        indicatorCountList[task[tickStateType]]++;
                    }
                    indicator = $('<div class="task-indicator ' + tickStateTypes[tickStateType] + '">')
                        .css("left", this.calculateTickPosition(task[tickStateType]))
                        .css("bottom", bottomOffset);
                    this.timeline.append(indicator);
                }
            }
        });
    }

    private calculateTickPosition(tick: number): string {
        let correction = 0;
        if (this.timeUnitFraction * this.timelineWidth > this.timeMarkerWidth) {
            correction = (this.timeUnitFraction * this.timelineWidth - this.timeMarkerWidth) *
                ((tick - 1) / this.simulationController.lastSimulatedTick);
        }

        return (100 * (this.timeUnitFraction * (tick - 1) + correction / this.timelineWidth)) + "%";
    }
}
