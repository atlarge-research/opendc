import PropTypes from 'prop-types';
import React from 'react';
import './NewProjectButton.css';

const NewProjectButton = ({onClick}) => (
    <div className="new-project-btn" onClick={onClick}>
        <span className="fa fa-plus"/>
        New Project
    </div>
);

NewProjectButton.propTypes = {
    onClick: PropTypes.func.isRequired,
};

export default NewProjectButton;
