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
import ScaleIndicatorContainer from '../containers/app/map/controls/ScaleIndicatorContainer'
import MapStage from '../containers/app/map/MapStage'
import TopologySidebarContainer from '../containers/app/sidebars/topology/TopologySidebarContainer'
import DeleteMachineModal from '../containers/modals/DeleteMachineModal'
import DeleteRackModal from '../containers/modals/DeleteRackModal'
import DeleteRoomModal from '../containers/modals/DeleteRoomModal'
import EditRackNameModal from '../containers/modals/EditRackNameModal'
import EditRoomNameModal from '../containers/modals/EditRoomNameModal'
import KeymapConfiguration from '../shortcuts/keymap'
import NewTopologyModal from '../containers/modals/NewTopologyModal'
import { openNewTopologyModal } from '../actions/modals/topology'
import AppNavbarContainer from '../containers/navigation/AppNavbarContainer'
import ProjectSidebarContainer from '../containers/app/sidebars/project/ProjectSidebarContainer'

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
                    <AppNavbarContainer fullWidth={true} />
                    {this.props.topologyIsLoading ? (
                        <div className="full-height d-flex align-items-center justify-content-center">
                            <LoadingScreen/>
                        </div>
                    ) : (
                        <div className="full-height">
                            <MapStage/>
                            <ScaleIndicatorContainer/>
                            <ToolPanelComponent/>
                            <ProjectSidebarContainer/>
                            <TopologySidebarContainer/>
                        </div>
                    )}
                    <NewTopologyModal/>
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
        onViewTopologies: () => dispatch(openNewTopologyModal()),
        openExperimentSucceeded: (projectId, experimentId) =>
            dispatch(openExperimentSucceeded(projectId, experimentId)),
    }
}

const App = connect(mapStateToProps, mapDispatchToProps)(AppComponent)

export default App
