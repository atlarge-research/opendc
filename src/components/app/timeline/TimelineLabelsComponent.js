import React from "react";
import {convertSecondsToFormattedTime} from "../../../util/date-time";

const TimelineLabelsComponent = ({currentTick, lastSimulatedTick}) => (
    <div className="timeline-labels">
        <div className="start-time-label">{convertSecondsToFormattedTime(currentTick)}</div>
        <div className="end-time-label">{convertSecondsToFormattedTime(lastSimulatedTick)}</div>
    </div>
);

export default TimelineLabelsComponent;
