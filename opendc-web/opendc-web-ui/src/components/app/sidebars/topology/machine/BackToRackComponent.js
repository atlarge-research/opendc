import PropTypes from 'prop-types'
import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleLeft } from '@fortawesome/free-solid-svg-icons'

const BackToRackComponent = ({ onClick }) => (
    <div className="btn btn-secondary btn-block" onClick={onClick}>
        <FontAwesomeIcon icon={faAngleLeft} className="mr-2" />
        Back to rack
    </div>
)

BackToRackComponent.propTypes = {
    onClick: PropTypes.func,
}

export default BackToRackComponent
