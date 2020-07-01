import { connect } from 'react-redux'
import RackGroup from '../../../components/app/map/groups/RackGroup'
import { getStateLoad } from '../../../util/simulation-load'

const mapStateToProps = (state, ownProps) => {
    const inSimulation = state.currentExperimentId !== -1

    let rackLoad = undefined
    if (inSimulation) {
        if (
            state.states.rack[state.currentTick] &&
            state.states.rack[state.currentTick][ownProps.tile.objectId]
        ) {
            rackLoad = getStateLoad(
                state.loadMetric,
                state.states.rack[state.currentTick][ownProps.tile.objectId],
            )
        }
    }

    return {
        interactionLevel: state.interactionLevel,
        inSimulation,
        rackLoad,
    }
}

const RackContainer = connect(mapStateToProps)(RackGroup)

export default RackContainer
