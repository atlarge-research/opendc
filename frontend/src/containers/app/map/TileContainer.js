import { connect } from 'react-redux'
import { goFromRoomToRack } from '../../../actions/interaction-level'
import TileGroup from '../../../components/app/map/groups/TileGroup'

const mapStateToProps = (state, ownProps) => {
    const tile = state.objects.tile[ownProps.tileId]

    return {
        interactionLevel: state.interactionLevel,
        tile,
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
