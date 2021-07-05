import PropTypes from 'prop-types'
import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus, faMinus } from '@fortawesome/free-solid-svg-icons'

const ZoomControlComponent = ({ zoomInOnCenter }) => {
    return (
        <span>
            <button
                className="btn btn-default btn-circle btn-sm mr-1"
                title="Zoom in"
                onClick={() => zoomInOnCenter(true)}
            >
                <FontAwesomeIcon icon={faPlus} />
            </button>
            <button
                className="btn btn-default btn-circle btn-sm mr-1"
                title="Zoom out"
                onClick={() => zoomInOnCenter(false)}
            >
                <FontAwesomeIcon icon={faMinus} />
            </button>
        </span>
    )
}

ZoomControlComponent.propTypes = {
    zoomInOnCenter: PropTypes.func.isRequired,
}

export default ZoomControlComponent
