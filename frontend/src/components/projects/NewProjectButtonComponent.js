import PropTypes from 'prop-types'
import React from 'react'

const NewProjectButtonComponent = ({ onClick }) => (
    <div className="bottom-btn-container">
        <div className="btn btn-primary float-right" onClick={onClick}>
            <span className="fa fa-plus mr-2"/>
            New Project
        </div>
    </div>
)

NewProjectButtonComponent.propTypes = {
    onClick: PropTypes.func.isRequired,
}

export default NewProjectButtonComponent
