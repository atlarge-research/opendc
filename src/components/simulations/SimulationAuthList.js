import PropTypes from 'prop-types';
import React from 'react';
import Shapes from "../../shapes/index";
import "./SimulationAuthList.css";
import SimulationAuthRow from "./SimulationAuthRow";

const SimulationAuthList = ({authorizations}) => {
    return (
        <div className="vertically-expanding-container">
            {authorizations.length === 0 ?
                <div className="alert alert-info">
                    <span className="info-icon fa fa-question-circle mr-2"/>
                    <strong>No simulations here yet...</strong> Add some with the 'New Simulation' button!
                </div> :
                <table className="table">
                    <thead>
                    <tr>
                        <th>Simulation name</th>
                        <th>Last edited</th>
                        <th>Access rights</th>
                        <th/>
                    </tr>
                    </thead>
                    <tbody>
                    {authorizations.map(authorization => (
                        <SimulationAuthRow simulationAuth={authorization} key={authorization.simulation.id}/>
                    ))}
                    </tbody>
                </table>
            }
        </div>
    );
};

SimulationAuthList.propTypes = {
    authorizations: PropTypes.arrayOf(Shapes.Authorization).isRequired,
};

export default SimulationAuthList;
