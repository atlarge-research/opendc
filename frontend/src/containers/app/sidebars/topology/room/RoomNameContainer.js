import { connect } from 'react-redux'
import { openEditRoomNameModal } from '../../../../../actions/modals/topology'
import RoomNameComponent from '../../../../../components/app/sidebars/topology/room/RoomNameComponent'

const mapStateToProps = state => {
    return {
        roomName: state.objects.room[state.interactionLevel.roomId].name,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        onEdit: () => dispatch(openEditRoomNameModal()),
    }
}

const RoomNameContainer = connect(mapStateToProps, mapDispatchToProps)(
    RoomNameComponent,
)

export default RoomNameContainer
