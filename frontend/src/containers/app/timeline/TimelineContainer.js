import { connect } from 'react-redux'
import { pauseSimulation } from '../../../actions/simulation/playback'
import { incrementTick } from '../../../actions/simulation/tick'
import TimelineComponent from '../../../components/app/timeline/TimelineComponent'

const mapStateToProps = state => {
    return {
        isPlaying: state.isPlaying,
        currentTick: state.currentTick,
        lastSimulatedTick: state.lastSimulatedTick,
        currentTopologyId: state.currentTopologyId,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        incrementTick: () => dispatch(incrementTick()),
        pauseSimulation: () => dispatch(pauseSimulation()),
    }
}

const TimelineContainer = connect(mapStateToProps, mapDispatchToProps)(
    TimelineComponent,
)

export default TimelineContainer
