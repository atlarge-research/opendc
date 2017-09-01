import PropTypes from "prop-types";
import React from 'react';
import {connect} from "react-redux";
import {openSimulationSucceeded} from "../actions/simulations";
import {fetchLatestDatacenter} from "../actions/topology";
import MapStage from "../components/map/MapStage";
import AppNavbar from "../components/navigation/AppNavbar";
import EditRoomNameModal from "../containers/modals/EditRoomNameModal";
import TopologySidebar from "../containers/sidebars/topology/TopologySidebar";

class AppContainer extends React.Component {
    static propTypes = {
        simulationId: PropTypes.number.isRequired,
    };

    componentDidMount() {
        this.props.storeSimulationId(this.props.simulationId);
        this.props.fetchLatestDatacenter();
    }

    render() {
        return (
            <div className="page-container full-height">
                <AppNavbar/>
                <div className="full-height">
                    <MapStage/>
                    <TopologySidebar/>
                </div>
                <EditRoomNameModal/>
            </div>
        );
    }
}

const mapDispatchToProps = dispatch => {
    return {
        storeSimulationId: id => dispatch(openSimulationSucceeded(id)),
        fetchLatestDatacenter: () => dispatch(fetchLatestDatacenter()),
    };
};

const App = connect(
    undefined,
    mapDispatchToProps
)(AppContainer);

export default App;
