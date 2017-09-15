import React from "react";
import Shapes from "../../shapes/index";

const ExperimentRowComponent = ({experiment}) => (
    <tr>
        <td>{experiment.name}</td>
        <td>{experiment.path.name ? experiment.path.name : "Path " + experiment.path.id}</td>
        <td>{experiment.trace.name}</td>
        <td>{experiment.scheduler.name}</td>
    </tr>
);

ExperimentRowComponent.propTypes = {
    experiment: Shapes.Experiment
};

export default ExperimentRowComponent;
