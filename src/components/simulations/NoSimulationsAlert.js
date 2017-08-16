import React from 'react';
import "./NoSimulationsAlert.css";

const NoSimulationsAlert = () => (
    <div className="no-simulations-alert alert alert-info">
        <span className="info-icon fa fa-2x fa-question-circle"/>
        <strong>No simulations here yet...</strong> Add some with the 'New Simulation' button!
    </div>
);

export default NoSimulationsAlert;
