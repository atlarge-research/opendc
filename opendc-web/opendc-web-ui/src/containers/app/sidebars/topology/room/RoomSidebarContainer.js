import { connect } from 'react-redux'
import RoomSidebarComponent from '../../../../../components/app/sidebars/topology/room/RoomSidebarComponent'

const mapStateToProps = (state) => {
    return {
        roomId: state.interactionLevel.roomId,
    }
}

const RoomSidebarContainer = connect(mapStateToProps)(RoomSidebarComponent)

export default RoomSidebarContainer
