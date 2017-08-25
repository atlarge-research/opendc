import React from 'react';
import {connect} from "react-redux";
import {addSimulation, openNewSimulationModal} from "../actions/simulations";
import {fetchAuthorizationsOfCurrentUser} from "../actions/users";
import Navbar from "../components/navigation/Navbar";
import SimulationFilterPanel from "../components/simulations/FilterPanel";
import NewSimulationButton from "../components/simulations/NewSimulationButton";
import Login from "../containers/auth/Login";
import NewSimulationModal from "../containers/simulations/NewSimulationModal";
import VisibleSimulationList from "../containers/simulations/VisibleSimulationAuthList";
import "./Simulations.css";

class SimulationsContainer extends React.Component {
    componentDidMount() {
        this.props.fetchAuthorizationsOfCurrentUser();
    }

    render() {
        return (
            <div className="full-height">
                <Navbar/>
                <div className="container simulation-page-container full-height">
                    <SimulationFilterPanel/>
                    <VisibleSimulationList/>
                    <NewSimulationButton onClick={() => {this.props.openNewSimulationModal()}}/>
                </div>
                <NewSimulationModal/>
                <Login visible={false}/>
            </div>
        );
    }
}

const mapDispatchToProps = dispatch => {
    return {
        fetchAuthorizationsOfCurrentUser: () => dispatch(fetchAuthorizationsOfCurrentUser()),
        openNewSimulationModal: () => dispatch(openNewSimulationModal()),
        addSimulation: (text) => dispatch(addSimulation(text)),
    };
};

const Simulations = connect(
    undefined,
    mapDispatchToProps
)(SimulationsContainer);

export default Simulations;
