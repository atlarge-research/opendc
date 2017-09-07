import PropTypes from "prop-types";
import React from 'react';
import {connect} from "react-redux";
import {ShortcutManager} from "react-shortcuts";
import {openSimulationSucceeded} from "../actions/simulations";
import {fetchLatestDatacenter, resetCurrentDatacenter} from "../actions/topology/building";
import MapStage from "../components/map/MapStage";
import AppNavbar from "../components/navigation/AppNavbar";
import DeleteMachineModal from "../containers/modals/DeleteMachineModal";
import DeleteRackModal from "../containers/modals/DeleteRackModal";
import DeleteRoomModal from "../containers/modals/DeleteRoomModal";
import EditRackNameModal from "../containers/modals/EditRackNameModal";
import EditRoomNameModal from "../containers/modals/EditRoomNameModal";
import TopologySidebar from "../containers/sidebars/topology/TopologySidebar";
import KeymapConfiguration from "../shortcuts/keymap";

const shortcutManager = new ShortcutManager(KeymapConfiguration);

class AppContainer extends React.Component {
    static propTypes = {
        simulationId: PropTypes.number.isRequired,
    };
    static childContextTypes = {
        shortcuts: PropTypes.object.isRequired
    };

    componentDidMount() {
        this.props.storeSimulationId(this.props.simulationId);
        this.props.resetCurrentDatacenter();
        this.props.fetchLatestDatacenter();
    }

    getChildContext() {
        return {
            shortcuts: shortcutManager
        }
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
                <DeleteRoomModal/>
                <EditRackNameModal/>
                <DeleteRackModal/>
                <DeleteMachineModal/>
            </div>
        );
    }
}

const mapDispatchToProps = dispatch => {
    return {
        storeSimulationId: id => dispatch(openSimulationSucceeded(id)),
        resetCurrentDatacenter: () => dispatch(resetCurrentDatacenter()),
        fetchLatestDatacenter: () => dispatch(fetchLatestDatacenter()),
    };
};

const App = connect(
    undefined,
    mapDispatchToProps
)(AppContainer);

export default App;
