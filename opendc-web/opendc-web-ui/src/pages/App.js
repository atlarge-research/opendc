import PropTypes from 'prop-types'
import React from 'react'
import DocumentTitle from 'react-document-title'
import { connect } from 'react-redux'
import { ShortcutManager } from 'react-shortcuts'
import { openPortfolioSucceeded } from '../actions/portfolios'
import { openProjectSucceeded } from '../actions/projects'
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
import AppNavbarContainer from '../containers/navigation/AppNavbarContainer'
import ProjectSidebarContainer from '../containers/app/sidebars/project/ProjectSidebarContainer'
import { openScenarioSucceeded } from '../actions/scenarios'
import NewPortfolioModal from '../containers/modals/NewPortfolioModal'
import NewScenarioModal from '../containers/modals/NewScenarioModal'
import PortfolioResultsContainer from '../containers/app/results/PortfolioResultsContainer'

const shortcutManager = new ShortcutManager(KeymapConfiguration)

class AppComponent extends React.Component {
    static propTypes = {
        projectId: PropTypes.string.isRequired,
        portfolioId: PropTypes.string,
        scenarioId: PropTypes.string,
        projectName: PropTypes.string,
    }
    static childContextTypes = {
        shortcuts: PropTypes.object.isRequired,
    }

    componentDidMount() {
        if (this.props.scenarioId) {
            this.props.openScenarioSucceeded(this.props.projectId, this.props.portfolioId, this.props.scenarioId)
        } else if (this.props.portfolioId) {
            this.props.openPortfolioSucceeded(this.props.projectId, this.props.portfolioId)
        } else {
            this.props.openProjectSucceeded(this.props.projectId)
        }
    }

    getChildContext() {
        return {
            shortcuts: shortcutManager,
        }
    }

    render() {
        const constructionElements = this.props.topologyIsLoading ? (
            <div className="full-height d-flex align-items-center justify-content-center">
                <LoadingScreen />
            </div>
        ) : (
            <div className="full-height">
                <MapStage />
                <ScaleIndicatorContainer />
                <ToolPanelComponent />
                <ProjectSidebarContainer />
                <TopologySidebarContainer />
            </div>
        )

        const portfolioElements = (
            <div className="full-height app-page-container">
                <ProjectSidebarContainer />
                <div className="container-fluid full-height">
                    <PortfolioResultsContainer />
                </div>
            </div>
        )

        const scenarioElements = (
            <div className="full-height app-page-container">
                <ProjectSidebarContainer />
                <div className="container-fluid full-height">
                    <h2>Scenario loading</h2>
                </div>
            </div>
        )

        return (
            <DocumentTitle
                title={this.props.projectName ? this.props.projectName + ' - OpenDC' : 'Simulation - OpenDC'}
            >
                <div className="page-container full-height">
                    <AppNavbarContainer fullWidth={true} />
                    {this.props.scenarioId
                        ? scenarioElements
                        : this.props.portfolioId
                        ? portfolioElements
                        : constructionElements}
                    <NewTopologyModal />
                    <NewPortfolioModal />
                    <NewScenarioModal />
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
        openProjectSucceeded: (projectId) => dispatch(openProjectSucceeded(projectId)),
        openPortfolioSucceeded: (projectId, portfolioId) => dispatch(openPortfolioSucceeded(projectId, portfolioId)),
        openScenarioSucceeded: (projectId, portfolioId, scenarioId) =>
            dispatch(openScenarioSucceeded(projectId, portfolioId, scenarioId)),
    }
}

const App = connect(mapStateToProps, mapDispatchToProps)(AppComponent)

export default App
