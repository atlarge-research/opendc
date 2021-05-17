import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPencilAlt } from '@fortawesome/free-solid-svg-icons'

const NameComponent = ({ name, onEdit }) => (
    <h2>
        {name}
        <button className="btn btn-outline-secondary float-right" onClick={onEdit}>
            <FontAwesomeIcon icon={faPencilAlt} />
        </button>
    </h2>
)

export default NameComponent
