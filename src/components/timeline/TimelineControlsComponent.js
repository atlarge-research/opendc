import React from "react";
import PlayButtonContainer from "../../containers/timeline/PlayButtonContainer";

function getXPercentage(tick, maxTick) {
    if (maxTick === 0) {
        return "0%";
    } else if (tick > maxTick) {
        return "100%";
    }

    return (tick / maxTick) + "%";
}

const TimelineControlsComponent = ({currentTick, lastSimulatedTick, sectionTicks}) => (
    <div className="timeline-controls">
        <PlayButtonContainer/>
        <div className="timeline">
            <div
                className="time-marker"
                style={{left: getXPercentage(currentTick, lastSimulatedTick)}}
            />
            {sectionTicks.map(sectionTick => (
                <div
                    key={sectionTick}
                    className="section-marker"
                    style={{left: getXPercentage(sectionTick, lastSimulatedTick)}}
                />
            ))}
        </div>
    </div>
);

export default TimelineControlsComponent;
