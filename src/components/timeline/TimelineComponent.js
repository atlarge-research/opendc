import React from "react";
import TimelineControlsContainer from "../../containers/timeline/TimelineControlsContainer";
import TimelineLabelsContainer from "../../containers/timeline/TimelineLabelsContainer";
import "./Timeline.css";

class TimelineComponent extends React.Component {
    componentDidMount() {
        this.interval = setInterval(() => {
            if (!this.props.isPlaying) {
                return;
            }

            if (this.props.currentTick < this.props.lastSimulatedTick) {
                for (let i in this.props.sections.reverse()) {
                    if (this.props.currentTick + 1 >= this.props.sections[i].startTick) {
                        if (this.props.currentDatacenterId !== this.props.sections[i].datacenterId) {
                            this.props.setCurrentDatacenter(this.props.sections[i].datacenterId);
                        }
                        break;
                    }
                }
                this.props.incrementTick();
            }
        }, 1000);
    }

    componentWillUnmount() {
        clearInterval(this.interval);
    }

    render() {
        return (
            <div className="timeline-bar">
                <div className="timeline-container">
                    <TimelineLabelsContainer/>
                    <TimelineControlsContainer/>
                </div>
            </div>
        );
    }
}

export default TimelineComponent;
