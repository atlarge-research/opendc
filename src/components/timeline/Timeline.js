import React from "react";
import TimelineControlsContainer from "../../containers/timeline/TimelineControlsContainer";
import TimelineLabelsContainer from "../../containers/timeline/TimelineLabelsContainer";
import "./Timeline.css";

const Timeline = ({currentTick, lastSimulatedTick}) => (
    <div className="timeline-bar">
        <div className="timeline-container">
            <TimelineLabelsContainer/>
            <TimelineControlsContainer/>
        </div>
    </div>
);

export default Timeline;
