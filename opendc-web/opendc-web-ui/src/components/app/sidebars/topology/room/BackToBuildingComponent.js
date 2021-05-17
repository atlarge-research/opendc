import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleLeft } from '@fortawesome/free-solid-svg-icons'

const BackToBuildingComponent = ({ onClick }) => (
    <div className="btn btn-secondary btn-block mb-2" onClick={onClick}>
        <FontAwesomeIcon icon={faAngleLeft} className="mr-2" />
        Back to building
    </div>
)

export default BackToBuildingComponent
