import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTrash } from '@fortawesome/free-solid-svg-icons'
import { Button } from 'reactstrap'

const DeleteRoomComponent = ({ onClick }) => (
    <Button color="danger" outline block onClick={onClick}>
        <FontAwesomeIcon icon={faTrash} className="mr-2" />
        Delete this room
    </Button>
)

export default DeleteRoomComponent
