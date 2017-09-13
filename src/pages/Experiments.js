import PropTypes from "prop-types";
import React from "react";
import {connect} from "react-redux";
import {openSimulationSucceeded} from "../actions/simulations";
import AppNavbar from "../components/navigation/AppNavbar";

class ExperimentsComponent extends React.Component {
    static propTypes = {
        simulationId: PropTypes.number.isRequired,
    };

    componentDidMount() {
        this.props.storeSimulationId(this.props.simulationId);
        // TODO fetch experiments
    }

    render() {
        return (
            <div className="full-height">
                <AppNavbar simulationId={this.props.simulationId} inSimulation={true}/>
                <div className="container text-page-container full-height">
                    Test
                </div>
            </div>
        );
    }
}

const mapDispatchToProps = dispatch => {
    return {
        storeSimulationId: id => dispatch(openSimulationSucceeded(id)),
    };
};

const Experiments = connect(
    undefined,
    mapDispatchToProps
)(ExperimentsComponent);

export default Experiments;
