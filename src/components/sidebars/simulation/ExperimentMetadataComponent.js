import React from "react";

const ExperimentMetadataComponent = ({experimentName, pathName, traceName, schedulerName}) => (
    <div>
        <h2>{experimentName}</h2>
        <p><strong>Path:</strong> {pathName}</p>
        <p><strong>Trace:</strong> {traceName}</p>
        <p><strong>Scheduler:</strong> {schedulerName}</p>
    </div>
);

export default ExperimentMetadataComponent;
