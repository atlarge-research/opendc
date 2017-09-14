import PropTypes from "prop-types";
import React from "react";
import ExperimentRowContainer from "../../containers/experiments/ExperimentRowContainer";

const ExperimentListComponent = ({experimentIds}) => (
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
        {experimentIds.map(experimentId => (
            <ExperimentRowContainer experimentId={experimentId}/>
        ))}
        </tbody>
    </table>
);

ExperimentListComponent.propTypes = {
    experimentIds: PropTypes.arrayOf(PropTypes.number).isRequired,
};

export default ExperimentListComponent;
