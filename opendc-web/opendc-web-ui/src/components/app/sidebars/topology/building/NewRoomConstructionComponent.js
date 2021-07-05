import PropTypes from 'prop-types'
import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus, faCheck, faTimes } from '@fortawesome/free-solid-svg-icons'
import { Button } from 'reactstrap'

const NewRoomConstructionComponent = ({ onStart, onFinish, onCancel, currentRoomInConstruction }) => {
    if (currentRoomInConstruction === '-1') {
        return (
            <div className="btn btn-outline-primary btn-block" onClick={onStart}>
                <FontAwesomeIcon icon={faPlus} className="mr-2" />
                Construct a new room
            </div>
        )
    }
    return (
        <div>
            <Button color="primary" block onClick={onFinish}>
                <FontAwesomeIcon icon={faCheck} className="mr-2" />
                Finalize new room
            </Button>
            <Button color="default" block onClick={onCancel}>
                <FontAwesomeIcon icon={faTimes} className="mr-2" />
                Cancel construction
            </Button>
        </div>
    )
}

NewRoomConstructionComponent.propTypes = {
    onStart: PropTypes.func,
    onFinish: PropTypes.func,
    onCancel: PropTypes.func,
    currentRoomInConstruction: PropTypes.string,
}

export default NewRoomConstructionComponent
