import React from 'react';
import {connect} from "react-redux";
import {openNewSimulationModal} from "../actions/modals/simulations";
import {fetchAuthorizationsOfCurrentUser} from "../actions/users";
import AppNavbar from "../components/navigation/AppNavbar";
import SimulationFilterPanel from "../components/simulations/FilterPanel";
import NewSimulationButton from "../components/simulations/NewSimulationButton";
import NewSimulationModal from "../containers/modals/NewSimulationModal";
import VisibleSimulationList from "../containers/simulations/VisibleSimulationAuthList";

class SimulationsContainer extends React.Component {
    componentDidMount() {
        this.props.fetchAuthorizationsOfCurrentUser();
    }

    render() {
        return (
            <div className="full-height">
                <AppNavbar inSimulation={false}/>
                <div className="container text-page-container full-height">
                    <SimulationFilterPanel/>
                    <VisibleSimulationList/>
                    <NewSimulationButton onClick={() => {this.props.openNewSimulationModal()}}/>
                </div>
                <NewSimulationModal/>
            </div>
        );
    }
}

const mapDispatchToProps = dispatch => {
    return {
        fetchAuthorizationsOfCurrentUser: () => dispatch(fetchAuthorizationsOfCurrentUser()),
        openNewSimulationModal: () => dispatch(openNewSimulationModal()),
    };
};

const Simulations = connect(
    undefined,
    mapDispatchToProps
)(SimulationsContainer);

export default Simulations;
