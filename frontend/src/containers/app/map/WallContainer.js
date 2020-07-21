import { connect } from 'react-redux'
import WallGroup from '../../../components/app/map/groups/WallGroup'

const mapStateToProps = (state, ownProps) => {
    return {
        tiles: state.objects.room[ownProps.roomId].tileIds.map((tileId) => state.objects.tile[tileId]),
    }
}

const WallContainer = connect(mapStateToProps)(WallGroup)

export default WallContainer
