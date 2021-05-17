import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleLeft } from '@fortawesome/free-solid-svg-icons'
import { Button } from 'reactstrap'

const BackToRoomComponent = ({ onClick }) => (
    <Button color="secondary" block className="mb-2" onClick={onClick}>
        <FontAwesomeIcon icon={faAngleLeft} className="mr-2" />
        Back to room
    </Button>
)

export default BackToRoomComponent
