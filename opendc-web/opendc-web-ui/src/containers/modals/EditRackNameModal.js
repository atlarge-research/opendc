import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { closeEditRackNameModal } from '../../actions/modals/topology'
import { editRackName } from '../../actions/topology/rack'
import TextInputModal from '../../components/modals/TextInputModal'

const EditRackNameModal = (props) => {
    const { visible, previousName } = useSelector((state) => {
        return {
            visible: state.modals.editRackNameModalVisible,
            previousName:
                state.interactionLevel.mode === 'RACK'
                    ? state.objects.rack[state.objects.tile[state.interactionLevel.tileId].rackId].name
                    : '',
        }
    })

    const dispatch = useDispatch()
    const callback = (name) => {
        if (name) {
            dispatch(editRackName(name))
        }
        dispatch(closeEditRackNameModal())
    }
    return (
        <TextInputModal
            title="Edit rack name"
            label="Rack name"
            show={visible}
            initialValue={previousName}
            callback={callback}
            {...props}
        />
    )
}

export default EditRackNameModal
