import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { finishRoomEdit, startRoomEdit } from '../../../../../redux/actions/topology/building'
import { Button } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck, faPencilAlt } from '@fortawesome/free-solid-svg-icons'

const EditRoomContainer = () => {
    const isEditing = useSelector((state) => state.construction.currentRoomInConstruction !== '-1')
    const isInRackConstructionMode = useSelector((state) => state.construction.inRackConstructionMode)

    const dispatch = useDispatch()
    const onEdit = () => dispatch(startRoomEdit())
    const onFinish = () => dispatch(finishRoomEdit())

    return isEditing ? (
        <Button color="info" outline block onClick={onFinish}>
            <FontAwesomeIcon icon={faCheck} className="mr-2" />
            Finish editing room
        </Button>
    ) : (
        <Button
            color="info"
            outline
            block
            disabled={isInRackConstructionMode}
            onClick={() => (isInRackConstructionMode ? undefined : onEdit())}
        >
            <FontAwesomeIcon icon={faPencilAlt} className="mr-2" />
            Edit the tiles of this room
        </Button>
    )
}

export default EditRoomContainer
