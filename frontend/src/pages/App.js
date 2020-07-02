import PropTypes from 'prop-types'
import React from 'react'
import DocumentTitle from 'react-document-title'
import { connect } from 'react-redux'
import { ShortcutManager } from 'react-shortcuts'
import { openExperimentSucceeded } from '../actions/experiments'
import { openSimulationSucceeded } from '../actions/simulations'
import { resetCurrentTopology } from '../actions/topology/building'
import ToolPanelComponent from '../components/app/map/controls/ToolPanelComponent'
import LoadingScreen from '../components/app/map/LoadingScreen'
import SimulationSidebarComponent from '../components/app/sidebars/simulation/SimulationSidebarComponent'
import AppNavbar from '../components/navigation/AppNavbar'
import ScaleIndicatorContainer from '../containers/app/map/controls/ScaleIndicatorContainer'
import MapStage from '../containers/app/map/MapStage'
import TopologySidebar from '../containers/app/sidebars/topology/TopologySidebar'
import TimelineContainer from '../containers/app/timeline/TimelineContainer'
import DeleteMachineModal from '../containers/modals/DeleteMachineModal'
import DeleteRackModal from '../containers/modals/DeleteRackModal'
import DeleteRoomModal from '../containers/modals/DeleteRoomModal'
import EditRackNameModal from '../containers/modals/EditRackNameModal'
import EditRoomNameModal from '../containers/modals/EditRoomNameModal'
import KeymapConfiguration from '../shortcuts/keymap'
import ChangeTopologyModal from '../containers/modals/ChangeTopologyModal'
import { openChangeTopologyModal } from '../actions/modals/topology'

const shortcutManager = new ShortcutManager(KeymapConfiguration)

class AppComponent extends React.Component {
    static propTypes = {
        simulationId: PropTypes.string.isRequired,
        inSimulation: PropTypes.bool,
        experimentId: PropTypes.number,
        simulationName: PropTypes.string,
        onViewTopologies: PropTypes.func,
    }
    static childContextTypes = {
        shortcuts: PropTypes.object.isRequired,
    }

    componentDidMount() {
        // TODO this.props.resetCurrentTopology()
        if (this.props.inSimulation) {
            this.props.openExperimentSucceeded(this.props.simulationId, this.props.experimentId)
            return
        }
        this.props.openSimulationSucceeded(this.props.simulationId)
    }

    getChildContext() {
        return {
            shortcuts: shortcutManager,
        }
    }

    render() {
        return (
            <DocumentTitle
                title={this.props.simulationName ? this.props.simulationName + ' - OpenDC' : 'Simulation - OpenDC'}
            >
                <div className="page-container full-height">
                    <AppNavbar
                        simulationId={this.props.simulationId}
                        inSimulation={true}
                        fullWidth={true}
                        onViewTopologies={this.props.onViewTopologies}
                    />
                    {this.props.topologyIsLoading ? (
                        <div className="full-height d-flex align-items-center justify-content-center">
                            <LoadingScreen />
                        </div>
                    ) : (
                        <div className="full-height">
                            <MapStage />
                            <ScaleIndicatorContainer />
                            <ToolPanelComponent />
                            <TopologySidebar />
                            {this.props.inSimulation ? <TimelineContainer /> : undefined}
                            {this.props.inSimulation ? <SimulationSidebarComponent /> : undefined}
                        </div>
                    )}
                    <ChangeTopologyModal />
                    <EditRoomNameModal />
                    <DeleteRoomModal />
                    <EditRackNameModal />
                    <DeleteRackModal />
                    <DeleteMachineModal />
                </div>
            </DocumentTitle>
        )
    }
}

const mapStateToProps = (state) => {
    let simulationName = undefined
    if (state.currentSimulationId !== -1 && state.objects.simulation[state.currentSimulationId]) {
        simulationName = state.objects.simulation[state.currentSimulationId].name
    }

    return {
        topologyIsLoading: state.currentTopologyId === -1,
        simulationName,
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        resetCurrentTopology: () => dispatch(resetCurrentTopology()),
        openSimulationSucceeded: (id) => dispatch(openSimulationSucceeded(id)),
        onViewTopologies: () => dispatch(openChangeTopologyModal()),
        openExperimentSucceeded: (simulationId, experimentId) =>
            dispatch(openExperimentSucceeded(simulationId, experimentId)),
    }
}

const App = connect(mapStateToProps, mapDispatchToProps)(AppComponent)

export default App
