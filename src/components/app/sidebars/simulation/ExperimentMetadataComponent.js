import React from "react";

const ExperimentMetadataComponent = ({experimentName, pathName, traceName, schedulerName}) => (
    <div>
        <h2>{experimentName}</h2>
        <p>Path: <strong>{pathName}</strong></p>
        <p>Trace: <strong>{traceName}</strong></p>
        <p>Scheduler: <strong>{schedulerName}</strong></p>
    </div>
);

export default ExperimentMetadataComponent;
