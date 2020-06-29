import PropTypes from "prop-types";
import React from "react";
import { Link } from "react-router-dom";
import Shapes from "../../shapes/index";

const ExperimentRowComponent = ({ experiment, simulationId, onDelete }) => (
  <tr>
    <td className="pt-3">{experiment.name}</td>
    <td className="pt-3">
      {experiment.path.name
        ? experiment.path.name
        : "Path " + experiment.path.id}
    </td>
    <td className="pt-3">{experiment.trace.name}</td>
    <td className="pt-3">{experiment.scheduler.name}</td>
    <td className="text-right">
      <Link
        to={"/simulations/" + simulationId + "/experiments/" + experiment.id}
        className="btn btn-outline-primary btn-sm mr-2"
        title="Open this experiment"
      >
        <span className="fa fa-play" />
      </Link>
      <div
        className="btn btn-outline-danger btn-sm"
        title="Delete this experiment"
        onClick={() => onDelete(experiment.id)}
      >
        <span className="fa fa-trash" />
      </div>
    </td>
  </tr>
);

ExperimentRowComponent.propTypes = {
  experiment: Shapes.Experiment.isRequired,
  simulationId: PropTypes.number.isRequired
};

export default ExperimentRowComponent;
