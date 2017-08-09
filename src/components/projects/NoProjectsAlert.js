import React from 'react';
import "./NoProjectsAlert.css";

const NoProjectsAlert = () => (
    <div className="no-projects-alert alert alert-info">
        <span className="info-icon fa fa-2x fa-question-circle"/>
        <strong>No projects here yet...</strong> Add some with the 'New Project' button!
    </div>
);

export default NoProjectsAlert;
