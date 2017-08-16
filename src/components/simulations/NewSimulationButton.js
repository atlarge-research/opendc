import PropTypes from 'prop-types';
import React from 'react';
import './NewSimulationButton.css';

const NewSimulationButton = ({onClick}) => (
    <div className="new-simulation-btn" onClick={onClick}>
        <span className="fa fa-plus"/>
        New Simulation
    </div>
);

NewSimulationButton.propTypes = {
    onClick: PropTypes.func.isRequired,
};

export default NewSimulationButton;
