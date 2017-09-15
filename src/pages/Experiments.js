import PropTypes from "prop-types";
import React from "react";
import {connect} from "react-redux";
import {fetchExperimentsOfSimulation} from "../actions/experiments";
import {openSimulationSucceeded} from "../actions/simulations";
import AppNavbar from "../components/navigation/AppNavbar";
import ExperimentListContainer from "../containers/experiments/ExperimentListContainer";
import NewExperimentButtonContainer from "../containers/experiments/NewExperimentButtonContainer";
import NewExperimentModal from "../containers/modals/NewExperimentModal";

class ExperimentsComponent extends React.Component {
    static propTypes = {
        simulationId: PropTypes.number.isRequired,
    };

    componentDidMount() {
        this.props.storeSimulationId(this.props.simulationId);
        this.props.fetchExperimentsOfSimulation(this.props.simulationId);
    }

    render() {
        return (
            <div className="full-height">
                <AppNavbar simulationId={this.props.simulationId} inSimulation={true}/>
                <div className="container text-page-container full-height">
                    <ExperimentListContainer/>
                    <NewExperimentButtonContainer/>
                </div>
                <NewExperimentModal/>
            </div>
        );
    }
}

const mapDispatchToProps = dispatch => {
    return {
        storeSimulationId: id => dispatch(openSimulationSucceeded(id)),
        fetchExperimentsOfSimulation: id => dispatch(fetchExperimentsOfSimulation(id)),
    };
};

const Experiments = connect(
    undefined,
    mapDispatchToProps
)(ExperimentsComponent);

export default Experiments;
