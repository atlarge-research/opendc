import PropTypes from 'prop-types'
import React from 'react'
import ExperimentRowContainer from '../../containers/experiments/ExperimentRowContainer'

const ExperimentListComponent = ({ experimentIds, loading }) => {
    let alert

    if (loading) {
        alert = (
            <div className="alert alert-success">
                <span className="fa fa-refresh fa-spin mr-2"/>
                <strong>Loading Experiments...</strong>
            </div>
        )
    } else if (experimentIds.length === 0 && !loading) {
        alert = (
            <div className="alert alert-info">
                <span className="fa fa-question-circle mr-2"/>
                <strong>No experiments here yet...</strong> Add some with the button
                below!
            </div>
        )
    }

    return (
        <div className="vertically-expanding-container">
            {alert ? (
                alert
            ) : (
                <table className="table table-striped">
                    <thead>
                    <tr>
                        <th>Name</th>
                        <th>Path</th>
                        <th>Trace</th>
                        <th>Scheduler</th>
                        <th/>
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
    )
}

ExperimentListComponent.propTypes = {
    experimentIds: PropTypes.arrayOf(PropTypes.number).isRequired,
    loading: PropTypes.bool,
}

export default ExperimentListComponent
