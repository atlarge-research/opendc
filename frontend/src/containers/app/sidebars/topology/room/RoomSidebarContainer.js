import { connect } from 'react-redux'
import RoomSidebarComponent from '../../../../../components/app/sidebars/topology/room/RoomSidebarComponent'

const mapStateToProps = state => {
    return {
        inSimulation: state.currentExperimentId !== '-1',
        roomId: state.interactionLevel.roomId,
    }
}

const RoomSidebarContainer = connect(mapStateToProps)(RoomSidebarComponent)

export default RoomSidebarContainer
