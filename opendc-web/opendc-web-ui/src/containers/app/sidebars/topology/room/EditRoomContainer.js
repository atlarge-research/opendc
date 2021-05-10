import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { finishRoomEdit, startRoomEdit } from '../../../../../actions/topology/building'
import EditRoomComponent from '../../../../../components/app/sidebars/topology/room/EditRoomComponent'

const EditRoomContainer = (props) => {
    const isEditing = useSelector((state) => state.construction.currentRoomInConstruction !== '-1')
    const isInRackConstructionMode = useSelector((state) => state.construction.inRackConstructionMode)

    const dispatch = useDispatch()
    const onEdit = () => dispatch(startRoomEdit())
    const onFinish = () => dispatch(finishRoomEdit())

    return (
        <EditRoomComponent
            {...props}
            onEdit={onEdit}
            onFinish={onFinish}
            isEditing={isEditing}
            isInRackConstructionMode={isInRackConstructionMode}
        />
    )
}

export default EditRoomContainer
