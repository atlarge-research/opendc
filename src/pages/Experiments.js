import PropTypes from "prop-types";
import React from "react";
import {connect} from "react-redux";
import AppNavbar from "../components/navigation/AppNavbar";

class ExperimentsContainer extends React.Component {
    static propTypes = {
        simulationId: PropTypes.number.isRequired,
    };

    componentDidMount() {
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
    return {};
};

const Experiments = connect(
    undefined,
    mapDispatchToProps
)(ExperimentsContainer);

export default Experiments;
