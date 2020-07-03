import { connect } from 'react-redux'
import { goFromRoomToRack } from '../../../actions/interaction-level'
import TileGroup from '../../../components/app/map/groups/TileGroup'
import { getStateLoad } from '../../../util/simulation-load'

const mapStateToProps = (state, ownProps) => {
    const tile = state.objects.tile[ownProps.tileId]
    const inSimulation = state.currentExperimentId !== '-1'

    let roomLoad = undefined
    if (inSimulation) {
        if (
            state.states.room[state.currentTick] &&
            state.states.room[state.currentTick][tile.roomId]
        ) {
            roomLoad = getStateLoad(
                state.loadMetric,
                state.states.room[state.currentTick][tile.roomId],
            )
        }
    }

    return {
        interactionLevel: state.interactionLevel,
        tile,
        inSimulation,
        roomLoad,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        onClick: tile => {
            if (tile.rackId) {
                dispatch(goFromRoomToRack(tile._id))
            }
        },
    }
}

const TileContainer = connect(mapStateToProps, mapDispatchToProps)(TileGroup)

export default TileContainer
