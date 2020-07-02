import { connect } from 'react-redux'
import { goToTick } from '../../../actions/simulation/tick'
import TimelineControlsComponent from '../../../components/app/timeline/TimelineControlsComponent'

const mapStateToProps = state => {
    return {
        currentTick: state.currentTick,
        lastSimulatedTick: state.lastSimulatedTick,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        goToTick: tick => dispatch(goToTick(tick)),
    }
}

const TimelineControlsContainer = connect(mapStateToProps, mapDispatchToProps)(
    TimelineControlsComponent,
)

export default TimelineControlsContainer
