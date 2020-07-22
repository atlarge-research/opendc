import React from 'react'
import { connect } from 'react-redux'
import { closeEditRackNameModal } from '../../actions/modals/topology'
import { editRackName } from '../../actions/topology/rack'
import TextInputModal from '../../components/modals/TextInputModal'

const EditRackNameModalComponent = ({ visible, previousName, callback }) => (
    <TextInputModal
        title="Edit rack name"
        label="Rack name"
        show={visible}
        initialValue={previousName}
        callback={callback}
    />
)

const mapStateToProps = (state) => {
    return {
        visible: state.modals.editRackNameModalVisible,
        previousName:
            state.interactionLevel.mode === 'RACK'
                ? state.objects.rack[state.objects.tile[state.interactionLevel.tileId].rackId].name
                : '',
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        callback: (name) => {
            if (name) {
                dispatch(editRackName(name))
            }
            dispatch(closeEditRackNameModal())
        },
    }
}

const EditRackNameModal = connect(mapStateToProps, mapDispatchToProps)(EditRackNameModalComponent)

export default EditRackNameModal
