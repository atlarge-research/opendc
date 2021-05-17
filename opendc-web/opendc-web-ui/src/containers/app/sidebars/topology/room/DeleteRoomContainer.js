import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { Button } from 'reactstrap'
import ConfirmationModal from '../../../../../components/modals/ConfirmationModal'
import { deleteRoom } from '../../../../../redux/actions/topology/room'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTrash } from '@fortawesome/free-solid-svg-icons'

const DeleteRoomContainer = () => {
    const dispatch = useDispatch()
    const [isVisible, setVisible] = useState(false)
    const callback = (isConfirmed) => {
        if (isConfirmed) {
            dispatch(deleteRoom())
        }
        setVisible(false)
    }
    return (
        <>
            <Button color="danger" outline block onClick={() => setVisible(true)}>
                <FontAwesomeIcon icon={faTrash} className="mr-2" />
                Delete this room
            </Button>
            <ConfirmationModal
                title="Delete this room"
                message="Are you sure you want to delete this room?"
                show={isVisible}
                callback={callback}
            />
        </>
    )
}

export default DeleteRoomContainer
