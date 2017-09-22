import React from 'react';
import DocumentTitle from "react-document-title";
import {connect} from "react-redux";
import {openNewSimulationModal} from "../actions/modals/simulations";
import {fetchAuthorizationsOfCurrentUser} from "../actions/users";
import AppNavbar from "../components/navigation/AppNavbar";
import SimulationFilterPanel from "../components/simulations/FilterPanel";
import NewSimulationModal from "../containers/modals/NewSimulationModal";
import NewSimulationButtonContainer from "../containers/simulations/NewSimulationButtonContainer";
import VisibleSimulationList from "../containers/simulations/VisibleSimulationAuthList";

class SimulationsContainer extends React.Component {
    componentDidMount() {
        this.props.fetchAuthorizationsOfCurrentUser();
    }

    render() {
        return (
            <DocumentTitle title="My Simulations - OpenDC">
                <div className="full-height">
                    <AppNavbar inSimulation={false}/>
                    <div className="container text-page-container full-height">
                        <SimulationFilterPanel/>
                        <VisibleSimulationList/>
                        <NewSimulationButtonContainer/>
                    </div>
                    <NewSimulationModal/>
                </div>
            </DocumentTitle>
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
