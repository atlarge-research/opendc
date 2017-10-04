import PropTypes from "prop-types";
import React from "react";
import ExperimentRowContainer from "../../containers/experiments/ExperimentRowContainer";

const ExperimentListComponent = ({ experimentIds }) => {
  return (
    <div className="vertically-expanding-container">
      {experimentIds.length === 0 ? (
        <div className="alert alert-info">
          <span className="info-icon fa fa-question-circle mr-2" />
          <strong>No experiments here yet...</strong> Add some with the button
          below!
        </div>
      ) : (
        <table className="table table-striped">
          <thead>
            <tr>
              <th>Name</th>
              <th>Path</th>
              <th>Trace</th>
              <th>Scheduler</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {experimentIds.map(experimentId => (
              <ExperimentRowContainer
                experimentId={experimentId}
                key={experimentId}
              />
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

ExperimentListComponent.propTypes = {
  experimentIds: PropTypes.arrayOf(PropTypes.number).isRequired
};

export default ExperimentListComponent;
