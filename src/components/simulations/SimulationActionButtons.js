import PropTypes from "prop-types";
import React from 'react';
import {Link} from "react-router-dom";

const SimulationActionButtons = ({simulationId, onViewUsers, onDelete}) => (
    <td className="text-right">
        <Link
            to={"/simulations/" + simulationId}
            className="btn btn-outline-primary btn-sm mr-1"
            title="Open this simulation"
        >
            <span className="fa fa-play"/>
        </Link>
        <div
            className="btn btn-outline-success btn-sm mr-1"
            title="View and edit collaborators"
            onClick={() => onViewUsers(simulationId)}
        >
            <span className="fa fa-users"/>
        </div>
        <div
            className="btn btn-outline-danger btn-sm"
            title="Delete this simulation"
            onClick={() => onDelete(simulationId)}
        >
            <span className="fa fa-trash"/>
        </div>
    </td>
);

SimulationActionButtons.propTypes = {
    simulationId: PropTypes.number.isRequired,
    onViewUsers: PropTypes.func,
    onDelete: PropTypes.func,
};

export default SimulationActionButtons;
