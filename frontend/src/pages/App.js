import PropTypes from 'prop-types'
import React from 'react'
import DocumentTitle from 'react-document-title'
import { connect } from 'react-redux'
import { ShortcutManager } from 'react-shortcuts'
import { openExperimentSucceeded } from '../actions/experiments'
import { openProjectSucceeded } from '../actions/projects'
import { resetCurrentTopology } from '../actions/topology/building'
import ToolPanelComponent from '../components/app/map/controls/ToolPanelComponent'
import LoadingScreen from '../components/app/map/LoadingScreen'
import AppNavbar from '../components/navigation/AppNavbar'
import ScaleIndicatorContainer from '../containers/app/map/controls/ScaleIndicatorContainer'
import MapStage from '../containers/app/map/MapStage'
import TopologySidebar from '../containers/app/sidebars/topology/TopologySidebar'
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
        projectId: PropTypes.string.isRequired,
        experimentId: PropTypes.number,
        projectName: PropTypes.string,
        onViewTopologies: PropTypes.func,
    }
    static childContextTypes = {
        shortcuts: PropTypes.object.isRequired,
    }

    componentDidMount() {
        this.props.openProjectSucceeded(this.props.projectId)
    }

    getChildContext() {
        return {
            shortcuts: shortcutManager,
        }
    }

    render() {
        return (
            <DocumentTitle
                title={this.props.projectName ? this.props.projectName + ' - OpenDC' : 'Simulation - OpenDC'}
            >
                <div className="page-container full-height">
                    <AppNavbar
                        projectId={this.props.projectId}
                        inProject={true}
                        fullWidth={true}
                        onViewTopologies={this.props.onViewTopologies}
                    />
                    {this.props.topologyIsLoading ? (
                        <div className="full-height d-flex align-items-center justify-content-center">
                            <LoadingScreen/>
                        </div>
                    ) : (
                        <div className="full-height">
                            <MapStage/>
                            <ScaleIndicatorContainer/>
                            <ToolPanelComponent/>
                            <TopologySidebar/>
                        </div>
                    )}
                    <ChangeTopologyModal/>
                    <EditRoomNameModal/>
                    <DeleteRoomModal/>
                    <EditRackNameModal/>
                    <DeleteRackModal/>
                    <DeleteMachineModal/>
                </div>
            </DocumentTitle>
        )
    }
}

const mapStateToProps = (state) => {
    let projectName = undefined
    if (state.currentProjectId !== '-1' && state.objects.project[state.currentProjectId]) {
        projectName = state.objects.project[state.currentProjectId].name
    }

    return {
        topologyIsLoading: state.currentTopologyId === '-1',
        projectName,
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        resetCurrentTopology: () => dispatch(resetCurrentTopology()),
        openProjectSucceeded: (id) => dispatch(openProjectSucceeded(id)),
        onViewTopologies: () => dispatch(openChangeTopologyModal()),
        openExperimentSucceeded: (projectId, experimentId) =>
            dispatch(openExperimentSucceeded(projectId, experimentId)),
    }
}

const App = connect(mapStateToProps, mapDispatchToProps)(AppComponent)

export default App
