import PropTypes from 'prop-types';
import React from 'react';
import Shapes from "../../shapes/index";
import NoSimulationsAlert from "./NoSimulationsAlert";
import SimulationAuth from "./SimulationAuth";
import "./SimulationAuthList.css";

const SimulationAuthList = ({authorizations}) => {
    if (authorizations.length === 0) {
        return <NoSimulationsAlert/>;
    }

    return (
        <div className="simulation-list">
            <div className="list-head">
                <div>Simulation name</div>
                <div>Last edited</div>
                <div>Access rights</div>
            </div>
            <div className="list-body">
                {authorizations.map(authorization => (
                    <SimulationAuth simulationAuth={authorization} key={authorization.simulation.id}/>
                ))}
            </div>
        </div>
    );
};

SimulationAuthList.propTypes = {
    authorizations: PropTypes.arrayOf(Shapes.Authorization).isRequired,
};

export default SimulationAuthList;
