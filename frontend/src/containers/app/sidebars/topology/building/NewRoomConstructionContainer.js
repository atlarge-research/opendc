import { connect } from 'react-redux'
import {
    cancelNewRoomConstruction,
    finishNewRoomConstruction,
    startNewRoomConstruction,
} from '../../../../../actions/topology/building'
import StartNewRoomConstructionComponent
    from '../../../../../components/app/sidebars/topology/building/NewRoomConstructionComponent'

const mapStateToProps = state => {
    return {
        currentRoomInConstruction: state.construction.currentRoomInConstruction,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        onStart: () => dispatch(startNewRoomConstruction()),
        onFinish: () => dispatch(finishNewRoomConstruction()),
        onCancel: () => dispatch(cancelNewRoomConstruction()),
    }
}

const NewRoomConstructionButton = connect(mapStateToProps, mapDispatchToProps)(
    StartNewRoomConstructionComponent,
)

export default NewRoomConstructionButton
