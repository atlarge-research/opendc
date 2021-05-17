import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTimes, faPlus } from '@fortawesome/free-solid-svg-icons'
import { Button } from 'reactstrap'

const RackConstructionComponent = ({ onStart, onStop, inRackConstructionMode, isEditingRoom }) => {
    if (inRackConstructionMode) {
        return (
            <Button color="primary" block onClick={onStop}>
                <FontAwesomeIcon icon={faTimes} className="mr-2" />
                Stop rack construction
            </Button>
        )
    }

    return (
        <Button
            color="primary"
            outline
            block
            disabled={isEditingRoom}
            onClick={() => (isEditingRoom ? undefined : onStart())}
        >
            <FontAwesomeIcon icon={faPlus} className="mr-2" />
            Start rack construction
        </Button>
    )
}

export default RackConstructionComponent
