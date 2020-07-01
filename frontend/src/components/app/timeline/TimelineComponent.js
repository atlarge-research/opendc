import React from 'react'
import TimelineControlsContainer from '../../../containers/app/timeline/TimelineControlsContainer'
import TimelineLabelsContainer from '../../../containers/app/timeline/TimelineLabelsContainer'
import './Timeline.css'

class TimelineComponent extends React.Component {
    componentDidMount() {
        this.interval = setInterval(() => {
            if (!this.props.isPlaying) {
                return
            }

            if (this.props.currentTick < this.props.lastSimulatedTick) {
                this.props.incrementTick()
            } else {
                this.props.pauseSimulation()
            }
        }, 1000)
    }

    componentWillUnmount() {
        clearInterval(this.interval)
    }

    render() {
        return (
            <div className="timeline-bar">
                <div className="timeline-container">
                    <TimelineLabelsContainer/>
                    <TimelineControlsContainer/>
                </div>
            </div>
        )
    }
}

export default TimelineComponent
