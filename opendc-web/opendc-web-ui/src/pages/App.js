import PropTypes from 'prop-types'
import React, { useEffect } from 'react'
import DocumentTitle from 'react-document-title'
import { HotKeys } from 'react-hotkeys'
import { useDispatch, useSelector } from 'react-redux'
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
import NewTopologyModal from '../containers/modals/NewTopologyModal'
import AppNavbarContainer from '../containers/navigation/AppNavbarContainer'
import ProjectSidebarContainer from '../containers/app/sidebars/project/ProjectSidebarContainer'
import { openScenarioSucceeded } from '../actions/scenarios'
import NewPortfolioModal from '../containers/modals/NewPortfolioModal'
import NewScenarioModal from '../containers/modals/NewScenarioModal'
import PortfolioResultsContainer from '../containers/app/results/PortfolioResultsContainer'
import KeymapConfiguration from '../shortcuts/keymap'

const App = ({ projectId, portfolioId, scenarioId }) => {
    const projectName = useSelector(
        (state) =>
            state.currentProjectId !== '-1' &&
            state.objects.project[state.currentProjectId] &&
            state.objects.project[state.currentProjectId].name
    )
    const topologyIsLoading = useSelector((state) => state.currentTopologyId === '-1')

    const dispatch = useDispatch()
    useEffect(() => {
        if (scenarioId) {
            dispatch(openScenarioSucceeded(projectId, portfolioId, scenarioId))
        } else if (portfolioId) {
            dispatch(openPortfolioSucceeded(projectId, portfolioId))
        } else {
            dispatch(openProjectSucceeded(projectId))
        }
    }, [projectId, portfolioId, scenarioId, dispatch])

    const constructionElements = topologyIsLoading ? (
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
        <DocumentTitle title={projectName ? projectName + ' - OpenDC' : 'Simulation - OpenDC'}>
            <HotKeys keyMap={KeymapConfiguration} className="page-container full-height">
                <AppNavbarContainer fullWidth={true} />
                {scenarioId ? scenarioElements : portfolioId ? portfolioElements : constructionElements}
                <NewTopologyModal />
                <NewPortfolioModal />
                <NewScenarioModal />
                <EditRoomNameModal />
                <DeleteRoomModal />
                <EditRackNameModal />
                <DeleteRackModal />
                <DeleteMachineModal />
            </HotKeys>
        </DocumentTitle>
    )
}

App.propTypes = {
    projectId: PropTypes.string.isRequired,
    portfolioId: PropTypes.string,
    scenarioId: PropTypes.string,
}

export default App
