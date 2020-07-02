import React from 'react'

const ExperimentMetadataComponent = ({
                                         experimentName,
                                         topologyName,
                                         traceName,
                                         schedulerName,
                                     }) => (
    <div>
        <h2>{experimentName}</h2>
        <p>
            Topology: <strong>{topologyName}</strong>
        </p>
        <p>
            Trace: <strong>{traceName}</strong>
        </p>
        <p>
            Scheduler: <strong>{schedulerName}</strong>
        </p>
    </div>
)

export default ExperimentMetadataComponent
