import React from "react";

const ExperimentListContainer = ({experiments}) => (
    <table className="table">
        <thead>
        <tr>
            <th>Name</th>
            <th>Path</th>
            <th>Trace</th>
            <th>Scheduler</th>
        </tr>
        </thead>
        <tbody>
        {experiments.map(experiment => (
            <tr>
                <td>{experiment.name}</td>
                <td>{experiment.path.name}</td>
                <td>{experiment.trace.name}</td>
                <td>{experiment.scheduler.name}</td>
            </tr>
        ))}
        </tbody>
    </table>
);

export default ExperimentListContainer;
