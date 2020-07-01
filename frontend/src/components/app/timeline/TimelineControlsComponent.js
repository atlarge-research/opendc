import React from 'react'
import PlayButtonContainer from '../../../containers/app/timeline/PlayButtonContainer'
import { convertTickToPercentage } from '../../../util/timeline'

class TimelineControlsComponent extends React.Component {
    onTimelineClick(e) {
        const percentage = e.nativeEvent.offsetX / this.timeline.clientWidth
        const tick = Math.floor(percentage * (this.props.lastSimulatedTick + 1))
        this.props.goToTick(tick)
    }

    render() {
        return (
            <div className="timeline-controls">
                <PlayButtonContainer/>
                <div
                    className="timeline"
                    ref={timeline => (this.timeline = timeline)}
                    onClick={this.onTimelineClick.bind(this)}
                >
                    <div
                        className="time-marker"
                        style={{
                            left: convertTickToPercentage(
                                this.props.currentTick,
                                this.props.lastSimulatedTick,
                            ),
                        }}
                    />
                    {this.props.sectionTicks.map(sectionTick => (
                        <div
                            key={sectionTick}
                            className="section-marker"
                            style={{
                                left: convertTickToPercentage(
                                    sectionTick,
                                    this.props.lastSimulatedTick,
                                ),
                            }}
                            title="Topology changes at this tick"
                        />
                    ))}
                </div>
            </div>
        )
    }
}

export default TimelineControlsComponent
