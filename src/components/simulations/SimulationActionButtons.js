import PropTypes from "prop-types";
import React from 'react';

const SimulationActionButtons = ({simulationId, onOpen, onViewUsers, onDelete}) => (
    <div className="simulation-icons">
        <div className="open" title="Open this simulation" onClick={() => onOpen(simulationId)}>
            <span className="fa fa-play"/>
        </div>
        <div className="users" title="View and edit collaborators on this simulation"
             onClick={() => onViewUsers(simulationId)}>
            <span className="fa fa-users"/>
        </div>
        <div className="delete" title="Delete this simulation" onClick={() => onDelete(simulationId)}>
            <span className="fa fa-trash"/>
        </div>
    </div>
);

SimulationActionButtons.propTypes = {
    simulationId: PropTypes.number.isRequired,
    onOpen: PropTypes.func,
    onViewUsers: PropTypes.func,
    onDelete: PropTypes.func,
};

export default SimulationActionButtons;
