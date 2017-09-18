import PropTypes from "prop-types";
import React from "react";
import {connect} from "react-redux";
import {ShortcutManager} from "react-shortcuts";
import {openExperimentSucceeded} from "../actions/experiments";
import {openSimulationSucceeded} from "../actions/simulations";
import {resetCurrentDatacenter} from "../actions/topology/building";
import LoadingScreen from "../components/map/LoadingScreen";
import AppNavbar from "../components/navigation/AppNavbar";
import Timeline from "../components/timeline/Timeline";
import MapStage from "../containers/map/MapStage";
import DeleteMachineModal from "../containers/modals/DeleteMachineModal";
import DeleteRackModal from "../containers/modals/DeleteRackModal";
import DeleteRoomModal from "../containers/modals/DeleteRoomModal";
import EditRackNameModal from "../containers/modals/EditRackNameModal";
import EditRoomNameModal from "../containers/modals/EditRoomNameModal";
import TopologySidebar from "../containers/sidebars/topology/TopologySidebar";
import KeymapConfiguration from "../shortcuts/keymap";

const shortcutManager = new ShortcutManager(KeymapConfiguration);

class AppComponent extends React.Component {
    static propTypes = {
        simulationId: PropTypes.number.isRequired,
        inSimulation: PropTypes.bool,
        experimentId: PropTypes.number,
    };
    static childContextTypes = {
        shortcuts: PropTypes.object.isRequired
    };

    componentDidMount() {
        this.props.resetCurrentDatacenter();
        if (this.props.inSimulation) {
            this.props.openExperimentSucceeded(this.props.simulationId, this.props.experimentId);
            return;
        }
        this.props.openSimulationSucceeded(this.props.simulationId);
    }

    getChildContext() {
        return {
            shortcuts: shortcutManager
        }
    }

    render() {
        return (
            <div className="page-container full-height">
                <AppNavbar simulationId={this.props.simulationId} inSimulation={true}/>
                {this.props.datacenterIsLoading ?
                    <div className="full-height d-flex align-items-center justify-content-center">
                        <LoadingScreen/>
                    </div> :
                    <div className="full-height">
                        <MapStage/>
                        <TopologySidebar/>
                        {this.props.inSimulation ?
                            <Timeline/> :
                            undefined
                        }
                    </div>
                }
                <EditRoomNameModal/>
                <DeleteRoomModal/>
                <EditRackNameModal/>
                <DeleteRackModal/>
                <DeleteMachineModal/>
            </div>
        );
    }
}

const mapStateToProps = state => {
    return {
        datacenterIsLoading: state.currentDatacenterId === -1,
    };
};

const mapDispatchToProps = dispatch => {
    return {
        resetCurrentDatacenter: () => dispatch(resetCurrentDatacenter()),
        openSimulationSucceeded: id => dispatch(openSimulationSucceeded(id)),
        openExperimentSucceeded: (simulationId, experimentId) =>
            dispatch(openExperimentSucceeded(simulationId, experimentId)),
    };
};

const App = connect(
    mapStateToProps,
    mapDispatchToProps
)(AppComponent);

export default App;
